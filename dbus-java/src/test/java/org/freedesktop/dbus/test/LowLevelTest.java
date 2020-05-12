package org.freedesktop.dbus.test;

import com.github.hypfvieh.util.FileIoUtil;
import com.github.hypfvieh.util.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.connections.transports.AbstractTransport;
import org.freedesktop.dbus.connections.transports.TransportFactory;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.fixtures.TestDaemonFixtures;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.messages.MethodCall;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

@Slf4j
public class LowLevelTest {
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
  public void testLowLevel() throws IOException, DBusException {
    String addr = getAddress();
    LOGGER.debug(addr);
    BusAddress address = new BusAddress(addr);
    LOGGER.debug(address + "");

    try (AbstractTransport conn = TransportFactory.createTransport(address)) {
      Message m = new MethodCall("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "Hello", (byte) 0, null);
      conn.writeMessage(m);
      Utils.sleep(250);
      m = conn.readMessage();
      LOGGER.debug(m.getClass() + "");
      LOGGER.debug(m + "");
      m = conn.readMessage();
      LOGGER.debug(m.getClass() + "");
      LOGGER.debug(m + "");
      m = new MethodCall("org.freedesktop.DBus", "/", null, "Hello", (byte) 0, null);
      conn.writeMessage(m);
      // Utils.sleep(250);
      m = conn.readMessage();
      LOGGER.debug(m + "");

      m = new MethodCall("org.freedesktop.DBus", "/org/freedesktop/DBus", "org.freedesktop.DBus", "RequestName", (byte) 0, "su", "org.testname", 0);
      conn.writeMessage(m);
      // Utils.sleep(250);
      m = conn.readMessage();
      LOGGER.debug(m + "");
      m = new DBusSignal(null, "/foo", "org.foo", "Foo", null);
      conn.writeMessage(m);
      // Utils.sleep(250);
      m = conn.readMessage();
      LOGGER.debug(m + "");
    }
  }

  static String getAddress() throws DBusException {
    String s = System.getenv("DBUS_SESSION_BUS_ADDRESS");
    if (s == null) {
      // address gets stashed in $HOME/.dbus/session-bus/`dbus-uuidgen --get`-`sed 's/:\(.\)\..*/\1/' <<<
      // $DISPLAY`
      String display = System.getenv("DISPLAY");
      if (null == display) {
        throw new RuntimeException("Cannot Resolve Session Bus Address");
      }
      if (!display.startsWith(":") && display.contains(":")) { // display seems to be a remote display
        // (e.g. X forward through SSH)
        display = display.substring(display.indexOf(':'));
      }

      String uuid = DBusConnection.getDbusMachineId();
      String homedir = System.getProperty("user.home");
      File addressfile = new File(homedir + "/.dbus/session-bus",
          uuid + "-" + display.replaceAll(":([0-9]*)\\..*", "$1"));
      if (!addressfile.exists()) {
        throw new RuntimeException("Cannot Resolve Session Bus Address");
      }
      Properties readProperties = FileIoUtil.readProperties(addressfile);
      String sessionAddress = readProperties.getProperty("DBUS_SESSION_BUS_ADDRESS");
      if (StringUtil.isEmpty(sessionAddress)) {
        throw new RuntimeException("Cannot Resolve Session Bus Address");
      }
      return sessionAddress;
    }

    return s;
  }
}
