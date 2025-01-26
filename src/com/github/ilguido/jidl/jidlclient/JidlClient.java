/**
 * JidlClient.java
 *
 * Copyright (c) 2025 Stefano Guidoni
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

package com.github.ilguido.jidl.jidlclient;

import com.github.cliftonlabs.json_simple.JsonObject;

/**
 * JidlClient
 * Common interface for all JIDL clients.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public interface JidlClient {  
  /**
   * Returns the data from the last reading as a <code>JsonObject</code>.
   *
   * @return the data read from the remote resource
   */
  public JsonObject getDataAsJsonObject();
  
  /**
   * Returns the data from the last reading as a <code>String</code>.
   *
   * @return the data read from the remote resource
   */
  public String getDataAsString();
}
 
