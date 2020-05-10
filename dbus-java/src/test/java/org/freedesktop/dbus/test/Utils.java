package org.freedesktop.dbus.test;

public class Utils {
  public static void sleep(int delay) {
    try {
      Thread.sleep(delay);
    } catch (InterruptedException e) {
      //
    }
  }
}
