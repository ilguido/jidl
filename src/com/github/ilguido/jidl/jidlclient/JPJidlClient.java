/**
 * JPJidlClient.java
 *
 * Copyright (c) 2025 Stefano Guidoni
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

package com.github.ilguido.jidl.jidlclient;

import java.io.IOException;
import java.rmi.ServerException;
import java.util.concurrent.ExecutorService;
import javax.net.ssl.SSLContext;

import com.github.ilguido.jidl.ipc.JidlProtocolClient;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * JPJidlClient
 * A client for reading data from a Jidl Protocol connection.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class JPJidlClient {
  /**
   * The server address.
   */
  private final String address;
    
  /**
   * The <code>ExecutorService</code> of the client thread.
   */
  private final ExecutorService executorService;
  
  /**
   * The server port.
   */
  private final int port;
  
  /**
   * The <code>SSLContext</code>, which is used to generate the socket.
   */
  private final SSLContext sslContext;
  
  /**
   * The connection timeout in milliseconds.
   */
  private final int timeOut;  
  
  /**
   * The actual client as a {@link JidlProtocolClient} object.
   */
  private JidlProtocolClient client;
   
  /**
   * The read data as a <code>JsonObject</code>.
   */
  private JsonObject data;
  
  /**
   * Class constructor.  
   *
   * @param inAddress the address of the remote resource as a 
   *                  <code>String</code>
   * @param inPort the server port
   * @param inTimeOut the timeout of the connection in milliseconds
   * @param inExecutorService an <code>ExecutorService</code> for the client
   *                          thread
   * @param inSSLContext an <code>SSLContext</code>, which is used to generate
   *                     the socket
   */
  public JPJidlClient (String inAddress,
                       int inPort,
                       int inTimeOut,
                       ExecutorService inExecutorService,
                       SSLContext inSSLContext) {
    this.address = inAddress;
    this.port = inPort;
    this.timeOut = inTimeOut;
    this.executorService = inExecutorService;
    this.sslContext = inSSLContext;
    
    // test connection?
    this.initialize();
  }
  
  /**
   * Returns the data from the last reading as a <code>JsonObject</code>.
   *
   * @return the data read from the remote resource
   */
  public JsonObject getDataAsJsonObject() {
    return data;
  }
  
  /**
   * Returns the data from the last reading as a <code>String</code>.
   *
   * @return the data read from the remote resource
   */
  public String getDataAsString() {
    return Jsoner.serialize(data);
  }
  
  /**
   * Reinitializes the client.
   *
   */
  public void initialize() {
    this.client = new JidlProtocolClient(this.address,
                                         this.port,
                                         this.timeOut,
                                         this.executorService,
                                         this.sslContext);
  }
  
  /**
   * Returns the status of the {@link JidlProtocolClient} object.
   *
   * @return <code>true</code> if initialized
   */
  public boolean isInitialized() {
    return client != null;
  }
  
  /**
   * Reads the data from the remote HTTP server and returns this object.
   *
   * @param inMethod the request method
   * @param inPayload the contents of the request
   * @throws IllegalArgumentException if either the passed method or contents of
   *                                  the request are malformed
   * @throws IOException if the connection to the remote server failed
   */
  public JPJidlClient read(String inMethod,
                           String inPayload) 
    throws IllegalArgumentException, IOException {
    JsonObject read;
    
    try {
      read = client.request(inMethod, 
                            Jsoner.deserialize(inPayload, new JsonObject()));
    } catch (ServerException se) {
      throw new IllegalArgumentException(se.getMessage());
    } catch (Exception e) {
      throw new IOException();
    }
    
    return this;
  }
}
