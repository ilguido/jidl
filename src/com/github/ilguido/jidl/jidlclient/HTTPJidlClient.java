/**
 * HTTPJidlClient.java
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

package com.github.ilguido.jidl.jidlclient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * HTTPJidlClient
 * A custom client for reading from HTTP connections.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class HTTPJidlClient {
  /**
   * The HTTP url of the remote resource as a string.
   */
  private final String urlS;
   
  /**
   * The HTTP url of the remote resource.
   */
  private URL url = null;
  
  /**
   * The connection to the HTTP url.
   */
  private URLConnection connection = null;
  
  /**
   * A boolean value to remember whether the client is connected.
   */
  private boolean connected = false;
  
  /**
   * The data read from the url.
   */
  private String readData;

  /**
   * Class constructor.  It initializes the <code>URLConnection</code> and 
   * tests it.
   *
   * @param inURL the url of the remote resource as a <code>String</code>
   * @throws IllegalArgumentException if the URL is malformed
   */
  public HTTPJidlClient (String inURL) throws IllegalArgumentException {
    urlS = inURL;
    try {
      initialize();
    } catch (MalformedURLException murle) {
      throw new IllegalArgumentException("Malformed URL.");
    } catch (IOException e) {
      //FIXME: do something?
    }
  }
  
  /**
   * Connects to the remote HTTP server.
   *
   * @throws IOException if the remote server is unavailable
   */
  public void connect() throws IOException {
    if (!isInitialized()) {
      throw new IOException("HTTP connection unavailable!");
    }
  
    try {
      connection.connect();
      connected = true;
    } catch (IOException e) {
      connected = false;
      throw e;
    }
  }
  
  /**
   * Disconnects the client.  It sets the <code>URL</code> object to 
   * <code>null</code>, forcing its re-initialization at the next connect.
   */
  public void disconnect() {
    url = null;
    connection = null;
    connected = false;
  }
  
  /**
   * Returns the data from the last reading as a <code>String</code>.
   *
   * @return the data read from the remote resource
   */
  public String getDataAsString() {
    return readData;
  }
  
  /**
   * Returns the data from the last reading as a <code>JsonObject</code>.  It
   * tries to parse the data as JSON data and returns the parsed JSON object.
   *
   * @return the data read from the remote resource
   * @throws JsonException if the parsing of the data failed
   */
  public JsonObject getDataAsJsonObject() throws JsonException {
    return Jsoner.deserialize(readData, new JsonObject());
  }
  
  /**
   * Opens the HTTP connection.
   *
   * @throws IOException if the connection fails
   */
  public void initialize() throws IOException {
    if (url == null) {
      url = new URL(urlS);
    }
    connection = url.openConnection();
  }
  
  /**
   * Returns the status of the connection.
   *
   * @return <code>true</code> if connected
   */
  public boolean isConnected() {
    return connected;
  }
  
  /**
   * Returns the status of the <code>URLConnection</code> object.
   *
   * @return <code>true</code> if initialized
   */
  public boolean isInitialized() {
    return connection != null;
  }
  
  /**
   * Reads the data from the remote HTTP server.
   *
   * @return the read data
   */
  public String read() {
    readData = "";
    
    try {
      String read;
      BufferedReader bf = new BufferedReader(
                            new InputStreamReader(
                              connection.getInputStream()));
      while ((read = bf.readLine()) != null) {
        readData += read;
      }
      bf.close();
      initialize(); // re-initialize the connection
    } catch (IOException ioe) {
      connected = false;
    } catch (Exception e) {
      //
    }
    
    return readData;
  }
}
