/**
 * Variable.java
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

import com.github.ilguido.jidl.DataTypes;

/**
 * Variable
 * Common interface for all JIDL variables.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public interface Variable extends DataTypes {
  /**
   * Returns the address of the variable.
   *
   * @return the address of the variable as a string
   */
  public String getAddress();
  
  /**
   * Returns the name of the variable.
   *
   * @return the mnemonic name of the variable
   */
  public String getName();

  /**
   * Returns the type of the variable.
   *
   * @return the type of the variable as defined by {@link com.github.ilguido.jidl.DataTypes}
   */
  public DataType getType();
  
  /**
   * Returns the value of the variable as an object.
   *
   * @return the value of the variable
   */
  public Object getValue();
  
  /**
   * Converts the value of the variable to a string.
   *
   * @return the value of the variable as a string
   */
  public String toString();
}
