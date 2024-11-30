/**
 * PLCConnectionManager.java
 *
 * Copyright (c) 2021 Stefano Guidoni
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

import com.github.ilguido.jidl.DataTypes;
import com.github.ilguido.jidl.variable.VariableReader;
import com.github.ilguido.jidl.variable.VariableWriter;
import org.apache.plc4x.java.api.PlcConnection;

/**
 * PLCConnectionManager
 * Superclass to manage a connection to an industrial device.  Each connection
 * allows polling an industrial device and reading a number of variables from
 * that device. This is a common set of methods for PLC devices.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public abstract class PLCConnectionManager extends ConnectionManager 
                                           implements DataTypes,
                                                      ShareableConnection,
                                                      WriteableConnection {
  /**
   * The <code>PlcConnection</code> object managing the connection.
   */
  protected PlcConnection client = null;
  
  /**
   * Class constructor.  It sets the name of the connection and initializes the
   * lists of variables.
   *
   * @param inName the mnemonic name of the connection
   * @param inType the mnemonic name of the type of the connection
   * @param inAddress the address of the connection
   * @throws IllegalArgumentException if <code>inName</code> is not a valid name
   */
  public PLCConnectionManager(String inName, String inType, String inAddress)
    throws IllegalArgumentException {
    super(inName, inType, inAddress);
  }

  /**
   * Establishes a connection to the industrial device.
   *
   * @throws IOException if the connection cannot be established
   */
  @Override
  public void connect()
    throws IOException {
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
    try {
      if (getStatus())
        client.close();
    } catch (Exception e) {
      //TODO: add some error output message
    } finally {
      status = false;
    }
  }

  /**
   * Returns the <code>PlcConnection</code> object managing the connection.
   *
   * @return a <code>PlcConnection</code> object
   */
  @Override
  public Object getClient() {
    return (Object) client;
  }
  
  /**
   * Returns the status of the connection.
   *
   * @return <code>true</code> if the connection is established,
   *         <code>false</code> otherwise
   */
  @Override
  public boolean getStatus() {
    if (this.isInitialized()) {
      status = client.isConnected();
    } else {
      status = false;
    }
    
    return super.getStatus();
  }
  
  /**
   * Returns <code>true</code> if the connection is correctly initialized.
   *
   * @return <code>true</code> if the member <code>client</code> is initialized,
   *         <code>false</code> otherwise
   */
  @Override
  public boolean isInitialized() {
    return client != null;
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

    for (final VariableReader v : variableReaderList) {
      try {
        v.read(client);
      } catch (IOException e) {
        /* an error with the connection occurred,
         * disconnect and retry to connect */
        disconnect();
        break; // exit the for cycle
      } catch (Exception e) {
        //TODO: set the variable to some default error value,
        //      better call a vr.setDefaultValue() or a
        //      vr.setErrorValue()
      }
    }

    return this;
  }
    
  /**
   * Set the <code>PlcConnection</code> object to be used for connecting to the 
   * industrial device.
   *
   * @param inClient the <code>PlcConnection</code> object to be used 
   * @throws IllegalArgumentException if the object is not a valid 
   *                                  <code>PlcConnection</code> object
   */
  @Override
  public void setClient(Object inClient) 
    throws IllegalArgumentException {
    if (inClient instanceof PlcConnection) {
      client = (PlcConnection) inClient;
    } else {
      throw new IllegalArgumentException("Client is not PlcConnection object");
    }
  }
    
  /**
   * Writes all the variables listed in this connection and returns itself.
   *
   * @return this {@link com.github.ilguido.jidl.connectionmanager.ConnectionManager} object
   */
  @Override
  public ConnectionManager write() {

    for (final VariableWriter v : variableWriterList) {
      try {
        v.write(client);
      } catch (IOException e) {
        /* an error with the connection occurred,
         * disconnect and retry to connect */
        disconnect();
        break; // exit the for cycle
      } catch (Exception e) {
        //FIXME:stub!
      }
    }

    return this;
  }
}
