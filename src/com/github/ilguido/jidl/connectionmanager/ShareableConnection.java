/**
 * ShareableConnection.java
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

/**
 * ShareableConnection
 * Interface for sharing the same client among connections.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public interface ShareableConnection {
  /**
   * Set the object to be used for connecting to the industrial device.
   *
   * @param inClient the object describing a client 
   * @throws IllegalArgumentException if the object cannot be applied to this
   *                            {@link com.github.ilguido.jidl.connectionmanager.ConnectionManager}
   */
  public void setClient(Object inClient) 
    throws IllegalArgumentException;
}
