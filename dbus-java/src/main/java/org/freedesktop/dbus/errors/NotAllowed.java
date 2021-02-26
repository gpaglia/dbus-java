package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if operation not allowed
 */
@SuppressWarnings("serial")
public class NotAllowed extends DBusExecutionException {
  public NotAllowed(String message) {
    super(message);
  }
}