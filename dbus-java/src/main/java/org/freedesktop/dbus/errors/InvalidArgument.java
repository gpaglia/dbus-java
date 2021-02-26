package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if argument(s) is(are) invalid
 */
@SuppressWarnings("serial")
public class InvalidArgument extends DBusExecutionException {
  public InvalidArgument(String message) {
    super(message);
  }
}