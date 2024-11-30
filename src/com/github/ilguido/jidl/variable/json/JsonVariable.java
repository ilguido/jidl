/**
 * JsonVariable.java
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

package com.github.ilguido.jidl.variable.json;

import com.github.ilguido.jidl.variable.VariableCommon;

/**
 * JsonVariable
 * Superclass for managing Json variables.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public abstract class JsonVariable extends VariableCommon {
  /**
   * Class constructor.  It calls the parent class constructor and then sets the
   * client property.
   *
   * @param inName the mnemonic name of the variable
   * @param inAddress the address of the variable, its format depends on the
   *                  connection
   * @param inType the type of the variable as defined in {@link com.github.ilguido.jidl.DataTypes}
   * @throws IllegalArgumentException if address is not a valid address
   */
  public JsonVariable(String inName, 
                      String inAddress,
                      DataType inType)
    throws IllegalArgumentException {
    super(inName, inAddress, inType);
  }
  
  /**
  * Checks that a given string is a valid Json address.  If address is not a
  * valid address, this function throws an exception. If it is valid, it
  * populates the addressParameters property.
  *
  * @param inAddress the address to be validated
  * @throws IllegalArgumentException if address is not a valid Json address
  */
  @Override
  protected void validateAddress(String inAddress)
    throws IllegalArgumentException {
    //FIXME: nothing!
  }    
}
