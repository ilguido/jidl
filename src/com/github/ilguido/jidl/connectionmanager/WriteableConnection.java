/**
 * WriteableConnection.java
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

package com.github.ilguido.jidl.connectionmanager;

import com.github.ilguido.jidl.DataTypes;
import com.github.ilguido.jidl.connectionmanager.ConnectionManager;
import com.github.ilguido.jidl.variable.VariableReader;
import com.github.ilguido.jidl.variable.VariableWriter;

/**
 * WriteableConnection
 * Interface for writing to a connection.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public interface WriteableConnection extends DataTypes {
  /**
   * Adds a new {@link com.github.ilguido.jidl.variable.VariableWriter} object to the
   * connection. Each connection is a link to a number of variables of the
   * remote device, stored in a list.
   *
   * @param inName the mnemonic name of the variable
   * @param inAddress the address of the variable, its format depends on the
   *                  connection
   * @param inType the type of the variable as defined in {@link com.github.ilguido.jidl.DataTypes}
   * @param inSource the source variable
   * @throws IllegalArgumentException if the <code>inType</code> is an invalid
   *                                  type
   * @return the created {@link com.github.ilguido.jidl.variable.VariableWriter} object
   */
  public abstract VariableWriter addVariableWriter(String inName, 
                                                   String inAddress,
                                                   DataType inType, 
                                                   VariableReader inSource)
    throws IllegalArgumentException;

  /**
   * Writes all the variables listed in this connection and returns itself.
   *
   * @return this {@link com.github.ilguido.jidl.connectionmanager.ConnectionManager} object
   */
  public ConnectionManager write();
}
