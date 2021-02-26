package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if request or operation was rejected
 */
@SuppressWarnings("serial")
public class Rejected extends DBusExecutionException {
  public Rejected(String message) {
    super(message);
  }
}