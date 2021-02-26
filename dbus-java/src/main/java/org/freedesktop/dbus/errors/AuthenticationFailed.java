package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a pair request for a device failed because the authentication process failed
 */
@SuppressWarnings("serial")
public class AuthenticationFailed extends DBusExecutionException {
  public AuthenticationFailed(String message) {
    super(message);
  }
}