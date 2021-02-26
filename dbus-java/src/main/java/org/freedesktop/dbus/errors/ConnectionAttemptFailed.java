package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a connection attempt for a [bluetooth or network] device fails
 */
@SuppressWarnings("serial")
public class ConnectionAttemptFailed extends DBusExecutionException {
  public ConnectionAttemptFailed(String message) {
    super(message);
  }
}