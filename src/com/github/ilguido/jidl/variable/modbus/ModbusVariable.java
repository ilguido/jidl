/**
 * ModbusVariable.java
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

package com.github.ilguido.jidl.variable.modbus;

import com.github.ilguido.jidl.variable.VariableCommon;
import org.apache.plc4x.java.api.PlcConnection;

/**
 * ModbusVariable
 * Superclass for managing a variable from an industrial device
 * through a Modbus TCP connection.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public abstract class ModbusVariable extends VariableCommon {
  /**
   * PLC4J appends the size of the array to the name of the variable.
   */
  private final String addressSuffix;
  
  /**
   * The size as number of registers of the variable.
   */
  private int tagSize;
  
  /**
   * The order of the registers when converting multiregister variables.
   */
  private final boolean reversed;

  /**
   * Class constructor.  It calls the parent class constructor and then sets the
   * client property.
   *
   * @param inName the mnemonic name of the variable
   * @param inAddress the address of the variable, its format depends on the
   *                  connection
   * @param inType the type of the variable as defined in {@link com.github.ilguido.jidl.DataTypes}
   * @param inClient a <code>PlcConnection</code> object from the PLC4J library
   * @param inOrder the order of the words in double words
   * @throws IllegalArgumentException if address is not a valid address
   */
  public ModbusVariable(String inName, 
                        String inAddress,
                        DataType inType,
                        PlcConnection inClient,
                        boolean inOrder)
    throws IllegalArgumentException {
    super(inName, inAddress, inType);
    
    reversed = inOrder;
    
    /**
     * That part of the address where PLC4J indicates the size of the array.
     * E.g. <code>"[10]"</code> for an array of size <code>10</code>, or an
     * empty string when just one register or coil is requested.
     */
    switch (inType) {
      case DOUBLE_INTEGER:
      case REAL:
      case DOUBLE_WORD:
        addressSuffix = "[2]";
        break;
      case TEXT:
        addressSuffix = "[" + String.valueOf(getTagSize()) + "]";
        break;
      default:
        addressSuffix = "";
        break;
    }
  }

  /**
   * Returns that part of the address where PLC4J indicates the size of the
   * array.
   *
   * @return the suffix of the address according to PLC4J
   */
  public String getAddressSuffix() {
    return addressSuffix;
  }
  
  /**
   * Returns the size as the number of registers of the variable.
   *
   * @return the number of registers of the variable
   */
  public int getTagSize() {
    return tagSize;
  }
  
  /**
   * Returns whether the register order is reversed.  In a Modbus connection the
   * order of registers for variables spanning multiple registers can be 
   * ascending or descending.
   *
   * @return the order of the registers for this connection
   */
  public boolean isReversed() {
    return reversed;
  }
  
  /**
  * Checks that a given string is a valid Modbus address.  If address is not a
  * valid address, this function throws an exception. If it is valid, it
  * populates the addressParameters property.
  *
  * @param inAddress the address to be validated
  * @throws IllegalArgumentException if address is not a valid Modbus address
  */
  @Override
  protected void validateAddress(String inAddress)
    throws IllegalArgumentException {
    int size = 0; // size in bits of the addressed area
    //int type = 0; // the Modbus object type
    int length = 0; // the number of registers or coil to read

    switch (inAddress.charAt(0)) {
      /* coil */
      case '0':
      case '1':
        size = 1;
        break;
      /* register */
      case '3':
      case '4':
        size = 16;
        break;
      default:
        throw new IllegalArgumentException("Illegal Modbus address:" +
                                           inAddress);
    }

    //type = (inAddress.charAt(0) - '0');

    /* compare the size of the addressed area with
     * the size of the requested type: they must match */
    switch (this.getType()) {
      case BOOLEAN:
        if (size != 1)
          throw new IllegalArgumentException("Illegal Modbus address for bit " +
                                             "variable: " + inAddress);
        length = 1;
        break;
      case INTEGER:
      case FLOAT:
      case WORD:
        if (size == 1)
          throw new IllegalArgumentException("Illegal Modbus address for word" +
                                             " variable: " + inAddress);
        length = 1;
        break;
      case DOUBLE_INTEGER:
      case REAL:
      case DOUBLE_WORD:
        if (size == 1)
          throw new IllegalArgumentException("Illegal Modbus address for " +
                                             "double word variable: " + 
                                             inAddress);
        length = 2;
        break;
      case TEXT:
        if (size == 1)
          throw new IllegalArgumentException("Illegal Modbus address for " +
                                             "string variable: " + 
                                             inAddress);
        length = this.getType().getSize();
        if (this.getType().getSize() == null) {
          /* default value */
          length = 127;
        }
        break;
      default:
        throw new IllegalArgumentException("Illegal type for Modbus");
    }

    tagSize = length;
  }

}
