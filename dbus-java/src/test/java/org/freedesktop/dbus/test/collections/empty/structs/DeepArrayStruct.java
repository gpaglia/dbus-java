/*
   D-Bus Java Implementation
   Copyright (c) 2019 Technolution BV

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.test.collections.empty.structs;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.test.helper.structs.IntStruct;

public final class DeepArrayStruct extends Struct implements IEmptyCollectionStruct<IntStruct[][][]> {

  @Position(0)
  private final IntStruct[][][] list;

  @Position(1)
  private final String validationValue;

  public DeepArrayStruct(IntStruct[][][] list, String validationValue) {
    this.list = list.clone();
    this.validationValue = validationValue;
  }

  @Override
  public IntStruct[][][] getValue() {
    return list.clone();
  }

  @Override
  public String getValidationValue() {
    return validationValue;
  }

  @Override
  public String getStringTestValue() {
    StringBuilder string = new StringBuilder("[");
    for (IntStruct[][] l1 : list) {
      string.append("[");
      for (IntStruct[] l2 : l1) {
        string.append("[");
        for (IntStruct e : l2) {
          string.append(e.toSimpleString());
        }
        string.append("]");
      }
      string.append("]");
    }
    string.append("]");

    return string.toString();

  }

  @Override
  public boolean isEmpty() {
    return list.length == 0;
  }
}
