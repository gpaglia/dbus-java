package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if operation not authorized
 */
@SuppressWarnings("serial")
public class NotAuthorized extends DBusExecutionException {
  public NotAuthorized(String message) {
    super(message);
  }
}