package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown from HealthDevice1
 */
@SuppressWarnings("serial")
public class HealthError extends DBusExecutionException {
  public HealthError(String message) {
    super(message);
  }
}