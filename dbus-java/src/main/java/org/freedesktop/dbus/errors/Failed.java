package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a requested operation failed
 */
@SuppressWarnings("serial")
public class Failed extends DBusExecutionException {
  public Failed(String message) {
    super(message);
  }
}