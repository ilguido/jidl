/**
 * JidlProtocolClient.java
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
import java.rmi.ServerException;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import javax.net.SocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.ilguido.jidl.ipc.JidlProtocol;
import com.github.ilguido.jidl.ipc.JidlProtocolException;
import com.github.ilguido.jidl.ipc.JidlProtocolStatusCode;
import com.github.ilguido.jidl.ipc.RequestHandlerInterface;
import com.github.ilguido.jidl.utils.FileManager;

/**
 * JidlProtocolClient
 * A class to manage a client for the Jidl Protocol.  It is based
 * on the ProcBribdge protocol.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class JidlProtocolClient {
  /**
   * The host name or IP address of the remote server.
   */
  private final String host;
  
  /**
   * The port of the remote server.
   */
  private final int port;
  
  /**
   * A connection timeout in milliseconds.
   */
  private final long timeout;
  
  /**
   * An executor to launch the client on a separate thread.
   */
  private final ExecutorService executor;

  /**
   * Constant equivalent to an infinite timeout.
   */
  public static final long FOREVER = 0;

  /**
   * Initializes the client and sets a timeout for connections.  It also 
   * requires the certificates to establish trusted connections. You can pass a
   * <code>null</code> value for the executor, but you should consider to
   * instantiate a common executor, if you plan to instantiate this client 
   * multiple times. Creating a new executor for each request takes time and 
   * resources: it is not good for repeated requests, it is absolutely bad 
   * for multiple concurrent requests.
   *
   * @param inHost the host name or IP address of the remote server
   * @param inPort the port of the remote server
   * @param inTimeout the timeout of the connection
   * @param inExecutorService an executor for the client thread or 
   *                          <code>null</code>
   * @param inKeyStorePassword the password to open the certificate file
   * @param inTrustStore the path to the file with the trusted certificates
   * @param inTrustStorePassword the password to open the file with the trusted
   *                             certificates
   * @throws IllegalArgumentException if at least one of the files does not 
   *                                  exist, or the path is <code>null</code>
   */
  public JidlProtocolClient(String inHost, 
                            int inPort, 
                            long inTimeout, 
                            ExecutorService inExecutorService,
                            final String inKeyStore,
                            final String inKeyStorePassword,
                            final String inTrustStore,
                            final String inTrustStorePassword) {
    this.host = inHost;
    this.port = inPort;
    this.timeout = inTimeout;
    if (inExecutorService != null) {
      this.executor = inExecutorService;
    } else {
      final ExecutorService executor = Executors.newSingleThreadExecutor();
      this.executor = executor;
    }
    
    System.setProperty("javax.net.ssl.keyStore", inKeyStore);
    System.setProperty("javax.net.ssl.keyStorePassword", inKeyStorePassword);
    System.setProperty("javax.net.ssl.trustStore", inTrustStore);
    System.setProperty("javax.net.ssl.trustStorePassword", 
                       inTrustStorePassword);
  }

  /**
   * Initializes the client and does not set a timeout for connections.  It also
   * requires the certificates to establish trusted connections. You can pass a
   * <code>null</code> value for the executor, but you should consider to
   * instantiate a common executor, if you plan to instantiate this client 
   * multiple times. Creating a new executor for each request takes time and 
   * resources: it is not good for repeated requests, it is absolutely bad 
   * for multiple concurrent requests.
   *
   * @param inHost the host name or IP address of the remote server
   * @param inPort the port of the remote server
   * @param inExecutorService an executor for the client thread or 
   *                          <code>null</code>
   * @param inKeyStorePassword the password to open the certificate file
   * @param inTrustStore the path to the file with the trusted certificates
   * @param inTrustStorePassword the password to open the file with the trusted
   *                             certificates
   * @throws IllegalArgumentException if at least one of the files does not 
   *                                  exist, or the path is <code>null</code>
   */
  public JidlProtocolClient(String host, 
                            int port,
                            ExecutorService inExecutorService,
                            final String inKeyStore,
                            final String inKeyStorePassword,
                            final String inTrustStore,
                            final String inTrustStorePassword) {
    this(host, port, FOREVER, inExecutorService, inKeyStore, inKeyStorePassword,
         inTrustStore, inTrustStorePassword);
  }

  /**
   * Returns the host name of the server.
   *
   * @return the host name of the server
   */
  public final String getHost() {
    return host;
  }

  /**
   * Returns the port of the remote server.
   *
   * @return the port of the server
   */
  public final int getPort() {
    return port;
  }

  /**
   * Returns the time out.  It is zero, if there is not a timeout.
   *
   * @return the time out in milliseconds
   */
  public long getTimeout() {
    return timeout;
  }

  /**
   * Returns the executor.
   */
  public ExecutorService getExecutorService() {
    return executor;
  }

  /**
   * Executes a request to the remote server.
   *
   * @param inMethod the method called by the request, it is a string which
   *                 is a meaningful command for the receiving server
   * @param inData some json data or <code>null</code>
   * @throws IOException if writing the request failed
   * @throws RuntimeException if the client got interrupted after sending the
   *                          request
   * @throws ServerException if the response from the server is not positive
   */
  public final JsonObject request(String inMethod, 
                              JsonObject inData) 
    throws IOException, RuntimeException, ServerException {
     // response from the server
    final JidlProtocolStatusCode[] respStatusCode = { null };
    // data received from the server
    final JsonObject[] respPayload = { null };
    /* If the client thread throws an exception, put it here:
     */
    final Throwable[] innerException = { null };

    SocketFactory factory = SSLSocketFactory.getDefault();
    
    try (final SSLSocket socket = (SSLSocket) factory.createSocket(host, port)) 
      {
      socket.setEnabledCipherSuites(new String[] { "TLS_RSA_WITH_AES_128_GCM_SHA256" });
      socket.setEnabledProtocols(new String[] { "TLSv1.2" });
      Runnable task = () -> {
        try {
          OutputStream os = new BufferedOutputStream(socket.getOutputStream());
          InputStream is = new BufferedInputStream(socket.getInputStream());

          JidlProtocol.writeRequest(os, inMethod, inData);
          Map.Entry<JidlProtocolStatusCode, JsonObject> entry = 
                                                  JidlProtocol.readResponse(is);
          respStatusCode[0] = entry.getKey();
          respPayload[0] = entry.getValue();

        } catch (Exception ex) {
          innerException[0] = ex;
        }
      };

      if (timeout <= 0) {
        task.run();
      } else {
        Future future = executor.submit(task);

        try {
          future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
          future.cancel(true);
          throw new IOException(e);
        } catch (Exception e) {
          throw new IOException(e);
        } finally {
          executor.shutdownNow();
        }
      }
    }

    if (innerException[0] != null) {
      throw new RuntimeException(innerException[0]);
    }

    if (!JidlProtocolStatusCode.isGood(respStatusCode[0])) {
      throw new ServerException(respStatusCode[0].textMessage);
    }

    return respPayload[0];
  }

} 
