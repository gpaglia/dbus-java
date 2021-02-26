package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if param out of range
 */
@SuppressWarnings("serial")
public class OutOfRange extends DBusExecutionException {
  public OutOfRange(String message) {
    super(message);
  }
}