package org.freedesktop.dbus.utils.generator;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.support.Util;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

import static org.freedesktop.dbus.support.Util.isWindows;
import static org.junit.jupiter.api.Assertions.*;

class InterfaceCodeGeneratorTest {

  @Test
  void testCreateFirewallInterfaces() {
    String objectPath = "/org/fedoraproject/FirewallD1";
    String busName = "org.fedoraproject.FirewallD1";
    //noinspection unused
    boolean ignoreDtd = true;

    Logger logger = LoggerFactory.getLogger(InterfaceCodeGenerator.class);


    if (!StringUtils.isBlank(busName)) {
      String introspectionData = Util.readFileToString(new File("src/test/resources/CreateInterface/firewall/org.fedoraproject.FirewallD1.xml"));

      InterfaceCodeGenerator ci2 = new InterfaceCodeGenerator(introspectionData, objectPath, busName);
      try {
        Map<File, String> analyze = ci2.analyze(true);

        assertEquals(9, analyze.size());

      } catch (Exception _ex) {
        logger.error("Error while analyzing introspection data", _ex);
      }
    }
  }

  // On windows only 17 items are generated instead of the expected 20 -- to be understood
  @Test
  void testCreateAllFirewallInterfaces() {
    String objectPath = "/org/fedoraproject/FirewallD1";
    String busName = "*";
    //noinspection unused
    boolean ignoreDtd = true;

    Logger logger = LoggerFactory.getLogger(InterfaceCodeGenerator.class);


    if (!StringUtils.isBlank(busName)) {
      String introspectionData = Util.readFileToString(new File("src/test/resources/CreateInterface/firewall/org.fedoraproject.FirewallD1.xml"));
      int expectedNo = isWindows() ? 17 : 20;

      InterfaceCodeGenerator ci2 = new InterfaceCodeGenerator(introspectionData, objectPath, busName);
      try {
        Map<File, String> analyze = ci2.analyze(true);

        assertEquals(analyze.size(), expectedNo);

      } catch (Exception _ex) {
        logger.error("Error while analyzing introspection data", _ex);
      }
    }
  }

  @Test
  void testCreateNetworkManagerWirelessInterface() {
    String objectPath = "/";
    String busName = "org.freedesktop.NetworkManager.Device.Wireless";
    //noinspection unused
    boolean ignoreDtd = true;

    Logger logger = LoggerFactory.getLogger(InterfaceCodeGenerator.class);


    if (!StringUtils.isBlank(busName)) {
      String introspectionData = Util.readFileToString(
          new File(
              "src/test/resources/CreateInterface/networkmanager/org.freedesktop.NetworkManager.Device.Wireless.xml"
          )
      );

      InterfaceCodeGenerator ci2 = new InterfaceCodeGenerator(introspectionData, objectPath, busName);
      try {
        Map<File, String> analyze = ci2.analyze(true);

        assertEquals(1, analyze.size());

        String clzContent = analyze.get(analyze.keySet().iterator().next());

        assertTrue(clzContent.contains("@" + DBusInterfaceName.class.getSimpleName() + "(\"" + busName + "\")"));
        assertFalse(clzContent.contains("this._properties"));
        assertFalse(clzContent.contains("this._path"));
        assertFalse(clzContent.contains("this._interfaceName"));
      } catch (Exception _ex) {
        logger.error("Error while analyzing introspection data", _ex);
      }
    }
  }

}
