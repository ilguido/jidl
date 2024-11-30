/**
 * S7Variable.java
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

package com.github.ilguido.jidl.variable.s7;

import com.github.ilguido.jidl.variable.VariableCommon;
import org.apache.plc4x.java.api.PlcConnection;

/**
 * S7Variable
 * Superclass for managing a variable from a Siemens industrial device 
 * through a Step 7 connection.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public abstract class S7Variable extends VariableCommon {
  /**
   * Class constructor.  It calls the parent class constructor and then sets the
   * client property.
   *
   * @param inName the mnemonic name of the variable
   * @param inAddress the address of the variable, its format depends on the
   *                  connection
   * @param inType the type of the variable as defined in {@link com.github.ilguido.jidl.DataTypes}
   * @param inClient a <code>PlcConnection</code> object from the PLC4J library
   * @throws IllegalArgumentException if address is not a valid address
   */
  public S7Variable(String inName,
                    String inAddress,
                    DataType inType,
                    PlcConnection inClient)
    throws IllegalArgumentException {
    super(inName, inAddress, inType);
  }
  
  /**
  * Checks that a given string is a valid S7 address.  If address is not a valid
  * address, this function throws an exception.
  *
  * @param inAddress the address to be validated
  * @throws IllegalArgumentException if address is not a valid S7 address
  */
  @Override
  protected void validateAddress(String inAddress) 
    throws IllegalArgumentException {
    //PLC4J does everything, nothing to do here
  }
  
  /**
   * Returns the code used by PLC4J to identify the data type.
   *
   * @param inType the data type of the variable
   * @return the PLC4J data type as a string
   */
  static protected String getPLC4JDataType(DataType inType) {
    switch (inType) {
      case BOOLEAN:
        return "BOOL";
      case BYTE:
        return "BYTE";
      case TEXT:
        return "STRING";
      case INTEGER:
        return "INT";
      case WORD:
        return "WORD";
      case DOUBLE_INTEGER:
        return "DINT";
      case REAL:
        return "REAL";
    }
    
    return "";
  }
}
