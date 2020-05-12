/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.test.helper;

import org.freedesktop.dbus.interfaces.DBusSerializable;

import java.util.ArrayList;
import java.util.List;

public class SampleSerializable<A> implements DBusSerializable {
  private int first;
  private String second;
  private List<Integer> third;

  public SampleSerializable(int _a, A _b, List<Integer> _c) {
    this.first = _a;
    this.second = _b.toString();
    this.third = _c;
  }

  @SuppressWarnings("unused")
  public SampleSerializable() {
  }

  @SuppressWarnings("unused")
  public void deserialize(int _a, String _b, List<Integer> _c) {
    this.first = _a;
    this.second = _b;
    this.third = new ArrayList<>(_c);
  }

  @Override
  public Object[] serialize() {
    return new Object[]{
        first, second, third
    };
  }

  public int getInt() {
    return first;
  }

  public String getString() {
    return second;
  }

  public List<Integer> getList() {
    return third;
  }

  @Override
  public String toString() {
    return "TestSerializable{" + first + "," + second + "," + third + "}";
  }
}
