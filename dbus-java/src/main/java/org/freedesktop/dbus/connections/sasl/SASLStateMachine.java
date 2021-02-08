package org.freedesktop.dbus.connections.sasl;

import jnr.posix.POSIXFactory;

import jnr.unixsocket.Credentials;
import jnr.unixsocket.UnixSocket;
import org.freedesktop.dbus.connections.FreeBSDHelper;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.utils.Hexdump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Collator;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Random;

import static org.freedesktop.dbus.connections.sasl.AuthScheme.*;
import static org.freedesktop.dbus.connections.sasl.SaslUtils.*;
import static org.freedesktop.dbus.connections.sasl.SaslCommand.*;

@SuppressWarnings("unused")
public class SASLStateMachine {
  private final Logger LOGGER = LoggerFactory.getLogger(getClass());

  // Message headers for state machine messages (org.springframework.messaging)
  private static final String MECHS_MSG_HDR = "mechs";
  private static final String DATA_MSG_HDR = "data";
  private static final String RESPONSE_MSG_HDR = "response";

  private static final Collator col = Collator.getInstance();

  static {
    col.setDecomposition(Collator.FULL_DECOMPOSITION);
    col.setStrength(Collator.PRIMARY);
  }

  private String challenge = "";
  private String cookie = "";


  private boolean fileDescriptorSupported;
  /**
   * whether file descriptor passing is supported on the current connection.
   */
  private final boolean hasFileDescriptorSupport;

  /**
   * Create a new SASL auth handler.
   * Defaults to disable file descriptor passing.
   */
  public SASLStateMachine() {
    this(false);
  }

  /**
   * Create a new SASL auth handler.
   *
   * @param _hasFileDescriptorSupport true to support file descriptor passing (usually only works with UNIX_SOCKET).
   */
  public SASLStateMachine(boolean _hasFileDescriptorSupport) {
    hasFileDescriptorSupport = _hasFileDescriptorSupport;

  }

  /*
  public SASLStateMachine.Command receive(InputStream s) throws IOException {
    StringBuffer sb = new StringBuffer();
    top:
    while (true) {
      int c = s.read();
      switch (c) {
        case -1:
          throw new IOException("Stream unexpectedly short (broken pipe)");
        case 0:
        case '\r':
          continue;
        case '\n':
          break top;
        default:
          sb.append((char) c);
      }
    }
    LOGGER.trace("received: {}", sb);
    try {
      return new Command(sb.toString());
    } catch (Exception e) {
      LOGGER.error("Cannot create command.", e);
      return new Command();
    }
  }
  */

  public Command receive(InputStream s) throws IOException {
    StringBuffer sb = new StringBuffer();
    top:
    while (true) {
      int c = s.read();
      switch (c) {
        case -1:
          throw new IOException("Stream unexpectedly short (broken pipe)");
        case 0:
        case '\r':
          continue;
        case '\n':
          break top;
        default:
          sb.append((char) c);
      }
    }
    LOGGER.trace("received: {}", sb);
    try {
      return new Command(sb.toString());
    } catch (Exception e) {
      LOGGER.error("Cannot create command.", e);
      return new Command();
    }
  }

