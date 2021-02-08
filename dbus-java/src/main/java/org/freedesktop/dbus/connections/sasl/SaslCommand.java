package org.freedesktop.dbus.connections.sasl;

public enum SaslCommand {
  AUTH,
  DATA,
  REJECTED,
  OK,
  BEGIN,
  CANCEL,
  ERROR,
  NEGOTIATE_UNIX_FD,
  AGREE_UNIX_FD
}