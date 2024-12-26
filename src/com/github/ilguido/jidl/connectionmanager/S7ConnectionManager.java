/**
 * S7ConnectionManager.java
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
import javax.management.AttributeNotFoundException;

import com.github.ilguido.jidl.variable.VariableReader;
import com.github.ilguido.jidl.variable.VariableWriter;
import com.github.ilguido.jidl.variable.s7.S7VariableReader;
import com.github.ilguido.jidl.variable.s7.S7VariableWriter;
import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.PlcConnection;

/**
 * S7ConnectionManager
 * Class to manage a connection to an S7 device.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class S7ConnectionManager extends PLCConnectionManager {
  /**
   * The IP address of the S7 device.
   */
  private final String ipAddress;

  /**
   * The rack number of the S7 device.  It is usually 0, unless the device
   * is a S7 400 CPU.
   */
  private final int rack;

  /**
   * The slot number of the S7 device.  It must be 2 for S7 300 CPU, 0 for
   * S7 1200/1500, a number between 1 and 31 otherwise.
   */
  private final int slot;

  /**
   * Class constructor.  It calls the parent class constructor and then create 
   * the client object.
   *
   * @param inName the mnemonic name of the variable
   * @param inIP the IP address of the device
   * @param inRack the rack number of the S7 device
   * @param inSlot the slot number of the S7 device
   * @param inDeciseconds the sample time
   * @throws IllegalArgumentException if some input parameter is not valid
   */
  public S7ConnectionManager(String inName,
                             String inIP,
                             int inRack,
                             int inSlot,
                             int inDeciseconds)
    throws IllegalArgumentException {
    super(inName, "S7", "s7://" + inIP + "?remote-rack=" + inRack +
                        "&remote-slot=" + inSlot);

    ipAddress = inIP;
    rack = inRack;
    slot = inSlot;

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
      throw new IllegalArgumentException("Invalid S7 TCP address: " +
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
      } else if (inParName.equals("rack")) {
        return getRack();
      } else if (inParName.equals("slot")) {
        return getSlot();
      }
    }
    
    // If attribute_name has not been recognized
    throw new AttributeNotFoundException("Unknown parameter name: "+ inParName);
  }
  
  /**
   * Returns the string array of the names of the parameters.
   *
   * @return string array of parameter names
   */
  @Override
  public String[] getParameterNames() {
    return new String[]{"name", "sample time", "type", "ip address", "rack",
                        "slot"};
  }

  /**
   * Returns the rack number of the connection.
   *
   * @return the rack number of the industrial device
   */
  public Integer getRack() {
    return Integer.valueOf(rack);
  }

  /**
   * Returns the slot number of the connection.
   *
   * @return the slot number of the industrial device
   */
  public Integer getSlot() {
    return Integer.valueOf(slot);
  }

  /**
   * Adds a new {@link com.github.ilguido.jidl.variable.VariableReader} object to the
   * S7 connection. Each connection is a link to a number of variables of the
   * remote device, stored in a list.
   *
   * @param inName the mnemonic name of the variable
   * @param inAddress the S7 address of the variable
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
      case DOUBLE_INTEGER:
      case INTEGER:
      case REAL:
      case DOUBLE_WORD:
      case WORD:
      case BYTE:
      case TEXT:
        v = new S7VariableReader(inName, inAddress, inType, client);
        break;
      default:
        throw new IllegalArgumentException("Illegal S7 variable type:" + 
                                           inType.name());
    }

    variableReaderList.add(v);
    return v;
  }
  
  /**
   * Adds a new {@link com.github.ilguido.jidl.variable.VariableWriter} object to the
   * S7 connection. Each connection is a link to a number of variables of the
   * remote device, stored in a list.
   *
   * @param inName the mnemonic name of the variable
   * @param inAddress the S7 address of the variable
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
      case DOUBLE_INTEGER:
      case INTEGER:
      case REAL:
      case DOUBLE_WORD:
      case WORD:
      case BYTE:
      case TEXT:
        v = new S7VariableWriter(inName, inAddress, inType, inSource, client);
        break;
      default:
        throw new IllegalArgumentException("Illegal S7 variable type:" + 
                                           inType.name());
    }

    variableWriterList.add(v);
    return v;
  }
}
