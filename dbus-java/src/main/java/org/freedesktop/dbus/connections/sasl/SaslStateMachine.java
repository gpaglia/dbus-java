package org.freedesktop.dbus.connections.sasl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.*;
import java.net.Socket;
import java.text.Collator;
import java.util.EnumSet;

public abstract class SaslStateMachine {
  private final Logger LOGGER = LoggerFactory.getLogger(getClass());

  protected static final Collator col = Collator.getInstance();

  /**
   * Factory method to create a state machine instance in the client or server mode
   *
   */
  public static SaslStateMachine createSaslStateMachine(final SaslMode mode, final boolean _hasFileDescriptorSupport)
      throws IllegalArgumentException
  {
    switch (mode) {
      case CLIENT:
        return new SaslClientStateMachine(_hasFileDescriptorSupport);
      case SERVER:
        return new SaslServerStateMachine(_hasFileDescriptorSupport);
      default:
        throw new IllegalArgumentException("Unsupported mode: " + mode);
    }
  }

  static {
    col.setDecomposition(Collator.FULL_DECOMPOSITION);
    col.setStrength(Collator.PRIMARY);
  }

  private String challenge = "";
  private String cookie = "";

  /**
   * whether file descriptor passing is supported on the current connection.
   */
  protected final boolean hasFileDescriptorSupport;

  /**
   * Create a new SASL auth handler.
   * Defaults to disable file descriptor passing.
   */
  protected SaslStateMachine() {
    this(false);
  }

  /**
   * Create a new SASL auth handler.
   *
   * @param _hasFileDescriptorSupport true to support file descriptor passing (usually only works with UNIX_SOCKET).
   */
  protected SaslStateMachine(boolean _hasFileDescriptorSupport) {
    hasFileDescriptorSupport = _hasFileDescriptorSupport;

  }

  protected Command receive(InputStream s) throws IOException {
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

  protected void send(OutputStream out, SaslCommand command, String... data) throws IOException {
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

  /**
   * To be implemented by client and server subclasses.
   *
   * Performs SASL auth on the given streams.
   * Mode selects whether to run as a SASL server or client.
   * Types is a bitmask of the available auth types.
   *
   * @param types An EnumSet of available {@link AuthScheme}s
   * @param guid  the guid
   * @param out   The {@link OutputStream} for outbund communication towards server
   * @param in    The {@link InputStream} for inbound communication from server
   * @param us    The {@link Socket} backing the input and output streams
   * @return true if the auth was successful and false if it failed.
   * @throws IOException on failure
   */
  public abstract boolean auth(EnumSet<AuthScheme> types, String guid, OutputStream out, InputStream in, Socket us) throws IOException;

  public boolean isFileDescriptorSupported() {
    return false;
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