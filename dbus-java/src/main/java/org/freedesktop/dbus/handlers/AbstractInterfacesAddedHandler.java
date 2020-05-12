package org.freedesktop.dbus.handlers;

import org.freedesktop.dbus.connections.AbstractConnection;
import org.freedesktop.dbus.interfaces.DBusSigHandler;
import org.freedesktop.dbus.interfaces.ObjectManager.InterfacesAdded;


/**
 * Subclass this abstract class for creating a callback for InterfaceAdded signal provided by DBus ObjectManager.
 * <p>
 * As soon as your callback is registered by calling {@link AbstractConnection#addSigHandler(Class, DBusSigHandler)},
 * all property changes by DBus will be visible in the handle(DBusSigHandler) method of your callback class.
 */
@SuppressWarnings("unused")
public abstract class AbstractInterfacesAddedHandler extends AbstractSignalHandlerBase<org.freedesktop.dbus.interfaces.ObjectManager.InterfacesAdded> {

  @Override
  public final Class<InterfacesAdded> getImplementationClass() {
    return org.freedesktop.dbus.interfaces.ObjectManager.InterfacesAdded.class;
  }


}
