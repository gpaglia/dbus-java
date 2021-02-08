package org.freedesktop.dbus.connections.transports;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.EnumSet;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.freedesktop.dbus.connections.BusAddress;
import org.freedesktop.dbus.connections.sasl.AuthScheme;
import org.freedesktop.dbus.connections.sasl.SaslStateMachine;
import org.freedesktop.dbus.connections.sasl.SaslMode;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.spi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.freedesktop.dbus.connections.sasl.AuthScheme.*;
import static org.freedesktop.dbus.connections.sasl.SaslStateMachine.createSaslStateMachine;

/**
 * Base class for all transport types.
 *
 * @author hypfvieh
 * @since v3.2.0 - 2019-02-08
 */
public abstract class AbstractTransport implements Closeable {

  ServiceLoader<ISocketProvider> spiLoader = ServiceLoader.load(ISocketProvider.class);

  private final Logger logger;
  private final BusAddress address;

  private SaslMode saslMode;

  private EnumSet<AuthScheme> saslAuthModes;
  private IMessageReader inputReader;
  private IMessageWriter outputWriter;

  private boolean fileDescriptorSupported;

  AbstractTransport(BusAddress _address) {
    address = _address;

    if (_address.isListeningSocket()) {
      saslMode = SaslMode.SERVER;
    } else {
      saslMode = SaslMode.CLIENT;
    }

    saslAuthModes = EnumSet.of(AUTH_NONE);
    logger = LoggerFactory.getLogger(getClass());
  }

  /**
   * Write a message to the underlying socket.
   *
   * @param _msg message to write
   * @throws IOException on write error or if output was already closed or null
   */
  public void writeMessage(Message _msg) throws IOException {
    if (!fileDescriptorSupported && Message.ArgumentType.FILEDESCRIPTOR == _msg.getType()) {
      throw new IllegalArgumentException("File descriptors are not supported!");
    }
    if (outputWriter != null && !outputWriter.isClosed()) {
      outputWriter.writeMessage(_msg);
    } else {
      throw new IOException("OutputWriter already closed or null");
    }
  }

  /**
   * Read a message from the underlying socket.
   *
   * @return read message, maybe null
   * @throws IOException   when input already close or null
   * @throws DBusException when message could not be converted to a DBus message
   */
  public Message readMessage() throws IOException, DBusException {
    if (inputReader != null && !inputReader.isClosed()) {
      return inputReader.readMessage();
    }
    throw new IOException("InputReader already closed or null");
  }

  /**
   * Abstract method implemented by concrete sub classes to establish a connection
   * using whatever transport type (e.g. TCP/Unix socket).
   *
   * @throws IOException when connection fails
   */
  abstract void connect() throws IOException;

  /**
   * Method to indicate if passing of file descriptors is allowed.
   *
   * @return true to allow FD passing, false otherwise
   */
  abstract boolean hasFileDescriptorSupport();

  /**
   * Helper method to authenticate to DBus using SASL.
   *
   * @param _out  output stream
   * @param _in   input stream
   * @param _sock socket
   * @throws IOException on any error
   */
  protected void authenticate(OutputStream _out, InputStream _in, Socket _sock) throws IOException {
    SaslStateMachine sasl = createSaslStateMachine(saslMode, hasFileDescriptorSupport());
    if (!sasl.auth(saslAuthModes, address.getGuid(), _out, _in, _sock)) {
      _out.close();
      throw new IOException("Failed to auth");
    }
    fileDescriptorSupported = sasl.isFileDescriptorSupported();
  }

  /**
   * Setup message reader/writer.
   * Will look for SPI provider first, if none is found default implementation is used.
   * The default implementation does not support file descriptor passing!
   *
   * @param _socket socket to use
   */
  protected void setInputOutput(Socket _socket) {
    try {
      for (ISocketProvider provider : spiLoader) {
        logger.debug("Found ISocketProvider {}", provider);

        provider.setFileDescriptorSupport(hasFileDescriptorSupport() && fileDescriptorSupported);
        inputReader = provider.createReader(_socket);
        outputWriter = provider.createWriter(_socket);
        if (inputReader != null && outputWriter != null) {
          logger.debug("Using ISocketProvider {}", provider);
          break;
        }
      }
    } catch (ServiceConfigurationError _ex) {
      logger.error("Could not initialize service provider.", _ex);
    } catch (IOException _ex) {
      logger.error("Could not initialize alternative message reader/writer.", _ex);
    }

    try {
      if (inputReader == null || outputWriter == null) {
        logger.debug("No alternative ISocketProvider found, using built-in implementation.  "
                + "inputReader = {}, outputWriter = {}",
            inputReader,
            outputWriter);
        inputReader = new InputStreamMessageReader(_socket.getInputStream());
        outputWriter = new OutputStreamMessageWriter(_socket.getOutputStream());
      }
    } catch (IOException _ex) {
      logger.error("Could not initialize default message reader/writer.", _ex);
    }

  }

  @SuppressWarnings("unused")
  protected EnumSet<AuthScheme> getSaslAuthModes() {
    return saslAuthModes;
  }

  protected void setSaslAuthModes(EnumSet<AuthScheme> _saslAuthMode) {
    saslAuthModes = _saslAuthMode;
  }

  @SuppressWarnings("unused")
  protected SaslMode getSaslMode() {
    return saslMode;
  }

  @SuppressWarnings("unused")
  protected void setSaslMode(SaslMode _saslMode) {
    saslMode = _saslMode;
  }

  protected BusAddress getAddress() {
    return address;
  }

  protected Logger getLogger() {
    return logger;
  }

  @Override
  public void close() throws IOException {
    inputReader.close();
    outputWriter.close();
  }

}
