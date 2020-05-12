/*
   D-Bus Java Implementation
   Copyright (c) 2005-2006 Matthew Johnson
   Copyright (c) 2017-2019 David M.

   This program is free software; you can redistribute it and/or modify it
   under the terms of either the GNU Lesser General Public License Version 2 or the
   Academic Free Licence Version 2.1.

   Full licence texts are included in the LICENSE file with this program.
*/

package org.freedesktop.dbus.messages;

import java.lang.annotation.Annotation;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.freedesktop.dbus.Marshalling;
import org.freedesktop.dbus.MethodTuple;
import org.freedesktop.dbus.StrongReference;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.DBusExecutionException;
import org.freedesktop.dbus.interfaces.DBusInterface;

public class ExportedObject {
  private final Map<MethodTuple, Method> methods;
  private final Reference<DBusInterface> object;
  private final StringBuilder introspectiondata;

  public ExportedObject(DBusInterface _object, boolean _weakreferences) throws DBusException {
    if (_weakreferences) {
      this.object = new WeakReference<>(_object);
    } else {
      this.object = new StrongReference<>(_object);
    }
    introspectiondata = new StringBuilder();
    methods = extractExportedMethods(_object.getClass());
    introspectiondata
        .append(" <interface name=\"org.freedesktop.DBus.Introspectable\">\n" + "  <method name=\"Introspect\">\n")
        .append("   <arg type=\"s\" direction=\"out\"/>\n" + "  </method>\n" + " </interface>\n")
        .append(" <interface name=\"org.freedesktop.DBus.Peer\">\n" + "  <method name=\"Ping\">\n")
        .append("  </method>\n" + " </interface>\n");
  }

