package org.freedesktop.dbus.test.helper.signals;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.DBusPath;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.annotations.IntrospectionDescription;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;
import org.freedesktop.dbus.test.helper.structs.SampleStruct2;
import org.freedesktop.dbus.types.UInt32;

public interface SampleSignals extends DBusInterface {

  @SuppressWarnings("unused")
  class TestStringSignal extends DBusSignal {
    private final String contentString;

    public TestStringSignal(String _path, String _aoeu) throws DBusException {
      super(_path, _aoeu);
      this.contentString = _aoeu;
    }

    @SuppressWarnings("unused")
    public String getContentString() {
      return contentString;
    }
  }

  @IntrospectionDescription("Test basic signal")
  class TestSignal extends DBusSignal {
    private final String value;
    private final UInt32 number;

    /**
     * Create a signal.
     */
    public TestSignal(String _path, String _value, UInt32 _number) throws DBusException {
      super(_path, _value, _number);
      this.value = _value;
      this.number = _number;
    }

    public String getValue() {
      return value;
    }

    @SuppressWarnings("unused")
    public UInt32 getNumber() {
      return number;
    }
  }

  @IntrospectionDescription("Test basic signal")
  class TestRenamedSignal extends DBusSignal {
    private final String value;
    private final UInt32 number;

    /**
     * Create a signal.
     */
    public TestRenamedSignal(String _path, String _value, UInt32 _number) throws DBusException {
      super(_path, _value, _number);
      this.value = _value;
      this.number = _number;
    }

    public String getValue() {
      return value;
    }

    @SuppressWarnings("unused")
    public UInt32 getNumber() {
      return number;
    }
  }

  class TestPathSignal extends DBusSignal {
    public final DBusPath otherpath;
    public final List<DBusPath> pathlist;
    public final Map<DBusPath, DBusPath> pathmap;

    public TestPathSignal(String _path, DBusPath _otherpath, List<DBusPath> _pathlist, Map<DBusPath, DBusPath> _pathmap) throws DBusException {
      super(_path, _otherpath, _pathlist, _pathmap);
      this.otherpath = _otherpath;
      this.pathlist = _pathlist;
      this.pathmap = _pathmap;
    }
  }

  @IntrospectionDescription("Test signal sending an object path")
  @DBusMemberName("TestSignalObject")
  class TestObjectSignal extends DBusSignal {
    public final DBusInterface otherpath;

    public TestObjectSignal(String _path, DBusInterface _otherpath) throws DBusException {
      super(_path, _otherpath);
      this.otherpath = _otherpath;
    }
  }

  class TestEmptySignal extends DBusSignal {
    public TestEmptySignal(String path) throws DBusException {
      super(path);
    }
  }

  @IntrospectionDescription("Test signal with arrays")
  class TestArraySignal extends DBusSignal {
    private final List<SampleStruct2> listOfStruct;
    private final Map<UInt32, SampleStruct2> mapOfIntStruct;

    public TestArraySignal(String _path, List<SampleStruct2> _listOfStruct, Map<UInt32, SampleStruct2> _mapOfIntStruct) throws DBusException {
      super(_path, _listOfStruct, _mapOfIntStruct);
      this.listOfStruct = _listOfStruct;
      this.mapOfIntStruct = _mapOfIntStruct;
    }

    public Map<UInt32, SampleStruct2> getMapOfIntStruct() {
      return mapOfIntStruct;
    }

    public List<SampleStruct2> getListOfStruct() {
      return listOfStruct;
    }
  }
}
