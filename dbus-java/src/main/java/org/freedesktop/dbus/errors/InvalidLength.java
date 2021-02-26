package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if length is invalid
 */
@SuppressWarnings("serial")
public class InvalidLength extends DBusExecutionException {
  public InvalidLength(String message) {
    super(message);
  }
}