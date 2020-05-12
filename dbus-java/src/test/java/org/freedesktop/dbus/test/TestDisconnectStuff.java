package org.freedesktop.dbus.test;

import static org.junit.jupiter.api.Assertions.fail;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.NotConnected;
import org.freedesktop.dbus.fixtures.TestDaemonFixtures;
import org.freedesktop.dbus.test.helper.SampleClass;
import org.freedesktop.dbus.test.helper.interfaces.SampleRemoteInterface;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

public class TestDisconnectStuff {

  private static final TestDaemonFixtures fixt = new TestDaemonFixtures();

  @BeforeAll
  public static void setup() throws DBusException {
    fixt.setup();
  }

  @AfterAll
  public static void teardown() throws IOException {
    fixt.teardown();
  }


  @Test
  public void testStuffAfterDisconnect() throws DBusException, InterruptedException {

    DBusConnection serverConnection = DBusConnection.getConnection(DBusBusType.SESSION);
    DBusConnection clientConnection = DBusConnection.getConnection(DBusBusType.SESSION);
    serverConnection.setWeakReferences(true);
    clientConnection.setWeakReferences(true);

    serverConnection.requestBusName("foo.bar.why.again.disconnect.Test");

    SampleClass tclass = new SampleClass(serverConnection);

    serverConnection.exportObject("/Test2001", tclass);

    SampleRemoteInterface tri =
        clientConnection.getRemoteObject("foo.bar.why.again.disconnect.Test", "/Test2001", SampleRemoteInterface.class);


    // Call a method when disconnected 
    try {
      clientConnection.disconnect();
      Thread.sleep(1000L);
      serverConnection.disconnect();
      Thread.sleep(1000L);
      System.out.println("getName() suceeded and returned: " + tri.getName());
      fail("Should not succeed when disconnected");
    } catch (NotConnected ignored) { }
  }
}
