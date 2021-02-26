package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if offset is invalid
 */
@SuppressWarnings("serial")
public class InvalidOffset extends DBusExecutionException {
  public InvalidOffset(String message) {
    super(message);
  }
}