package org.freedesktop.dbus.connections.sasl;

import jnr.posix.POSIXFactory;
import org.freedesktop.dbus.connections.FreeBSDHelper;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.utils.Hexdump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.EnumSet;

import static org.freedesktop.dbus.connections.sasl.AuthScheme.*;
import static org.freedesktop.dbus.connections.sasl.AuthScheme.AUTH_NONE;
import static org.freedesktop.dbus.connections.sasl.SaslCommand.*;
import static org.freedesktop.dbus.connections.sasl.SaslUtils.*;
import static org.freedesktop.dbus.connections.sasl.SaslUtils.findCookie;

public class SaslClientStateMachine extends SaslStateMachine {

  private final Logger LOGGER = LoggerFactory.getLogger(getClass());

  private boolean fileDescriptorSupported;

  @SuppressWarnings("unused")
  protected SaslClientStateMachine() {
    super();
  }

  protected SaslClientStateMachine(final boolean _hasFileDescriptorSupport) {
    super(_hasFileDescriptorSupport);
  }

  /**
   * performs client mode SASL auth on the given streams.
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

    long uid = POSIXFactory.getJavaPOSIX().getuid();
    luid = stupidlyEncode("" + uid);

    Command c;
    EnumSet<AuthScheme> failed = EnumSet.noneOf(AuthScheme.class);
    AuthScheme current = AUTH_NONE;
    SaslAuthState state = SaslAuthState.INITIAL_STATE;

    while (state != SaslAuthState.FINISHED && state != SaslAuthState.FAILED) {

      LOGGER.trace("Mode: CLIENT AUTH state: {}", state);

      switch (state) {
        case INITIAL_STATE:
          if (FreeBSDHelper.isFreeBSD()) {
            FreeBSDHelper.send_cred(us);
          } else {
            out.write(new byte[]{
                0
            });
          }
          send(out, AUTH);
          state = SaslAuthState.WAIT_DATA;
          break;
        case NEGOTIATE_UNIX_FD:
          c = receive(in);
          switch (c.getCommand()) {
            case ERROR:
              // when asking for file descriptor support, ERROR means FD support is not supported
              state = SaslAuthState.FINISHED;
              LOGGER.trace("File descriptors NOT supported by server");
              fileDescriptorSupported = false;
              send(out, BEGIN);
              break;
            case AGREE_UNIX_FD:
              // when asking for file descriptor support, AGREE_UNIX_FD means FD support is supported
              state = SaslAuthState.FINISHED;
              LOGGER.trace("File descriptors IS supported by server");
              fileDescriptorSupported = true;
              send(out, BEGIN);
              break;
          }
          break;
        case WAIT_DATA:
          c = receive(in);
          switch (c.getCommand()) {
            case DATA:
              switch (doChallenge(current, c)) {
                case CONTINUE:
                  send(out, DATA, c.getResponse());
                  break;
                case OK:
                  send(out, DATA, c.getResponse());
                  state = SaslAuthState.WAIT_OK;
                  break;
                case ERROR:
                default:
                  send(out, ERROR, c.getResponse());
                  break;
              }
              break;
            case REJECTED:
              failed.add(current);
              EnumSet<AuthScheme> available = c.getMechsSet();
              available.removeAll(failed);

              if (available.contains(AUTH_EXTERNAL)) {
                send(out, AUTH, "EXTERNAL", luid);
                current = AUTH_EXTERNAL;
              } else if (available.contains(AUTH_SHA)) {
                send(out, AUTH, "DBUS_COOKIE_SHA1", luid);
                current = AUTH_SHA;
              } else if (available.contains(AUTH_ANON)) {
                send(out, AUTH, "ANONYMOUS");
                current = AUTH_ANON;
              } else {
                state = SaslAuthState.FAILED;
              }
              break;
            case ERROR:
              // Edited by GP -- removed
              /*
              if (state == SaslAuthState.NEGOTIATE_UNIX_FD) {
                state = SaslAuthState.FINISHED;
                LOGGER.trace("File descriptors NOT supported by server");
                fileDescriptorSupported = false;
                send(out, BEGIN);
              } else {
               */
              send(out, CANCEL);
              state = SaslAuthState.WAIT_REJECT;
              /*
              }
               */
              break;
            case OK:
              LOGGER.trace("Authenticated");
              // Check GP
              // state = SaslAuthState.AUTHENTICATED;
              if (hasFileDescriptorSupport) {
                // Check GP
                // state = SaslAuthState.WAIT_DATA;
                LOGGER.trace("Asking for file descriptor support");
                // if authentication was successful, ask remote end for file descriptor support
                send(out, SaslCommand.NEGOTIATE_UNIX_FD);
                state = SaslAuthState.NEGOTIATE_UNIX_FD;
              } else {
                state = SaslAuthState.FINISHED;
                send(out, BEGIN);
              }
              break;
            case AGREE_UNIX_FD:
              if (hasFileDescriptorSupport) {
                state = SaslAuthState.FINISHED;
                LOGGER.trace("File descriptors supported by server");
                fileDescriptorSupported = true;
                send(out, BEGIN);
              }
              break;
            default:
              send(out, ERROR, "Got invalid command");
              break;
          }
          break;
        case WAIT_OK:
          c = receive(in);
          switch (c.getCommand()) {
            case OK:
              send(out, BEGIN);
              state = SaslAuthState.AUTHENTICATED;
              break;
            case ERROR:
            case DATA:
              send(out, CANCEL);
              state = SaslAuthState.WAIT_REJECT;
              break;
            case REJECTED:
              failed.add(current);
              EnumSet<AuthScheme> available = c.getMechsSet();
              available.removeAll(failed);
              state = SaslAuthState.WAIT_DATA;
              if (available.contains(AUTH_EXTERNAL)) {
                send(out, AUTH, AUTH_EXTERNAL.getAuthSchemeName(), luid);
                current = AUTH_EXTERNAL;
              } else if (available.contains(AUTH_SHA)) {
                send(out, AUTH, AUTH_SHA.getAuthSchemeName(), luid);
                current = AUTH_SHA;
              } else if (available.contains(AUTH_ANON)) {
                send(out, AUTH, AUTH_NONE.getAuthSchemeName());
                current = AUTH_ANON;
              } else {
                state = SaslAuthState.FAILED;
              }
              break;
            default:
              send(out, ERROR, "Got invalid command");
              break;
          }
          break;
        case WAIT_REJECT:
          c = receive(in);
          //noinspection SwitchStatementWithTooFewBranches
          switch (c.getCommand()) {
            case REJECTED:
              failed.add(current);
              EnumSet<AuthScheme> available = c.getMechsSet();
              available.removeAll(failed);

              if (available.contains(AUTH_EXTERNAL)) {
                send(out, AUTH, "EXTERNAL", luid);
                current = AUTH_EXTERNAL;
              } else if (available.contains(AUTH_SHA)) {
                send(out, AUTH, "DBUS_COOKIE_SHA1", luid);
                current = AUTH_SHA;
              } else if (available.contains(AUTH_ANON)) {
                send(out, AUTH, "ANONYMOUS");
                current = AUTH_ANON;
              } else {
                state = SaslAuthState.FAILED;
              }
              break;
            default:
              state = SaslAuthState.FAILED;
              break;
          }
          break;
        default:
          state = SaslAuthState.FAILED;
      }

    }

    return state == SaslAuthState.FINISHED;
  }

  @Override
  public boolean isFileDescriptorSupported() {
    return fileDescriptorSupported;
  }

  private SaslResult doChallenge(AuthScheme _auth, Command c) throws IOException {
    if (_auth == AUTH_SHA) {
      String[] reply = stupidlyDecode(c.getData()).split(" ");
      LOGGER.trace(Arrays.toString(reply));
      if (3 != reply.length) {
        LOGGER.debug("Reply is not length 3");
        return SaslResult.ERROR;
      }
      String context = reply[0];
      String id = reply[1];
      String serverchallenge = reply[2];
      MessageDigest md;
      try {
        md = MessageDigest.getInstance("SHA");
      } catch (NoSuchAlgorithmException nsae) {
        LOGGER.debug("", nsae);
        return SaslResult.ERROR;
      }
      byte[] buf = new byte[8];
      Message.marshallintBig(System.currentTimeMillis(), buf, 0, 8);
      String clientchallenge = stupidlyEncode(md.digest(buf));
      md.reset();
      long start = System.currentTimeMillis();
      String lCookie = null;
      while (null == lCookie && (System.currentTimeMillis() - start) < LOCK_TIMEOUT) {
        lCookie = findCookie(context, id);
      }
      if (null == lCookie) {
        LOGGER.debug("Did not find a cookie in context {}  with ID {}", context, id);
        return SaslResult.ERROR;
      }
      String response = serverchallenge + ":" + clientchallenge + ":" + lCookie;
      buf = md.digest(response.getBytes());

      LOGGER.trace("Response: {} hash: {}", response, Hexdump.format(buf));

      response = stupidlyEncode(buf);
      c.setResponse(stupidlyEncode(clientchallenge + " " + response));
      return SaslResult.OK;
    }
    LOGGER.debug("Not DBUS_COOKIE_SHA1 authtype.");
    return SaslResult.ERROR;
  }

}
