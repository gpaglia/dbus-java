package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if not ready for requested operation
 */
@SuppressWarnings("serial")
public class NotReady extends DBusExecutionException {
  public NotReady(String message) {
    super(message);
  }
}