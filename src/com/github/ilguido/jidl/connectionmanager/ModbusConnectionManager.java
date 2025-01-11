/**
 * ModbusConnectionManager.java
 *
 * Copyright (c) 2022 Stefano Guidoni
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
import javax.management.AttributeNotFoundException;

import com.github.ilguido.jidl.variable.VariableReader;
import com.github.ilguido.jidl.variable.VariableWriter;
import com.github.ilguido.jidl.variable.modbus.ModbusVariableReader;
import com.github.ilguido.jidl.variable.modbus.ModbusVariableWriter;
import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.PlcConnection;

/**
 * ModbusConnectionManager
 * Class to manage a connection to a Modbus TCP capable device.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class ModbusConnectionManager extends PLCConnectionManager {
  /**
   * The IP address of the Modbus device.
   */
  private final String ipAddress;

  /**
   * The port number of the TCP/IP connection.  It is usually 522.
   */
  private final int port;
  
  /**
   * Order of the registers.  This affects the reading of double words,
   * switching the word order.
   */
  private final boolean reversed;
  
  /**
   * Class constructor.  It calls the parent class constructor and then create
   * the client object.
   *
   * @param inName the mnemonic name of the connection
   * @param inIP the IP address of the device
   * @param inPort the port number of the TCP/IP connection
   * @param inOrder the order of the words in double words
   * @param inDeciseconds the sample time
   * @throws IllegalArgumentException if some input parameter is not valid
   */
  public ModbusConnectionManager(String inName, 
                                 String inIP, 
                                 int inPort, 
                                 boolean inOrder,
                                 int inDeciseconds)
    throws IllegalArgumentException {
    super(inName, "modbus-tcp", "modbus-tcp:tcp://" + inIP + ":" +
                            Integer.toString(inPort));

    ipAddress = inIP;
    port = inPort;
    reversed = inOrder;

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
      throw new IllegalArgumentException("Invalid Modbus TCP address: " +
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
   * Returns the order of registers when reading multi-register variables.
   *
   * @return a <code>true</code> value if the order is reversed
   */
  public Boolean isReversed() {
    return Boolean.valueOf(reversed);
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
      } else if (inParName.equals("order")) {
        return isReversed();
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
    return new String[]{"name", "sample time", "type", "ip address", "port",
                        "order"};
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
   * @throws IllegalArgumentException if the <code>inType</code> is an invalid
   *                                  type or if <code>inName</code> is already
   *                                  present
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
      case WORD:
      case FLOAT:
      case TEXT:
        v = new ModbusVariableReader(inName, inAddress, inType, client, false);
        break;
      case DOUBLE_INTEGER:
      case REAL:
      case DOUBLE_WORD:
        v = new ModbusVariableReader(inName, inAddress, inType, client, reversed);
        break;
      default:
        throw new IllegalArgumentException("Illegal Modbus variable type:" + 
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
   * @throws IllegalArgumentException if the <code>inType</code> is an invalid
   *                                  type or if <code>inName</code> is already
   *                                  present
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
      case WORD:
      case FLOAT:
      case TEXT:
        v = new ModbusVariableWriter(inName, inAddress, inType, inSource, 
                                     client, false);
        break;
      case DOUBLE_INTEGER:
      case REAL:
      case DOUBLE_WORD:
        v = new ModbusVariableWriter(inName, inAddress, inType, inSource, 
                                     client, reversed);
        break;
      default:
        throw new IllegalArgumentException("Illegal Modbus variable type:" + 
                                           inType.name());
    }

    variableWriterList.add(v);
    return v;
  }
}