  private String extractAnnotations(AnnotatedElement c) {
    StringBuilder ans = new StringBuilder();
    for (Annotation a : c.getDeclaredAnnotations()) {

      if (!a.annotationType().isAssignableFrom(DBusInterface.class)) { // skip all interfaces not compatible with
        // DBusInterface (mother of all DBus
        // related interfaces)
        continue;
      }
      Class<?> t = a.annotationType();
      String value = "";
      try {
        Method m = t.getMethod("value");
        value = m.invoke(a).toString();
      } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException _ex) {
        // ignore
      }

      ans
          .append("  <annotation name=\"")
          .append(AbstractConnection.DOLLAR_PATTERN.matcher(t.getName()).replaceAll("."))
          .append("\" value=\"")
          .append(value)
          .append("\" />\n");
    }
    return ans.toString();
  }

  private Map<MethodTuple, Method> extractExportedMethods(Class<?> c) throws DBusException {
    if (DBusInterface.class.equals(c)) {
      return new HashMap<>();
    }
    Map<MethodTuple, Method> m = new HashMap<>();
    for (Class<?> i : c.getInterfaces()) {
      if (DBusInterface.class.equals(i)) {
        // add this class's public methods
        if (null != c.getAnnotation(DBusInterfaceName.class)) {
          String name = c.getAnnotation(DBusInterfaceName.class).value();
          introspectiondata
              .append(" <interface name=\"")
              .append(name)
              .append("\">\n");
          DBusSignal.addInterfaceMap(c.getName(), name);
        } else {
          // don't let people export things which don't have a
          // valid D-Bus interface name
          if (c.getName().equals(c.getSimpleName())) {
            throw new DBusException("DBusInterfaces cannot be declared outside a package");
          }
          if (c.getName().length() > AbstractConnection.MAX_NAME_LENGTH) {
            throw new DBusException(
                "Introspected interface name exceeds 255 characters. Cannot export objects of type "
                    + c.getName());
          } else {
            introspectiondata
                .append(" <interface name=\"")
                .append(AbstractConnection.DOLLAR_PATTERN.matcher(c.getName()).replaceAll("."))
                .append("\">\n");
          }
        }
        introspectiondata.append(extractAnnotations(c));
        for (Method meth : c.getDeclaredMethods()) {
          if (Modifier.isPublic(meth.getModifiers())) {
            StringBuilder ms = new StringBuilder();
            String name;
            if (meth.isAnnotationPresent(DBusMemberName.class)) {
              name = meth.getAnnotation(DBusMemberName.class).value();
            } else {
              name = meth.getName();
            }
            if (name.length() > AbstractConnection.MAX_NAME_LENGTH) {
              throw new DBusException(
                  "Introspected method name exceeds 255 characters. Cannot export objects with method "
                      + name);
            }
            introspectiondata
                .append("  <method name=\"")
                .append(name)
                .append("\" >\n")
                .append(extractAnnotations(meth));
            for (Class<?> ex : meth.getExceptionTypes()) {
              if (DBusExecutionException.class.isAssignableFrom(ex)) {
                introspectiondata
                    .append("   <annotation name=\"org.freedesktop.DBus.Method.Error\" value=\"")
                    .append(AbstractConnection.DOLLAR_PATTERN.matcher(ex.getName()).replaceAll("."))
                    .append("\" />\n");
              }
            }
            for (Type pt : meth.getGenericParameterTypes()) {
              for (String s : Marshalling.getDBusType(pt)) {
                introspectiondata
                    .append("   <arg type=\"")
                    .append(s)
                    .append("\" direction=\"in\"/>\n");
                ms.append(s);
              }
            }
            if (!Void.TYPE.equals(meth.getGenericReturnType())) {
              if (Tuple.class.isAssignableFrom(meth.getReturnType())) {
                ParameterizedType tc = (ParameterizedType) meth.getGenericReturnType();
                Type[] ts = tc.getActualTypeArguments();

                for (Type t : ts) {
                  if (t != null) {
                    for (String s : Marshalling.getDBusType(t)) {
                      introspectiondata
                          .append("   <arg type=\"")
                          .append(s)
                          .append("\" direction=\"out\"/>\n");
                    }
                  }
                }
              } else if (Object[].class.equals(meth.getGenericReturnType())) {
                throw new DBusException("Return type of Object[] cannot be introspected properly");
              } else {
                for (String s : Marshalling.getDBusType(meth.getGenericReturnType())) {
                  introspectiondata
                      .append("   <arg type=\"")
                      .append(s)
                      .append("\" direction=\"out\"/>\n");
                }
              }
            }
            introspectiondata.append("  </method>\n");
            m.put(new MethodTuple(name, ms.toString()), meth);
          }
        }
        for (Class<?> sig : c.getDeclaredClasses()) {
          if (DBusSignal.class.isAssignableFrom(sig)) {
            String name;
            if (sig.isAnnotationPresent(DBusMemberName.class)) {
              name = sig.getAnnotation(DBusMemberName.class).value();
              DBusSignal.addSignalMap(sig.getSimpleName(), name);
            } else {
              name = sig.getSimpleName();
            }
            if (name.length() > AbstractConnection.MAX_NAME_LENGTH) {
              throw new DBusException(
                  "Introspected signal name exceeds 255 characters. Cannot export objects with signals of type "
                      + name);
            }
            introspectiondata
                .append("  <signal name=\"")
                .append(name)
                .append("\">\n");
            Constructor<?> con = sig.getConstructors()[0];
            Type[] ts = con.getGenericParameterTypes();
            for (int j = 1; j < ts.length; j++) {
              for (String s : Marshalling.getDBusType(ts[j])) {
                introspectiondata
                    .append("   <arg type=\"")
                    .append(s)
                    .append("\" direction=\"out\" />\n");
              }
            }
            introspectiondata
                .append(extractAnnotations(sig))
                .append("  </signal>\n");

          }
        }
        introspectiondata.append(" </interface>\n");
      } else {
        // recurse
        m.putAll(extractExportedMethods(i));
      }
    }
    return m;
  }

  public Map<MethodTuple, Method> getMethods() {
    return methods;
  }

  public Reference<DBusInterface> getObject() {
    return object;
  }

  public String getIntrospectiondata() {
    return introspectiondata.toString();
  }

}
