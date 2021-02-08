package org.freedesktop.dbus.connections.sasl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.Collator;
import java.util.Arrays;
import java.util.EnumSet;

import static org.freedesktop.dbus.connections.sasl.AuthScheme.*;
import static org.freedesktop.dbus.connections.sasl.SaslCommand.*;

public class Command {
  private static final Logger LOGGER = LoggerFactory.getLogger(Command.class);

  private SaslCommand command;
  private EnumSet<AuthScheme> mechsSet = EnumSet.noneOf(AuthScheme.class);
  private AuthScheme mechs = AUTH_NONE;
  private String data;
  private String response;

  private static final Collator col = Collator.getInstance();

  static {
    col.setDecomposition(Collator.FULL_DECOMPOSITION);
    col.setStrength(Collator.PRIMARY);
  }

  public Command() {
  }

  public Command(String s) throws IOException {
    String[] ss = s.split(" ");
    LOGGER.trace("Creating command from: {}", Arrays.toString(ss));
    if (0 == col.compare(ss[0], "OK")) {
      command = SaslCommand.OK;
      data = ss[1];
    } else if (0 == col.compare(ss[0], "AUTH")) {
      command = AUTH;
      if (ss.length > 1) {
        if (0 == col.compare(ss[1], AUTH_EXTERNAL.getCommandName())) {
          mechs = AUTH_EXTERNAL;
        } else if (0 == col.compare(ss[1], AUTH_SHA.getCommandName())) {
          mechs = AUTH_SHA;
        } else if (0 == col.compare(ss[1], AUTH_ANON.getCommandName())) {
          mechs = AUTH_ANON;
        } else {
          mechs = AUTH_NONE;
        }
      }
      if (ss.length > 2) {
        data = ss[2];
      }
    } else if (0 == col.compare(ss[0], "DATA")) {
      command = DATA;
      data = ss[1];
    } else if (0 == col.compare(ss[0], "REJECTED")) {
      command = REJECTED;
      for (int i = 1; i < ss.length; i++) {
        if (0 == col.compare(ss[i], AUTH_EXTERNAL.getCommandName())) {
          mechsSet.add( AUTH_EXTERNAL);
        } else if (0 == col.compare(ss[i], AUTH_SHA.getCommandName())) {
          mechsSet.add(AUTH_SHA);
        } else if (0 == col.compare(ss[i], AUTH_ANON.getCommandName())) {
          mechsSet.add(AUTH_ANON);
        }
      }
    } else if (0 == col.compare(ss[0], "BEGIN")) {
      command = BEGIN;
    } else if (0 == col.compare(ss[0], "CANCEL")) {
      command = CANCEL;
    } else if (0 == col.compare(ss[0], "ERROR")) {
      command = ERROR;
      data = ss[1];
    } else if (0 == col.compare(ss[0], "NEGOTIATE_UNIX_FD")) {
      command = SaslCommand.NEGOTIATE_UNIX_FD;
    } else if (0 == col.compare(ss[0], "AGREE_UNIX_FD")) {
      command = AGREE_UNIX_FD;
    } else {
      throw new IOException("Invalid Command " + ss[0]);
    }
    LOGGER.trace("Created command: {}", this);
  }
  public SaslCommand getCommand() {
    return command;
  }

  public EnumSet<AuthScheme> getMechsSet() {
    return mechsSet;
  }

  public AuthScheme getMechs() { return mechs; }

  public String getData() {
    return data;
  }

  public String getResponse() {
    return response;
  }

  public void setResponse(String s) {
    response = s;
  }

  @Override
  public String toString() {
    return "Command(" + command + ", " + mechsSet + ", " + data + ")";
  }

}