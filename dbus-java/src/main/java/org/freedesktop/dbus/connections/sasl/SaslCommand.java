package org.freedesktop.dbus.connections.sasl;

import java.text.Collator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public enum SaslCommand {
  AUTH("AUTH"),
  DATA("DATA"),
  REJECTED("REJECTED"),
  OK("OK"),
  BEGIN("BEGIN"),
  CANCEL("CANCEL)"),
  ERROR("ERROR"),
  NEGOTIATE_UNIX_FD("NEGOTIATE_UNIX_FD"),
  AGREE_UNIX_FD("AGREE_UNIX_FD");

  private final String name;

  private static final Collator col = Collator.getInstance();

  private static final Map<String, SaslCommand> commandNameMap = new TreeMap<>(col);

  static {
    col.setDecomposition(Collator.FULL_DECOMPOSITION);
    col.setStrength(Collator.PRIMARY);
    for (SaslCommand cmd : SaslCommand.values()) {
      commandNameMap.put(cmd.name, cmd);
    }
  }

  SaslCommand(String name) {
    this.name = name;
  }

  public String getCommandName() { return name; }

  public static SaslCommand fromCommandName(String name) { return commandNameMap.get(name); }
}