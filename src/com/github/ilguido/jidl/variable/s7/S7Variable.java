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
   * The size property of a S7 string.  Strings in S7 have a size in characters,
   * this is it.
   */
  private Integer stringSize;
    
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
    
    if (inType == DataType.TEXT) {
      stringSize = inType.getSize();
      
      if (stringSize == null) {
        /* Set the default value for a string. */
        stringSize = 254;
      } else if (stringSize > 254) {
        /* Maximum size is 254. */
        throw 
          new IllegalArgumentException("Maximum size for S7 strings is 254.");
      }
    }
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
   * Returns the address used by PLC4J to identify the variable on the PLC.
   *
   * @return the PLC4J address as a string
   */
  protected String getPLC4JAddress() {
    String s = "%" + this.getAddress() + ":";
    
    switch (this.getType()) {
      case BOOLEAN:
        s += "BOOL";
        break;
      case BYTE:
        s += "BYTE";
        break;
      case TEXT:
        s += "STRING(" + stringSize.toString() + ")";
        break;
      case INTEGER:
        s += "INT";
        break;
      case WORD:
        s += "WORD";
        break;
      case DOUBLE_INTEGER:
        s += "DINT";
        break;
      case REAL:
        s += "REAL";
        break;
    }
    
    return s;
  }
}
