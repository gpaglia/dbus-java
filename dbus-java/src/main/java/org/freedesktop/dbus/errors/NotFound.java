package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if requested resource was not found
 */
@SuppressWarnings("serial")
public class NotFound extends DBusExecutionException {
  public NotFound(String message) {
    super(message);
  }
}