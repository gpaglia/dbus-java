package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if value length is invalid
 */
@SuppressWarnings("serial")
public class InvalidValueLength extends DBusExecutionException {
  public InvalidValueLength(String message) {
    super(message);
  }
}