  public void send(OutputStream out, SaslCommand command, String... data) throws IOException {
    StringBuffer sb = new StringBuffer();
    sb.append(command.name());

    for (String s : data) {
      sb.append(' ');
      sb.append(s);
    }
    sb.append('\r');
    sb.append('\n');
    LOGGER.trace("sending: {}", sb);
    out.write(sb.toString().getBytes());
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

  public String[] getTypes(EnumSet<AuthScheme> types) {
    return AuthScheme.mechanics(types);
    /*
    switch (types) {
      case AUTH_EXTERNAL:
        return new String[]{
            "EXTERNAL"
        };
      case AUTH_SHA:
        return new String[]{
            "DBUS_COOKIE_SHA1"
        };
      case AUTH_ANON:
        return new String[]{
            "ANONYMOUS"
        };
      case AUTH_SHA + AUTH_EXTERNAL:
        return new String[]{
            "EXTERNAL", "DBUS_COOKIE_SHA1"
        };
      case AUTH_SHA + AUTH_ANON:
        return new String[]{
            "ANONYMOUS", "DBUS_COOKIE_SHA1"
        };
      case AUTH_EXTERNAL + AUTH_ANON:
        return new String[]{
            "ANONYMOUS", "EXTERNAL"
        };
      case AUTH_EXTERNAL + AUTH_ANON + AUTH_SHA:
        return new String[]{
            "ANONYMOUS", "EXTERNAL", "DBUS_COOKIE_SHA1"
        };
      default:
        return new String[]{};
    }
     */
  }

  /*
   * performs SASL auth on the given streams.
   * Mode selects whether to run as a SASL server or client.
   * Types is a bitmask of the available auth types.
   *
   * @param mode  mode
   * @param types types
   * @param guid  guid
   * @param out   out
   * @param in    in
   * @param us    us
   * @return true if the auth was successful and false if it failed.
   * @throws IOException on failure
   */
  public boolean auth(SaslMode mode, EnumSet<AuthScheme> types, String guid, OutputStream out, InputStream in, Socket us) throws IOException {
    String luid;
    String kernelUid = null;

    long uid = POSIXFactory.getJavaPOSIX().getuid();
    luid = stupidlyEncode("" + uid);

    Command c;
    EnumSet<AuthScheme> failed = EnumSet.noneOf(AuthScheme.class);
    AuthScheme current = AUTH_NONE;
    SaslAuthState state = SaslAuthState.INITIAL_STATE;

    while (state != SaslAuthState.FINISHED && state != SaslAuthState.FAILED) {

      LOGGER.trace("Mode: {} AUTH state: {}", mode, state);

      switch (mode) {
        case CLIENT:
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
                  // CHECK GP -- Always false
                  // when asking for file descriptor support, ERROR means FD support is not supported
                  //noinspection ConstantConditions
                  if (state == SaslAuthState.NEGOTIATE_UNIX_FD) {
                    state = SaslAuthState.FINISHED;
                    LOGGER.trace("File descriptors NOT supported by server");
                    fileDescriptorSupported = false;
                    send(out, BEGIN);
                  } else {
                    send(out, CANCEL);
                    state = SaslAuthState.WAIT_REJECT;
                  }
                  break;
                case OK:
                  LOGGER.trace("Authenticated");
                  // Check GP
                  // state = SaslAuthState.AUTHENTICATED;

                  if (hasFileDescriptorSupport) {
                    // Check GP
                    // state = SaslAuthState.WAIT_DATA;
                    state = SaslAuthState.NEGOTIATE_UNIX_FD;
                    LOGGER.trace("Asking for file descriptor support");
                    // if authentication was successful, ask remote end for file descriptor support
                    send(out, SaslCommand.NEGOTIATE_UNIX_FD);
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
          break;
        case SERVER:
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
          break;
        default:
          return false;
      }
    }

    return state == SaslAuthState.FINISHED;
  }





  public boolean isFileDescriptorSupported() {
    return fileDescriptorSupported;
  }
/*
  // static builer for the state machine logic
  private static StateMachine<SaslAuthState, SaslCommand> configureClientStateMachine() throws Exception {
    StateMachineBuilder.Builder<SaslAuthState, SaslCommand> builder = StateMachineBuilder.builder();
    builder.configureStates().withStates()
        .initial(INITIAL_STATE)
        .end(END_STATE)
        .states(EnumSet.allOf(SaslAuthState.class));

    builder.configureTransitions()
        .withExternal()
          .source(INITIAL_STATE).target(INITIALIZED).event()
          .and()
        .withExternal()
          .source("S1").target("SF").event("E2");

    StateMachine<String, String> machine = builder.build();
  }

 */
}