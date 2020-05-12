package org.freedesktop.dbus.fixtures;

import org.freedesktop.dbus.bin.EmbeddedDBusDaemon;
import org.freedesktop.dbus.exceptions.DBusException;

import java.io.IOException;

public class TestDaemonFixtures {
  private EmbeddedDBusDaemon daemon;

  public TestDaemonFixtures() {}

  public void setup() throws DBusException {
    String address = System.getenv("DBUS_SESSION_BUS_ADDRESS");
    if (address == null) {
      throw new DBusException("DBUS_SESSION_BUS_ADDRESS not set");
    }

    if (! address.toLowerCase().contains("listen")) {
      address = address + ",listen=true";
    }
    daemon = new EmbeddedDBusDaemon();
    daemon.setAddress(address);
    daemon.startInBackground();
  }

  public void teardown() throws IOException {
    if (daemon != null) {
      daemon.close();
    }
  }

  public EmbeddedDBusDaemon getDaemon() { return daemon; }

}
