package org.freedesktop.dbus.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.impl.DBusConnection.DBusBusType;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.fixtures.TestDaemonFixtures;
import org.freedesktop.dbus.test.helper.interfaces.TwoPartInterface;
import org.freedesktop.dbus.test.helper.interfaces.TwoPartObject;
import org.freedesktop.dbus.test.helper.twopart.TwoPartTestClient.TwoPartTestObject;
import org.freedesktop.dbus.test.helper.twopart.TwoPartTestServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@SuppressWarnings("BusyWait")
public class TestTwoPart {

  private volatile boolean testDone = false;
  private volatile boolean serverReady = false;

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
  public void testTwoPart() throws InterruptedException {
    TwoPartServer twoPartServer = new TwoPartServer();
    twoPartServer.start();

    while (!serverReady) {
      Thread.sleep(1500L);
    }

    try {
      System.out.println("get conn");
      DBusConnection conn = DBusConnection.newConnection(DBusBusType.SESSION);

      System.out.println("get remote");
      TwoPartInterface remote = conn.getRemoteObject(
          "org.freedesktop.dbus.test.two_part_server",
          "/",
          TwoPartInterface.class
      );

      assertNotNull(remote);

      System.out.println("get object");
      TwoPartObject o = remote.getNew();
      assertNotNull(o);

      System.out.println("get name");
      assertEquals("give name", o.getName());

      TwoPartTestObject tpto = new TwoPartTestObject();
      conn.exportObject("/TestObject", tpto);
      conn.sendMessage(new TwoPartInterface.TwoPartSignal("/FromObject", tpto));

      try {
        Thread.sleep(1000);
      } catch (InterruptedException ignored) {
      }

      conn.disconnect();
    } catch (DBusException _ex) {
      _ex.printStackTrace();
      fail("Exception in client");
    } finally {
      testDone = true;
    }
  }

  private class TwoPartServer extends Thread {

    @Override
    public void run() {
      DBusConnection conn;
      try {
        conn = DBusConnection.newConnection(DBusBusType.SESSION);
        conn.requestBusName("org.freedesktop.dbus.test.two_part_server");
        TwoPartTestServer server = new TwoPartTestServer(conn);
        conn.exportObject("/", server);
        conn.addSigHandler(TwoPartInterface.TwoPartSignal.class, server);
        serverReady = true;
        while (!testDone) {
          try {
            Thread.sleep(500L);
          } catch (InterruptedException ignored) {
          }
        }
      } catch (DBusException _ex) {
        _ex.printStackTrace();
        fail("Exception in server");
      }

    }

  }
}
