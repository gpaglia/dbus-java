package org.freedesktop.dbus.bin;

import org.freedesktop.dbus.connections.impl.DirectConnection;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

/**
 *
 */
public class EmbeddedDBusDaemonTest {

  @Test
  public void test_start_stop() throws Exception {

    String address = DirectConnection.createDynamicTCPSession();
    for (int i = 0; i < 2; i++) {

      // initialize
      EmbeddedDBusDaemon daemon = new EmbeddedDBusDaemon();
      daemon.setAddress(address);

      // start the daemon in background to not block the test
      AtomicReference<Exception> exception = new AtomicReference<>();
      Thread daemonThread = new Thread(() -> {
        try {
          daemon.startInForeground();
        } catch (Exception ex) {
          exception.set(ex);
        }
      });
      daemonThread.start();

      // give the daemon time to start
      Thread.sleep(1000);
      daemon.close();
      assertThat(exception.get(), nullValue());
    }
  }
}
