package sample.issue;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.fixtures.TestDaemonFixtures;
import org.freedesktop.dbus.interfaces.DBusInterface;

import java.io.IOException;
/*
gpaglia edit: on windows, modify the run configuration to set the DBUS_SESSION_BUS_ADDRESS
 */
public class ExportClass implements DBusInterface {

  private static final TestDaemonFixtures fixt = new TestDaemonFixtures();

  @Override
  public boolean isRemote() {
    return false;
  }

  @Override
  public String getObjectPath() {
    return "/";
  }

  public static void main(String[] args) throws DBusException, InterruptedException, IOException, IllegalStateException {

    fixt.setup();

    try (DBusConnection conn = DBusConnection.getConnection(DBusConnection.DBusBusType.SESSION)) {

      conn.requestBusName("sample.issue");

      ExportClass ex = new ExportClass();
      conn.exportObject("/path", ex);

      System.out.println("Exported object, waiting");
      Thread.sleep(5000);

      conn.unExportObject("/path");
      System.out.println("Unexported object, waiting");

      Thread.sleep(5000);

    } finally {
      fixt.teardown();
    }
  }

}
