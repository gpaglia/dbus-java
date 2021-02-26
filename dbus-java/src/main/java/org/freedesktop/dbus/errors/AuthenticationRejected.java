package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a pair request for a device failed because the authentication was rejected by counterpart
 */
@SuppressWarnings("serial")
public class AuthenticationRejected extends DBusExecutionException {
  public AuthenticationRejected(String message) {
    super(message);
  }
}