package org.freedesktop.dbus.connections;

import lombok.extern.slf4j.Slf4j;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.FatalException;
import org.freedesktop.dbus.messages.Message;

import java.util.Objects;

@Slf4j
public class IncomingMessageThread extends Thread {

  private boolean terminate;
  private final AbstractConnection connection;

  public IncomingMessageThread(AbstractConnection _connection) {
    Objects.requireNonNull(_connection);
    connection = _connection;
    setName("DBusConnection");
    setDaemon(true);
  }

  public void setTerminate(boolean _terminate) {
    terminate = _terminate;
    interrupt();
  }

  @Override
  public void run() {

    Message msg;
    LOGGER.trace("Reader thread loop starting...");
    while (!terminate) {

        // read from the wire
      try {
        // this blocks on outgoing being non-empty or a message being available.
        msg = connection.readIncoming();
        if (msg != null) {
          LOGGER.trace("Got Incoming Message: {}", msg);

          connection.handleMessage(msg);

        }
      } catch (DBusException _ex) {
        if (_ex instanceof FatalException) {
          LOGGER.error("FatalException in connection thread.", _ex);
          if (connection.isConnected()) {
            connection.disconnect();
            setTerminate(true);
          }
        }

        if (!terminate) { // only log exceptions if the connection was not intended to be closed
          LOGGER.error("Exception in connection thread.", _ex);
        }
      }
    }
    LOGGER.trace("Reader thread terminated");
  }
}
