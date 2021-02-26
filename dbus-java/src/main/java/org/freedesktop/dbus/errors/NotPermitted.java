package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if operation not permitted
 */
@SuppressWarnings("serial")
public class NotPermitted extends DBusExecutionException {
  public NotPermitted(String message) {
    super(message);
  }
}