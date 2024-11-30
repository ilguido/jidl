/**
 * VariableCommon.java
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

package com.github.ilguido.jidl.variable;

import java.io.IOException;

import com.github.ilguido.jidl.utils.Validator;

/**
 * VariableCommon
 * Superclass for managing a variable from an industrial device.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public abstract class VariableCommon implements Variable {
  /**
   * The object read from the server.
   */
  protected Object value;
  
  /**
   * The name of the variable.
   */
  private final String name;

  /**
   * The address of the variable as a string.
   */
  private final String address;

  /**
   * The type of the variable as defined in {@link com.github.ilguido.jidl.DataTypes}.
   */
  private final DataType type;

  /**
  * Class constructor.
  *
  * @param inName the mnemonic name of the variable
  * @param inAddress the address of the variable, its format depends on the
  *                  connection
  * @param inType the type of the variable as defined in {@link com.github.ilguido.jidl.DataTypes}
  * @throws IllegalArgumentException if address is not a valid address or name
  *                                  is not a valid name
  */
  public VariableCommon(String inName,
                        String inAddress,
                        DataType inType)
    throws IllegalArgumentException {
    type = inType.validate();
    
    validateAddress(inAddress);
    address = inAddress;
    
    name = Validator.validateString(inName);
  }

  /**
   * Returns the address of the variable.
   *
   * @return the address of the variable as a string
   */
  public String getAddress() {
    return address;
  }
  
  /**
   * Returns the name of the variable.
   *
   * @return the mnemonic name of the variable
   */
  public String getName() {
    return name;
  }

  /**
   * Returns the type of the variable.
   *
   * @return the type of the variable as defined by {@link com.github.ilguido.jidl.DataTypes}
   */
  public DataType getType() {
    return type;
  }
  
  /**
   * Returns the value of the variable as an object.
   *
   * @return the value of the variable
   */
  public Object getValue() {
    return value;
  }
  
  /**
   * Converts the value of the variable to a string.
   *
   * @return the value of the variable as a string
   */
  public String toString() {
    return value.toString();
  }
  
  /**
  * Checks that a given string is a valid address.  Validity of the address
  * depends on the type of connection used, so subclasses must implement their
  * own validity checks.
  *
  * @param inAddress the address to be validated 
  * @throws IllegalArgumentException if address is not a valid address
  */
  protected abstract void validateAddress(String inAddress) 
    throws IllegalArgumentException;
}
