package org.freedesktop.dbus.connections.sasl;

import jnr.posix.POSIXFactory;
import jnr.unixsocket.Credentials;
import jnr.unixsocket.UnixSocket;
import org.freedesktop.dbus.connections.FreeBSDHelper;
import org.freedesktop.dbus.messages.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.Random;

import static org.freedesktop.dbus.connections.sasl.AuthScheme.AUTH_NONE;
import static org.freedesktop.dbus.connections.sasl.SaslCommand.*;
import static org.freedesktop.dbus.connections.sasl.SaslUtils.*;
import static org.freedesktop.dbus.connections.sasl.SaslUtils.stupidlyDecode;

public class SaslServerStateMachine extends SaslStateMachine {

  private final Logger LOGGER = LoggerFactory.getLogger(getClass());

  private String challenge = "";
  private String cookie = "";

  @SuppressWarnings("unused")
  protected SaslServerStateMachine() {
    super();
  }

  protected SaslServerStateMachine(final boolean _hasFileDescriptorSupport) {
    super(_hasFileDescriptorSupport);
  }

  /**
   * performs server mode SASL auth on the given streams.
   * Mode selects whether to run as a SASL server or client.
   * Types is a EnumSet of the available auth schemes
   *
   * @param types An EnumSet of available {@link AuthScheme}s
   * @param guid  the guid
   * @param out   The {@link OutputStream} for outbund communication towards server
   * @param in    The {@link InputStream} for inbound communication from server
   * @param us    The {@link Socket} backing the input and output streams
   * @return true if the auth was successful and false if it failed.
   * @throws IOException on failure
   */
  @Override
  public boolean auth(EnumSet<AuthScheme> types, String guid, OutputStream out, InputStream in, Socket us) throws IOException {
    String luid;
    String kernelUid = null;

    long uid = POSIXFactory.getJavaPOSIX().getuid();
    luid = stupidlyEncode("" + uid);

    Command c;
    AuthScheme current = AUTH_NONE;
    SaslAuthState state = SaslAuthState.INITIAL_STATE;

    while (state != SaslAuthState.FINISHED && state != SaslAuthState.FAILED) {

      LOGGER.trace("Mode: SERVER AUTH state: {}", state);

      switch (state) {
        case INITIAL_STATE:
          byte[] buf = new byte[1];
          if (null == us) {
            int nread = in.read(buf);
            if (nread <= 0 || 0 != buf[0]) {
              state = SaslAuthState.FAILED;
            } else {
              state = SaslAuthState.WAIT_AUTH;
            }
          } else {
            Credentials credentials;
            try {
              if (FreeBSDHelper.isFreeBSD()) {
                long euid = FreeBSDHelper.recv_cred(us);
                if (euid >= 0) {
                  kernelUid = stupidlyEncode("" + euid);
                }
              } else {
                credentials = ((UnixSocket) us).getCredentials();
                int kuid = credentials.getUid();
                if (kuid >= 0) {
                  kernelUid = stupidlyEncode("" + kuid);
                }
              }
              state = SaslAuthState.WAIT_AUTH;

            } catch (SocketException _ex) {
              state = SaslAuthState.FAILED;
            }
          }
          break;
        case WAIT_AUTH:
          c = receive(in);
          switch (c.getCommand()) {
            case AUTH:
              switch (doResponse(current, luid, kernelUid, c)) {
                case CONTINUE:
                  send(out, DATA, c.getResponse());
                  current = c.getMechs();
                  state = SaslAuthState.WAIT_DATA;
                  break;
                case OK:
                  send(out, SaslCommand.OK, guid);
                  state = SaslAuthState.WAIT_BEGIN;
                  current = AUTH_NONE;
                  break;
                case REJECT:
                default:
                  send(out, REJECTED, getTypes(types));
                  current = AUTH_NONE;
                  break;
              }
              break;
            case ERROR:
              send(out, REJECTED, getTypes(types));
              break;
            case BEGIN:
              state = SaslAuthState.FAILED;
              break;
            default:
              send(out, ERROR, "Got invalid command");
              break;
          }
          break;
        case WAIT_DATA:
          c = receive(in);
          switch (c.getCommand()) {
            case DATA:
              switch (doResponse(current, luid, kernelUid, c)) {
                case CONTINUE:
                  send(out, DATA, c.getResponse());
                  state = SaslAuthState.WAIT_DATA;
                  break;
                case OK:
                  send(out, SaslCommand.OK, guid);
                  state = SaslAuthState.WAIT_BEGIN;
                  current = AUTH_NONE;
                  break;
                case REJECT:
                default:
                  send(out, REJECTED, getTypes(types));
                  current = AUTH_NONE;
                  break;
              }
              break;
            case ERROR:
            case CANCEL:
              send(out, REJECTED, getTypes(types));
              state = SaslAuthState.WAIT_AUTH;
              break;
            case BEGIN:
              state = SaslAuthState.FAILED;
              break;
            default:
              send(out, ERROR, "Got invalid command");
              break;
          }
          break;
        case WAIT_BEGIN:
          c = receive(in);
          switch (c.getCommand()) {
            case ERROR:
            case CANCEL:
              send(out, REJECTED, getTypes(types));
              state = SaslAuthState.WAIT_AUTH;
              break;
            case BEGIN:
              state = SaslAuthState.FINISHED;
              break;
            case NEGOTIATE_UNIX_FD:
              LOGGER.debug("File descriptor negotiation requested");
              if (!hasFileDescriptorSupport) {
                send(out, ERROR);
              } else {
                send(out, AGREE_UNIX_FD);
              }

              break;
            default:
              send(out, ERROR, "Got invalid command");
              break;
          }
          break;
        default:
          state = SaslAuthState.FAILED;
      }

    }

    return state == SaslAuthState.FINISHED;
  }

