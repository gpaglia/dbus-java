package org.freedesktop.dbus.connections.sasl;

enum SaslAuthState {
  INITIAL_STATE,
  WAIT_DATA,
  WAIT_OK,
  WAIT_REJECT,
  WAIT_AUTH,
  WAIT_BEGIN,
  AUTHENTICATED,
  NEGOTIATE_UNIX_FD,
  FINISHED,
  FAILED,
  END_STATE
}