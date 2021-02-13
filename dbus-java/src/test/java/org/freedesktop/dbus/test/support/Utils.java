package org.freedesktop.dbus.test.support;

public class Utils {
  public static void sleep(long delay) {
    try {
      Thread.sleep(delay);
    } catch (InterruptedException ignored) { }
  }
}