  private String[] getTypes(EnumSet<AuthScheme> types) {
    return AuthScheme.mechanics(types);
  }

  private SaslResult doResponse(AuthScheme _auth, String _uid, String _kernelUid, Command _c) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("SHA");
    } catch (NoSuchAlgorithmException nsae) {
      LOGGER.error("", nsae);
      return SaslResult.ERROR;
    }

    LOGGER.trace("scheme: {}, uid: {}, kuid: {}, cmd: {}", _auth, _uid, _kernelUid, _c);

    switch (_auth) {
      case AUTH_NONE:
        switch (_c.getMechs()) {
          case AUTH_ANON:
            LOGGER.trace("result AUTH_NONE(AUTH_ANON): OK");
            return SaslResult.OK;
          case AUTH_EXTERNAL:
            if (0 == col.compare(_uid, _c.getData()) && (null == _kernelUid || 0 == col.compare(_uid, _kernelUid))) {
              LOGGER.trace("result AUTH_NONE(AUTH_EXTERNAL): OK");
              return SaslResult.OK;
            } else {
              LOGGER.trace("result AUTH_NONE(AUTH_EXTERNAL): REJECT");
              return SaslResult.REJECT;
            }
          case AUTH_SHA:
            String context = COOKIE_CONTEXT;
            long id = System.currentTimeMillis();
            byte[] buf = new byte[8];
            Message.marshallintBig(id, buf, 0, 8);
            challenge = stupidlyEncode(md.digest(buf));
            Random r = new Random();
            r.nextBytes(buf);
            cookie = stupidlyEncode(md.digest(buf));
            try {
              addCookie(context, "" + id, id / 1000, cookie);
            } catch (IOException ioe) {
              LOGGER.debug("", ioe);
            }

            LOGGER.debug("Sending challenge: {} {} {}", context, id, challenge);

            _c.setResponse(stupidlyEncode(context + ' ' + id + ' ' + challenge));

            LOGGER.trace("result AUTH_NONE(AUTH_SHA): CONTINUE");

            return SaslResult.CONTINUE;
          default:
            LOGGER.trace("result AUTH_ANON(default): ERROR");
            return SaslResult.ERROR;
        }
      case AUTH_SHA:
        String[] response = stupidlyDecode(_c.getData()).split(" ");
        if (response.length < 2) {
          LOGGER.trace("result AUTH_SHA: ERROR");
          return SaslResult.ERROR;
        }
        String cchal = response[0];
        String hash = response[1];
        String prehash = challenge + ":" + cchal + ":" + cookie;
        byte[] buf = md.digest(prehash.getBytes());
        String posthash = stupidlyEncode(buf);
        LOGGER.debug("Authenticating Hash; data={} remote-hash={} local-hash={}", prehash, hash, posthash);
        if (0 == col.compare(posthash, hash)) {
          LOGGER.trace("result AUTH_SHA: OK");
          return SaslResult.OK;
        } else {
          LOGGER.trace("result AUTH_SHA (2): ERROR");
          return SaslResult.ERROR;
        }
      default:
        LOGGER.trace("result default: ERROR");
        return SaslResult.ERROR;
    }
  }
}
