package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if resource not available
 */
@SuppressWarnings("serial")
public class NotAvailable extends DBusExecutionException {
  public NotAvailable(String message) {
    super(message);
  }
}