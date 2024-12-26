/**
 * OPCUAConnectionManager.java
 *
 * Copyright (c) 2023 Stefano Guidoni
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
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.management.AttributeNotFoundException;

import com.github.ilguido.jidl.utils.Decrypter;
import com.github.ilguido.jidl.variable.VariableReader;
import com.github.ilguido.jidl.variable.VariableWriter;
import com.github.ilguido.jidl.variable.opcua.OPCUAVariableReader;
import com.github.ilguido.jidl.variable.opcua.OPCUAVariableWriter;
import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.PlcConnection;

/**
 * OPCUAConnectionManager
 * Class to manage a connection to a OPC-UA capable device.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class OPCUAConnectionManager extends PLCConnectionManager {
  /**
   * The discovery endpoint option.
   */
  private final Boolean discovery;

  /**
   * The ip address of the OPC-UA device.
   */
  private final String ipAddress;

  /**
   * The password for the user authentication of the OPCUA connection.
   */
  private final String password;
  
  /**
   * The path to the endpoint of the OPCUA server.
   */
  private final String path;
  
  /**
   * The port number of the TCP/IP connection.
   */
  private final int port;
  
  /**
   * The name for the user authentication of the OPCUA connection.
   */
  private final String username;
  
  /**
   * Class constructor.  It calls the parent class constructor and then create
   * the client object. If a user name and a password are provided, those are
   * used for the login. If a salt and initialization vector are provided too,
   * then the user name and the password are expected to be Base64 encoded,
   * AES encrypted strings.
   *
   * @param inName the mnemonic name of the connection
   * @param inIP the IP address of the device
   * @param inPort the port number of the TCP/IP connection
   * @param inPath the optional path to the server endpoint
   * @param inDiscovery a boolean value to request the discovery endpoint
   * @param inUsername a string value for the user name
   * @param inPassword a string value for the user password
   * @param inSalt a Base64 encoded string value for the cryptographic salt
   * @param inIV a Base64 encoded string value for the initialization vector
   * @param inDeciseconds the sample time
   * @throws IllegalArgumentException if some input parameter is not valid
   * @throws ExecutionException if decrypting user credentials failed
   */
  public OPCUAConnectionManager(String inName, 
                                String inIP, 
                                int inPort,
                                String inPath,
                                Boolean inDiscovery,
                                String inUsername,
                                String inPassword,
                                String inSalt,
                                String inIV,
                                int inDeciseconds)
    throws IllegalArgumentException, ExecutionException {
    super(inName, "opcua", "opcua:tcp://" + inIP + ":" + 
                           Integer.toString(inPort) +
                           "/" + inPath + "?discovery=" + 
                           inDiscovery.toString() +
                           ((inUsername != null && inPassword != null) ? 
                           "&username=" + 
                           Decrypter.decrypt(inUsername, inSalt, inIV) + 
                           "&password=" + 
                           Decrypter.decrypt(inPassword, inSalt, inIV) :
                           ""));

    ipAddress = inIP;
    port = inPort;
    path = inPath;
    discovery = inDiscovery;
    username = inUsername;
    password = inPassword;

    setSampleTime(inDeciseconds);
    
    // get a client
    try {
      initialize();
    } catch (Exception e) {
      // FIXME!
    }
  }

  /**
   * Initializes the connection and a <code>PlcConnection</code> object.
   * The <code>PlcConnection</code> object from the PLC4J library handles the 
   * connection with the remote device.
   *
   * @throws IllegalArgumentException if something goes wrong
   */
  public void initialize() 
    throws IllegalArgumentException {
    try {
      client = new PlcDriverManager().getConnection(getAddress());
    } catch (Exception e) {
      client = null;
      throw new IllegalArgumentException("Invalid OPC UA address: " +
                                         getAddress(), e);
    }
    
    disconnect(); //PLC4J always connect when creating a new client
  }
  
  /**
   * Returns the IP address of the connected device.
   *
   * @return the IP address of the industrial device
   */
  public String getIP() {
    return ipAddress;
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
      if (inParName.equals("ip address")) {
        return getIP();
      } else if (inParName.equals("port")) {
        return getPort();
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
    return new String[]{"name", "sample time", "type", "ip address", "port"};
  }

  /**
   * Returns the port number of the connection.
   *
   * @return the port number of the TCP/IP connection
   */
  public Integer getPort() {
    return Integer.valueOf(port);
  }

  /**
   * Adds a new {@link com.github.ilguido.jidl.variable.VariableReader} object to the
   * Modbus connection. Each connection is a link to a number of variables of 
   * the remote device, stored in a list.
   *
   * @param inName the mnemonic name of the variable
   * @param inAddress the Modbus address of the variable
   * @param inType the type of the variable as defined in {@link com.github.ilguido.jidl.DataTypes}
   * @throws IllegalArgumentException if a variable with name
   *                                  <code>inName</code> is already present
   * @return the created {@link com.github.ilguido.jidl.variable.VariableReader} object
   */
  @Override
  public VariableReader addVariableReader(String inName,
                                          String inAddress,
                                          DataType inType)
    throws IllegalArgumentException {
    VariableReader v;

    for (final VariableReader var : variableReaderList) {
      if (inName.equals(var.getName()))
        throw new IllegalArgumentException("Duplicate variable name: " + 
                                           inName);
    }
    
    switch (inType) {
      case BOOLEAN:
      case INTEGER:
      case DOUBLE_INTEGER:
      case FLOAT:
      case REAL:
      case BYTE:
      case WORD:
      case DOUBLE_WORD:
      case TEXT:
        v = new OPCUAVariableReader(inName, inAddress, inType, client);
        break;
      default:
        throw new IllegalArgumentException("Illegal OPC UA variable type:" + 
                                           inType.name());
    }

    variableReaderList.add(v);
    return v;
  }
  
  /**
   * Adds a new {@link com.github.ilguido.jidl.variable.VariableWriter} object to the
   * Modbus connection. Each connection is a link to a number of variables of 
   * the remote device, stored in a list.
   *
   * @param inName the mnemonic name of the variable
   * @param inAddress the Modbus address of the variable
   * @param inType the type of the variable as defined in {@link com.github.ilguido.jidl.DataTypes}
   * @param inSource the source variable
   * @throws IllegalArgumentException if a variable with name
   *                                  <code>inName</code> is already present
   * @return the created {@link com.github.ilguido.jidl.variable.VariableWriter} object
   */
  @Override
  public VariableWriter addVariableWriter(String inName,
                                          String inAddress,
                                          DataType inType,
                                          VariableReader inSource)
    throws IllegalArgumentException {
    VariableWriter v;

    for (final VariableWriter var : variableWriterList) {
      if (inName.equals(var.getName()))
        throw new IllegalArgumentException("Duplicate variable name: " + 
                                           inName);
    }
    
    switch (inType) {
      case BOOLEAN:
      case INTEGER:
      case DOUBLE_INTEGER:
      case FLOAT:
      case REAL:
      case BYTE:
      case WORD:
      case DOUBLE_WORD:
      case TEXT:
        v = new OPCUAVariableWriter(inName, inAddress, inType, inSource,
                                    client);
        break;
      default:
        throw new IllegalArgumentException("Illegal OPC UA variable type:" + 
                                           inType.name());
    }

    variableWriterList.add(v);
    return v;
  }
}
