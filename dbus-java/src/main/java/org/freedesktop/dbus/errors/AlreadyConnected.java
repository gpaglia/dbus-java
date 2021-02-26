package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a connection request fails because device is already connected
 */
@SuppressWarnings("serial")
public class AlreadyConnected extends DBusExecutionException {
  public AlreadyConnected(String message) {
    super(message);
  }
}