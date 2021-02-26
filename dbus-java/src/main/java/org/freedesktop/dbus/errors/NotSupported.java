package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if operation not supported
 */
@SuppressWarnings("serial")
public class NotSupported extends DBusExecutionException {
  public NotSupported(String message) {
    super(message);
  }
}