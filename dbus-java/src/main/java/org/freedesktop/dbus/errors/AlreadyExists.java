package org.freedesktop.dbus.errors;

import org.freedesktop.dbus.exceptions.DBusExecutionException;

/**
 * Thrown if a connection or pair request for a device failed because the device already exists
 *    in the hierarchy or in general registration of an object failed because object already exists in hierarchy
 */
@SuppressWarnings("serial")
public class AlreadyExists extends DBusExecutionException {
  public AlreadyExists(String message) {
    super(message);
  }
}