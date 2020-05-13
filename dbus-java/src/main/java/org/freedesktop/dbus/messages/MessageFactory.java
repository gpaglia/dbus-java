package org.freedesktop.dbus.messages;

import lombok.extern.slf4j.Slf4j;
import org.freedesktop.Hexdump;
import org.freedesktop.dbus.FileDescriptor;
import org.freedesktop.dbus.errors.Error;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.exceptions.MessageTypeException;

import java.util.List;

@Slf4j
public class MessageFactory {

  public static Message createMessage(
      byte _type,
      byte[] _buf,
      byte[] _header,
      byte[] _body,
      List<FileDescriptor> filedescriptors
  ) throws DBusException, MessageTypeException {
    Message m;
    switch (_type) {
      case Message.MessageType.METHOD_CALL:
        m = new MethodCall();
        break;
      case Message.MessageType.METHOD_RETURN:
        m = new MethodReturn();
        break;
      case Message.MessageType.SIGNAL:
        m = new DBusSignal();
        break;
      case Message.MessageType.ERROR:
        m = new Error();
        break;
      default:
        throw new MessageTypeException(String.format("Message type %s unsupported", _type));
    }

    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(Hexdump.format(_buf));
      LOGGER.trace(Hexdump.format(_header));
      LOGGER.trace(Hexdump.format(_body));
    }

    m.populate(_buf, _header, _body, filedescriptors);
    return m;
  }

}
