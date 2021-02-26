package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if releasing a resource that is not acquired
 */
@SuppressWarnings("serial")
public class NotAcquired extends DBusExecutionException {
  public NotAcquired(String message) {
    super(message);
  }
}