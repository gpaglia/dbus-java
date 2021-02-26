package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown to signal operation is in progress already
 */
@SuppressWarnings("serial")
public class InProgress extends DBusExecutionException {
  public InProgress(String message) {
    super(message);
  }
}