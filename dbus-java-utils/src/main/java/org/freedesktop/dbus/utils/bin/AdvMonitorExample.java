package org.freedesktop.dbus.utils.bin;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.ObjectManager;
import org.freedesktop.dbus.types.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Implementation of the python example in bluez sources (test/example-adv-monitor)
 *
 * @see <a href="https://git.kernel.org/pub/scm/bluetooth/bluez.git/tree/test/example-adv-monitor">Python example from bluez source tree</a>
 */
public class AdvMonitorExample {

  private static final Logger LOGGER = LoggerFactory.getLogger(AdvMonitorExample.class);

  private static final String DBUS_OM_INTERFACE = "org.freedesktop.DBus.ObjectManager";
  private static final String DBUS_PROPERTIES_INTERFACE = "org.freedesktop.DBus.Properties";
  private static final String BLUEZ_SERVICE_NAME = "org.bluez";

  private static final String ADV_MONITOR_MANAGER_IFACE = "org.bluez.AdvertisementMonitorManager1";
  private static final String ADV_MONITOR_IFACE = "org.bluez.AdvertisementMonitor1";
  private static final String ADV_MONITOR_APP_BASE_PATH = "/org/bluez/example/adv_monitor_app";

  // Indexes of the Monitor object parameters in a monitor data list.
  private static final int MONITOR_TYPE = 0;
  private static final int RSSI_FILTER = 1;
  private static final int PATTERNS = 2;

  // Indexes of the RSSI filter parameters in a monitor data list.
  private static final int RSSI_H_THRESH = 0;
  private static final int RSSI_H_TIMEOUT = 1;
  private static final int RSSI_L_THRESH = 2;
  private static final int RSSI_L_TIMEOUT = 3;

  // Indexes of the Patterns filter parameters in a monitor data list.
  private static final int PATTERN_START_POS = 0;
  private static final int PATTERN_AD_TYPE = 1;
  private static final int PATTERN_DATA = 2;

  private static final int CONNECTION_TIMEOUT = 5000; // in ms

  private final DBusConnection conn;
  private final String busName;
  private final ObjectManager om;

  public static void main(String[] args) {
    final int appId = (args.length == 0 || !args[1].matches("[0-9]+")) ? 0 : Integer.valueOf(args[1]);

    LOGGER.info("Setting app_id to %d\n", appId);

    try (DBusConnection conn = DBusConnection.getConnection(
        DBusConnection.DBusBusType.SYSTEM,
        false,
        CONNECTION_TIMEOUT)
    ) {
      final AdvMonitorExample ame = new AdvMonitorExample(conn, appId);


    } catch (DBusException de) {
      LOGGER.error("Got DBusException", de);
    } finally {
      // no op
    }
  }

  public AdvMonitorExample(final DBusConnection conn, final int appId) throws DBusException {
    this.busName = ADV_MONITOR_APP_BASE_PATH + appId;
    this.conn = conn;

    
    LOGGER.info("Unique bus name {}", this.conn.getUniqueName());

    this.om = conn.getRemoteObject(BLUEZ_SERVICE_NAME, "/", ObjectManager.class);

    final Map<DBusPath, Map<String, Map<String, Variant<?>>>> objects = om.GetManagedObjects();

    // list all paths discovered
    for (DBusPath p: objects.keySet()) {
      final Map<String, Map<String, Variant<?>>> ifaces = objects.get(p);

      LOGGER.info(" ... discovered path {}", p.toString());

      for (String iface: ifaces.keySet()) {
        LOGGER.info("... -- with interface {}", iface);
      }
    }

    // find the adv monitor manager object if any ...
    final Map.Entry<DBusPath, Map<String, Map<String, Variant<?>>>> found = om
        .GetManagedObjects()
        .entrySet()
        .stream()
        .filter(e -> e.getValue().containsKey(ADV_MONITOR_MANAGER_IFACE))
        .findFirst()
        .orElse(null);

    if (found == null) {
      LOGGER.info("No advertisement monitor manager found, exiting ...");
      System.exit(1);
    }

    LOGGER.info("Found advertisement monitor manager, listing proeprties ...");
    for (Map.Entry<String, Variant<?>> prop : found.getValue().get(ADV_MONITOR_MANAGER_IFACE).entrySet()) {
      LOGGER.info(" ... property {} -> {}", prop.getKey(), prop.getValue().toString());
    }
  }
}
