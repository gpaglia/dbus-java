package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a pair request for a device failed because the authentication timed out
 */
@SuppressWarnings("serial")
public class AuthenticationTimeout extends DBusExecutionException {
  public AuthenticationTimeout(String message) {
    super(message);
  }
}