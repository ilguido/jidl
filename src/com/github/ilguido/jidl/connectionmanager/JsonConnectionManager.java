/**
 * JsonConnectionManager.java
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

package com.github.ilguido.jidl.connectionmanager;

import java.io.IOException;
import java.util.ArrayList;
import javax.management.AttributeNotFoundException;

import com.github.ilguido.jidl.DataTypes;
import com.github.ilguido.jidl.jidlclient.HTTPJidlClient;
import com.github.ilguido.jidl.variable.Variable;
import com.github.ilguido.jidl.variable.VariableReader;
import com.github.ilguido.jidl.variable.json.JsonVariableReader;

/**
 * JsonConnectionManager
 * Superclass to manage a connection to an industrial device.  Each connection
 * allows polling an industrial device and reading a number of variables from
 * that device.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class JsonConnectionManager extends ConnectionManager 
                                   implements DataTypes {
  /**
   * The {@link com.github.ilguido.jidl.jidlclient.HTTPJidlClient} client that actually reads the 
   * data over a HTTP connection.
   */
  private HTTPJidlClient client;
  
  /**
   * Class constructor.  It sets the name of the connection and initializes the
   * lists of variables.
   *
   * @param inName the mnemonic name of the connection
   * @param inAddress the address of the connection
   * @param inSeconds the sample time
   * @throws IllegalArgumentException if <code>inName</code> is not a valid name
   *                                  or <code>inAddress</code> is malformed
   */
  public JsonConnectionManager(String inName, 
                               String inAddress,
                               int inSeconds)
    throws IllegalArgumentException {
    super(inName, "json", inAddress);
    
    setSampleTime(inSeconds);
    client = new HTTPJidlClient(inAddress);
  }

  /**
   * Adds a new {@link com.github.ilguido.jidl.variable.VariableReader} object to the
   * connection. Each connection is a link to a number of variables of the
   * remote device, stored in a list.
   *
   * @param inName the mnemonic name of the variable
   * @param inAddress the address of the variable, its format depends on the
   *                  connection
   * @param inType the type of the variable as defined in {@link com.github.ilguido.jidl.DataTypes}
   * @throws IllegalArgumentException if the <code>inType</code> is an invalid
   *                                  type
   * @return the created {@link com.github.ilguido.jidl.variable.VariableReader} object
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

    switch (inType) {
      case BOOLEAN:
      case INTEGER:
      case WORD:
      case BYTE:
      case TEXT:
      case DOUBLE_INTEGER:
      case REAL:
        v = new JsonVariableReader(inName, inAddress, inType);
        break;
      default:
        throw new IllegalArgumentException("Illegal Json variable type:" + 
                                           inType.name());
    }

    variableReaderList.add(v);
    return v;
  }
  
  /**
   * Establishes a connection to the industrial device.
   *
   * @throws IOException if the connection cannot be established
   */
  @Override
  public void connect() throws IOException {
    status = true;
    try {
      if (!getStatus())
        client.connect();
    } catch (Exception e) {
      status = false;
      throw new IOException("Cannot connect: " + getName(), e);
    }
  }
  
  /**
   * Closes the connection to the industrial device.
   */
  @Override
  public void disconnect() {
    //Nothing to do?
    client.disconnect();
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
    return new String[]{"name", "sample time", "type", "url"};
  }
  
  /**
   * Initializes the connection.
   *
   * @throws IllegalArgumentException if something goes wrong
   */
  @Override
  public void initialize() throws IllegalArgumentException {
    try {
      client.initialize();
    } catch (Exception e) {
      //
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
    return client.isInitialized();
  }
  
  /**
   * Reads all the variables listed in this connection and returns itself.
   *
   * @return this {@link com.github.ilguido.jidl.connectionmanager.ConnectionManager} object
   */
  @Override
  public ConnectionManager read() {
    // this updates the timestamp
    updateTimestamp();
    client.read();
    
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
