package org.freedesktop.dbus.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Indicates that a DBus interface or method is deprecated
 */
@SuppressWarnings("unused")
@Retention(RetentionPolicy.RUNTIME)
@DBusInterfaceName("org.freedesktop.DBus.Deprecated")
public @interface DeprecatedOnDBus {
}