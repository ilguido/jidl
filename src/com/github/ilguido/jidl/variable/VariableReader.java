/**
 * VariableReader.java
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

/**
 * VariableReader
 * Interface for reading from a variable from an industrial device.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public interface VariableReader extends Variable {
  /**
   * Reads the value of the variable from the remote device and returns a
   * handle to itself.
   *
   * @param inClient a client as an object
   * @return a handle to itself
   * @throws IOException in case of a connection error
   * @throws Exception in case of error when reading a variable
   */
  public Variable read(Object inClient) throws IOException, Exception;
}
