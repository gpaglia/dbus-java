package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if [pairing or other] procedure was canceled
 */
@SuppressWarnings("serial")
public class Canceled extends DBusExecutionException {
  public Canceled(String message) {
    super(message);
  }
}