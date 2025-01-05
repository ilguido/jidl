/**
 * JidlProtocolStatusCode.java
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

/**
 * JidlProtocolStatusCode
 * An enumeration of status codes for the Jidl Protocol.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */
 
public enum JidlProtocolStatusCode {
  /* NOTE:
   * these status codes are encoded in one byte of data and they are
   * organized as follows
   *      00xxxx request codes
   *      01xxxx good response codes
   *      10xxxx bad response codes
   *      11xxxx reserved
   */
  REQUEST(0, "request"),
  REQUEST_WITHOUT_METHOD(1, "request without method"),
  REQUEST_WITHOUT_PAYLOAD(2, "request without payload"),
  REQUEST_WITHOUT_METHOD_AND_PAYLOAD(3, "request without method and payload"),
  GOOD_RESPONSE(64, "OK"),
  GOOD_RESPONSE_WITH_PAYLOAD(65, "payload"), 
  BAD_RESPONSE(128, "error"),
  BAD_RESPONSE_UNRECOGNIZED_PROTOCOL(129, "unrecognized protocol"),
  BAD_RESPONSE_INCOMPLETE_DATA(130, "incomplete data"),
  BAD_RESPONSE_INVALID_STATUS_CODE(131, "invalid status code"),
  BAD_RESPONSE_INVALID_BODY(132, "invalid body"),
  BAD_RESPONSE_BUFFER_OVERFLOW(133, "buffer overflow"),
  BAD_RESPONSE_FAILED_REQUEST_HANDLING(134, "failed request handling");

  /**
   * The numeric value of a status code.
   */
  public final int rawValue;
  
  /**
   * The text message describing a status code.
   */
  public final String textMessage;

  /**
   * Initializes a status code with its value and message.
   *
   * @param inValue the raw value
   * @param inMessage the text message
   */
  JidlProtocolStatusCode(int inValue, String inMessage) {
    this.rawValue = inValue;
    this.textMessage = inMessage;
  }

  /**
   * Returns the JidlProtocolStatusCode associated with the input value.  If
   * the raw value does not match a status code, this function returns
   * <code>null</code>.
   *
   * @param inValue the raw value of a status code
   * @return a JidlProtocolStatusCode or <code>null</code>
   */
  public static JidlProtocolStatusCode fromRawValue(final int inValue) {
    for (JidlProtocolStatusCode jpsc : JidlProtocolStatusCode.values()) {
      if (jpsc.rawValue == inValue) {
        return jpsc;
      }
    }
    return null;
  }
  
  /**
   * Returns <code>true</code> if the provided status code is a bad response.
   *
   * @param inStatusCode a JidlProtocolStatusCode
   * @return <code>true</code> for a bad response, <code>false</code> otherwise
   */
  public static boolean isBad(final JidlProtocolStatusCode inStatusCode) {
    if (inStatusCode.rawValue > 127 && inStatusCode.rawValue < 192) {
      return true;
    }
    
    return false;
  }
  
  /**
   * Returns <code>true</code> if the provided status code is a good response.
   *
   * @param inStatusCode a JidlProtocolStatusCode
   * @return <code>true</code> for a good response, <code>false</code> otherwise
   */
  public static boolean isGood(final JidlProtocolStatusCode inStatusCode) {
    if (inStatusCode.rawValue > 63 && inStatusCode.rawValue < 128) {
      return true;
    }
    
    return false;
  }
  
  /**
   * Returns <code>true</code> if the provided status code is a request.
   *
   * @param inStatusCode a JidlProtocolStatusCode
   * @return <code>true</code> for a request, <code>false</code> otherwise
   */
  public static boolean isRequest(final JidlProtocolStatusCode inStatusCode) {
    if (inStatusCode.rawValue < 64) {
      return true;
    }
    
    return false;
  }
}

