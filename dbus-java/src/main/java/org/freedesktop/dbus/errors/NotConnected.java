package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if device not connected (and connection required for requested operation)
 */
@SuppressWarnings("serial")
public class NotConnected extends DBusExecutionException {
  public NotConnected(String message) {
    super(message);
  }
}