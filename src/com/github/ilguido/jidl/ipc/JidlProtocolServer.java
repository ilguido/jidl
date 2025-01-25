/**
 * JidlProtocolServer.java
 *
 * Copyright (c) 2024 Stefano Guidoni
 *
 * This file is part of jidl.
 *
 * jidl is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jidl is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jidl.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.github.ilguido.jidl.ipc;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import javax.net.ServerSocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.ilguido.jidl.ipc.JidlProtocol;
import com.github.ilguido.jidl.ipc.JidlProtocolException;
import com.github.ilguido.jidl.ipc.JidlProtocolStatusCode;
import com.github.ilguido.jidl.ipc.RequestHandlerInterface;
import com.github.ilguido.jidl.utils.FileManager;

/**
 * JidlProtocolServer
 * A class to manage a server for the Jidl Protocol.  It is based
 * on the ProcBribdge protocol.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */
 
public class JidlProtocolServer {
  /**
   * This server is listening at this port.
   */
  private final int port;
  
  /**
   * The {@link com.github.ilguido.jidl.ipc.RequestHandler} used to process the
   * request.
   */
  private final RequestHandlerInterface requestHandler;
  
  /**
   * The context used to generate the server socket.
   */
  private final SSLContext sslContext;
  
  /**
   * An executor to run the server on a separate thread.
   */
  private ExecutorService executor;
  
  /**
   * The socket of the server
   */
  private SSLServerSocket serverSocket;
  
  /**
   * Flag of server started.
   */
  private boolean started;

  /**
   * Initializes the server.  It can use an SSL context to generate the server
   * socket. If the SSL context is <code>null</code>, the default context will
   * be used instead.
   *
   * @param inPort the port of the server
   * @param inRequestHandler the object that actually handles the requests
   *                         received by the server
   * @param inSSLContext a context to be used to generate the server socket, it
   *                     can be <code>null</code>
   */
  public JidlProtocolServer(final int inPort, 
                            RequestHandlerInterface inRequestHandler,
                            final SSLContext inSSLContext) 
    throws IllegalArgumentException {
    this.port = inPort;
    this.requestHandler = inRequestHandler;

    this.sslContext = inSSLContext;
    
    this.started = false;
    this.executor = null;
    this.serverSocket = null;
  }

  /**
   * Returns <code>true</code> if the server was started.  This does not
   * strictly mean that it is running now.
   *
   * @return <code>true</code> if the server was started
   */
  public final synchronized boolean isStarted() {
    return started;
  }

  /**
   * Returns the port of the server.
   *
   * @return the port of the server
   */
  public final int getPort() {
    return port;
  }

  /**
   * Starts the server.  It quietly does nothing, if the server was already
   * started.
   *
   * @throws IOException if the socket could not be created
   */
  public synchronized void start() 
    throws IOException {
    if (started) {
      return;
      //throw new IllegalStateException("server already started");
    }
    /* Get a ServerSocketFactory from the SSLContext passed to this server.
     * If the context is null, use the default.
     * Having a specific SSLContext is useful when the server must use and trust
     * some specific certificate.
     */
    final ServerSocketFactory factory = (sslContext == null? 
                                           SSLServerSocketFactory.getDefault():
                                           sslContext.getServerSocketFactory());
    final SSLServerSocket serverSocket;
    
    /* The following paragraph could throw an IOException. */
    serverSocket = (SSLServerSocket) factory.createServerSocket(this.port);
    serverSocket.setNeedClientAuth(true);
    serverSocket.setEnabledCipherSuites(new String[] { 
                                        "TLS_RSA_WITH_AES_128_GCM_SHA256" });
    serverSocket.setEnabledProtocols(new String[] { "TLSv1.2" });

    this.serverSocket = serverSocket;

    /* Run the server on a separate thread. */
    final ExecutorService executor = Executors.newCachedThreadPool();
    this.executor = executor;
    executor.execute(() -> {
      while (true) {
        try {
          Socket socket = serverSocket.accept();
          JSConnection conn = new JSConnection(socket, requestHandler);
          synchronized (JidlProtocolServer.this) {
            if (!started) {
              return; // finish listener
            }
            executor.execute(conn);
          }
        } catch (IOException ioe) {
          return; // finish listener
        }
      }
    });

    /* If we arrived to the end of it, it means that the server started. */
    started = true;
  }

  /**
   * Stops the server.  It also sets to <code>null</code> <code>executor</code>
   * and <code>serverSocket</code>.
   */
  public synchronized void stop() {
    if (!started) {
      return;
      //throw new IllegalStateException("server does not started");
    }

    executor.shutdown();
    executor = null;

    try {
      serverSocket.close();
    } catch (IOException ignored) {
    }
    serverSocket = null;

    this.started = false;
  }

  /**
   * JSConnection
   * A class to manage a connection to the JidlProtocolServer.
   *
   * @version 0.8
   * @author Stefano Guidoni
   */
  private final class JSConnection implements Runnable {
    /**
     * Socket of the connection.  It is the same socket as that of the server.
     */
    private final Socket socket;
    
    /**
     * The {@link com.github.ilguido.jidl.ipc.RequestHandler} used to process the
     * request.  It is the same of the server.
     */
    private final RequestHandlerInterface requestHandler;

    /**
     * Initializes the connection with its socket and request handler.
     *
     * @param inSocket the socket of the server
     * @param inRequestHandler the object that actually handles the requests
     *                         received by the server through this connection
     */
    JSConnection(final Socket inSocket, 
                 final RequestHandlerInterface inRequestHandler) {
      this.socket = inSocket;
      this.requestHandler = inRequestHandler;
    }

    /**
     * Handles the request and writes the response.
     */
    @Override
    public void run() {
      try {
        OutputStream os = new BufferedOutputStream(socket.getOutputStream());
        InputStream is = new BufferedInputStream(socket.getInputStream());

        Map.Entry<String, JsonObject> req = JidlProtocol.readRequest(is);
        String method = req.getKey();
        JsonObject payload = req.getValue();

        JsonObject result = null;
        Exception exception = null;
        try {
          result = requestHandler.handleRequest(method, payload);
        } catch (Exception e) {
          exception = e;
        }

        if (exception != null) {
          //TODO: more informative text?
          JidlProtocol.writeResponse(os, 
                   JidlProtocolStatusCode.BAD_RESPONSE_FAILED_REQUEST_HANDLING);
        } else {
          JidlProtocol.writeResponse(os, result);
        }
      } catch (Exception e) {
        //e.printStackTrace(logger);
      }
    }

  }

}
