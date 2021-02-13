package org.freedesktop.dbus.connections.sasl;

import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.support.Hexdump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SaslUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(SaslUtils.class);
  public static final int LOCK_TIMEOUT = 1000;
  public static final int NEW_KEY_TIMEOUT_SECONDS = 60 * 5;
  public static final int EXPIRE_KEYS_TIMEOUT_SECONDS = NEW_KEY_TIMEOUT_SECONDS + (60 * 2);
  public static final int MAX_TIME_TRAVEL_SECONDS = 60 * 5;
  public static final int COOKIE_TIMEOUT = 240;
  public static final String COOKIE_CONTEXT = "org_freedesktop_java";

  static String findCookie(String context, String ID) throws IOException {
    String homedir = System.getProperty("user.home");
    File f = new File(homedir + "/.dbus-keyrings/" + context);
    BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
    String s;
    String lCookie = null;
    long now = System.currentTimeMillis() / 1000;
    while (null != (s = r.readLine())) {
      String[] line = s.split(" ");
      long timestamp = Long.parseLong(line[1]);
      if (line[0].equals(ID) && (!(timestamp < 0 || (now + MAX_TIME_TRAVEL_SECONDS) < timestamp || (now - EXPIRE_KEYS_TIMEOUT_SECONDS) > timestamp))) {
        lCookie = line[2];
        break;
      }
    }
    r.close();
    return lCookie;
  }

  static void addCookie(String _context, String _id, long _timestamp, String _cookie) throws IOException {
    String homedir = System.getProperty("user.home");
    File keydir = new File(homedir + "/.dbus-keyrings/");
    File cookiefile = new File(homedir + "/.dbus-keyrings/" + _context);
    File lock = new File(homedir + "/.dbus-keyrings/" + _context + ".lock");
    File temp = new File(homedir + "/.dbus-keyrings/" + _context + ".temp");

    // ensure directory exists
    if (!keydir.exists()) {
      //noinspection ResultOfMethodCallIgnored
      keydir.mkdirs();
    }

    // acquire lock
    long start = System.currentTimeMillis();
    //noinspection StatementWithEmptyBody
    while (!lock.createNewFile() && LOCK_TIMEOUT > (System.currentTimeMillis() - start)) {
    }

    // read old file
    List<String> lines = new ArrayList<>();
    if (cookiefile.exists()) {
      BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(cookiefile)));
      String s;
      while (null != (s = r.readLine())) {
        String[] line = s.split(" ");
        long time = Long.parseLong(line[1]);
        // expire stale cookies
        if ((_timestamp - time) < COOKIE_TIMEOUT) {
          lines.add(s);
        }
      }
      r.close();
    }

    // add cookie
    lines.add(_id + " " + _timestamp + " " + _cookie);

    // write temp file
    PrintWriter w = new PrintWriter(new FileOutputStream(temp));
    for (String l : lines) {
      w.println(l);
    }
    w.close();

    // atomically move to old file
    if (!temp.renameTo(cookiefile)) {
      if (!cookiefile.delete()) {
        LOGGER.error("Error deleting cookiefile {}", cookiefile.toString());
        throw new DBusExecutionException("Failed delete of cookiefile");
      }
      if (!temp.renameTo(cookiefile)) {
        LOGGER.error("Failed rename of cookiefile {} to {}", temp, cookiefile);
        throw new DBusExecutionException("Failed rename of cookiefile");
      }
    }

    // remove lock
    if (!lock.delete()) {
      LOGGER.error("Failed delete of file {}", lock);
      throw new DBusExecutionException("Failed delete of lock file");
    }
  }

  /**
   * Takes the string, encodes it as hex and then turns it into a string again.
   * No, I don't know why either.
   */
  static String stupidlyEncode(String data) {
    return Hexdump.toHex(data.getBytes()).replaceAll(" ", "");
  }

  static String stupidlyEncode(byte[] data) {
    return Hexdump.toHex(data).replaceAll(" ", "");
  }

  static byte getNibble(char c) {
    switch (c) {
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
        return (byte) (c - '0');
      case 'A':
      case 'B':
      case 'C':
      case 'D':
      case 'E':
      case 'F':
        return (byte) (c - 'A' + 10);
      case 'a':
      case 'b':
      case 'c':
      case 'd':
      case 'e':
      case 'f':
        return (byte) (c - 'a' + 10);
      default:
        return 0;
    }
  }

  static String stupidlyDecode(String data) {
    char[] cs = new char[data.length()];
    char[] res = new char[cs.length / 2];
    data.getChars(0, data.length(), cs, 0);
    for (int i = 0, j = 0; j < res.length; i += 2, j++) {
      int b = 0;
      b |= getNibble(cs[i]) << 4;
      b |= getNibble(cs[i + 1]);
      res[j] = (char) b;
    }
    return new String(res);
  }
}
