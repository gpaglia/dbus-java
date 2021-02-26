package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a requested resource does not exist
 */
@SuppressWarnings("serial")
public class DoesNotExist extends DBusExecutionException {
  public DoesNotExist(String message) {
    super(message);
  }
}