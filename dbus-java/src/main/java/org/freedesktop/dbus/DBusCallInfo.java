/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus;

import org.freedesktop.dbus.messages.Message;

/**
 * Holds information on a method call
 */
public class DBusCallInfo {
  /**
   * Indicates the caller won't wait for a reply (and we won't send one).
   */
  @SuppressWarnings("unused")
  public static final int NO_REPLY = Message.Flags.NO_REPLY_EXPECTED;
  @SuppressWarnings("unused")
  public static final int ASYNC = 0x100;
  private final String source;
  private final String destination;
  private final String objectPath;
  private final String iface;
  private final String method;
  private final int flags;

  public DBusCallInfo(Message m) {
    this.source = m.getSource();
    this.destination = m.getDestination();
    this.objectPath = m.getPath();
    this.iface = m.getInterface();
    this.method = m.getName();
    this.flags = m.getFlags();
  }

  /**
   * Returns the BusID which called the method.
   *
   * @return source
   */
  public String getSource() {
    return source;
  }

  /**
   * Returns the name with which we were addressed on the Bus.
   *
   * @return destination
   */
  @SuppressWarnings("unused")
  public String getDestination() {
    return destination;
  }

  /**
   * Returns the object path used to call this method.
   *
   * @return objectpath
   */
  public String getObjectPath() {
    return objectPath;
  }

  /**
   * Returns the interface this method was called with.
   *
   * @return interface
   */
  public String getInterface() {
    return iface;
  }

  /**
   * Returns the method name used to call this method.
   *
   * @return method
   */
  public String getMethod() {
    return method;
  }

  /**
   * Returns any flags set on this method call.
   *
   * @return flags
   */
  @SuppressWarnings("unused")
  public int getFlags() {
    return flags;
  }
}
