/**
 * JidlProtocolConnectionManager.java
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

package com.github.ilguido.jidl.connectionmanager;

import java.io.IOException;
import java.util.ArrayList;
import javax.management.AttributeNotFoundException;
import javax.net.ssl.SSLContext;

import com.github.ilguido.jidl.DataTypes;
import com.github.ilguido.jidl.jidlclient.JPJidlClient;
import com.github.ilguido.jidl.variable.Variable;
import com.github.ilguido.jidl.variable.VariableReader;
import com.github.ilguido.jidl.variable.json.JsonVariableReader;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * JidlProtocolConnectionManager
 * Class to manage a connection to a JIDL Protocol service.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class JidlProtocolConnectionManager extends ConnectionManager 
                                           implements DataTypes {
  /**
   * The {@link JPJidlClient} client that actually reads data over a JIDL
   * Protocol connection.
   */
  private JPJidlClient client;
  
  /**
   * The contents of the request.
   */
  private JsonObject payload;
  
  /**
   * The socket port.
   */
  private final Integer port;
  
  /**
   * Class constructor.  It sets the name of the connection and initializes the
   * lists of variables.
   *
   * @param inName the mnemonic name of the connection
   * @param inAddress the address of the connection
   * @param inPort the port of the remote socket
   * @param inSSLContext an <code>SSLContext</code>, which is used to generate
   *                     the socket
   * @param inDeciseconds the sample time
   * @throws IllegalArgumentException if <code>inName</code> is not a valid name
   *                                  or <code>inAddress</code> is malformed
   */
  public JidlProtocolConnectionManager(String inName,
                                       String inAddress,
                                       int inPort,
                                       SSLContext inSSLContext,
                                       int inDeciseconds)
    throws IllegalArgumentException {
    super(inName, "jidlprotocol", inAddress);
    
    setSampleTime(inDeciseconds);
    /* Set the timeout, in milliseconds, to the sample time, in deciseconds. */
    client = new JPJidlClient(inAddress, inPort, inDeciseconds * 100,
                              null, inSSLContext);
    
    this.port = inPort;
    this.payload = null;
  }

  /**
   * Adds a new {@link VariableReader} object to the connection. Each 
   * connection is a link to a number of variables of the remote device, stored
   * in a list.
   *
   * @param inName the mnemonic name of the variable
   * @param inAddress the address of the variable, its format depends on the
   *                  connection
   * @param inType the type of the variable as defined in {@link DataTypes}
   * @throws IllegalArgumentException if the <code>inType</code> is an invalid
   *                                  type
   * @return the created {@link VariableReader} object
   */
  @Override
  public VariableReader addVariableReader(String inName, 
                                          String inAddress,
                                          DataType inType)
    throws IllegalArgumentException {
    VariableReader v;

    for (final VariableReader vr : variableReaderList) {
      if (inName.equals(vr.getName()))
        throw new IllegalArgumentException("Duplicate variable name: " + 
                                           inName);
    }

    /* same variables as JSON */
    switch (inType) {
      case BOOLEAN:
      case INTEGER:
      case WORD:
      case BYTE:
      case TEXT:
      case DOUBLE_INTEGER:
      case FLOAT:
      case REAL:
      case DOUBLE_WORD:
        v = new JsonVariableReader(inName, inAddress, inType);
        break;
      default:
        throw new IllegalArgumentException("Illegal JSON variable type:" + 
                                           inType.name());
    }

    variableReaderList.add(v);
    
    return v;
  }
  
  /**
   * Should establish a connection to the remote device, but since JIDL 
   * Protocol is stateless, this does nothing.
   *
   */
  @Override
  public void connect() throws IOException {
    status = true;
  }
  
  /**
   * Should close the connection to the industrial device, but JIDL Protocol is
   * stateless.
   *
   */
  @Override
  public void disconnect() {
    status = false;
  }
  
  /**
   * Returns the object used for connecting to the industrial device.
   *
   * @return an object or null if there is none
   */
  @Override
  public Object getClient() {
    return (Object) client;
  }
    
  /**
   * Returns the requested parameter as a generic object.
   *
   * @param inParName the name of the requested parameter
   * @return the value of the requested parameter as a generic object
   * @throws IllegalArgumentException when the argument is null
   * @throws AttributeNotFoundException if the parameter is not part of this
   *                                    class
   */
  @Override
  public Object getParameterByName(String inParName) 
    throws AttributeNotFoundException,
           IllegalArgumentException  {
    try {
      return super.getParameterByName(inParName);
    } catch (AttributeNotFoundException anfe) {
      // If the parameter is not part of the super class,
      // may be part of this subclass
      if (inParName.equals("url")) {
        return super.getAddress();
      } else if (inParName.equals("port")) {
        return this.getPort();
      } 
    }
    
    // If attribute name has not been recognized
    throw new AttributeNotFoundException("Unknown parameter name: "+ inParName);
  }
  
  /**
   * Returns the string array of the names of the parameters.
   *
   * @return string array of parameter names
   */
  @Override
  public String[] getParameterNames() {
    return new String[]{"name", "sample time", "type", "url", "port"};
  }
  
  /**
   * Returns the port number as an <code>Integer</code>.
   */
  public Integer getPort() {
    return this.port;
  }
  
  /**
   * Initializes the connection.
   *
   * @throws IllegalArgumentException if something goes wrong
   */
  @Override
  public void initialize() throws IllegalArgumentException {
    /* Update the payload of the request at initialization time. */
    this.payload = new JsonObject();
    
    for (final VariableReader vr : variableReaderList) {
      JsonArray array;
      String[] nameArray = splitQualifier(vr.getAddress());
      if (this.payload.containsKey(nameArray[1])) {
        array = (JsonArray) this.payload.get(nameArray[1]);
      } else {
        array = new JsonArray();
      }
      array.add(nameArray[0]);
      payload.put(nameArray[1], array);
    }
    
    try {
      client.initialize();
    } catch (Exception e) {
      client = null;
    }
  }
    
  /**
   * Returns <code>true</code> if the connection is correctly initialized.
   *
   * @return <code>true</code> if the member <code>client</code> is initialized,
   *         <code>false</code> otherwise
   */
  @Override
  public boolean isInitialized() {
    if (client == null)
      return false;

    return client.isInitialized();
  }
  
  /**
   * Reads all the variables listed in this connection and returns itself.
   *
   * @return this {@link ConnectionManager} object
   */
  @Override
  public ConnectionManager read() {
    // this updates the timestamp
    updateTimestamp();
    
    try {
      client.read("values", payload);
    } catch (IllegalArgumentException iae) {
      //FIXME: do something?
    } catch (IOException ioe) {
      status = false;
    }
    
    try {
      for (final VariableReader vr : variableReaderList) {
        vr.read((Object) client);
      }
    } catch (IOException ioe) {
      //FIXME: do something?
    } catch (Exception e) {
      //FIXME: do something?
    }
    
    return this;
  }
}
