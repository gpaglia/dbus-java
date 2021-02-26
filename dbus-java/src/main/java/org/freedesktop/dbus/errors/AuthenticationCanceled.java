package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a pair request for a device failed because the authentication process was canceled
 */
@SuppressWarnings("serial")
public class AuthenticationCanceled extends DBusExecutionException {
  public AuthenticationCanceled(String message) {
    super(message);
  }
}