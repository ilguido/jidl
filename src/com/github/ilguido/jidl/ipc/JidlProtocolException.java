/**
 * JidlProtocolException.java
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

package com.github.ilguido.jidl.ipc;

import com.github.ilguido.jidl.ipc.JidlProtocolStatusCode;

/**
 * JidlProtocolException
 * A custom exception for the Jidl Protocol.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */
 
public final class JidlProtocolException extends RuntimeException {
  /**
   * The {@link com.github.ilguido.jidl.ipc.JidlProtocolStatusCode} associated
   * with this exception.
   */
  private JidlProtocolStatusCode statusCode;
  
  /**
   * Initializes a JidlProtocolException with its status code.
   * 
   * @param inStatusCode a 
   *                  {@link com.github.ilguido.jidl.ipc.JidlProtocolStatusCode}
   */
  public JidlProtocolException(JidlProtocolStatusCode inStatusCode) {
    super(inStatusCode.textMessage);
    
    statusCode = inStatusCode;
  }

  /**
   * Returns the status code of this exception.
   *
   * @return the {@link com.github.ilguido.jidl.ipc.JidlProtocolStatusCode} of
   *         this exception
   */
  public JidlProtocolStatusCode getStatusCode() {
    return statusCode;
  }
}
