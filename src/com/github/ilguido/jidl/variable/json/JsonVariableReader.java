/**
 * JsonVariableReader.java
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

package com.github.ilguido.jidl.variable.json;

import java.io.IOException;
import java.util.Objects;

import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonKey;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

import com.github.ilguido.jidl.jidlclient.HTTPJidlClient;
import com.github.ilguido.jidl.variable.Variable;
import com.github.ilguido.jidl.variable.VariableReader;

/**
 * JsonVariableReader
 * Class for reading Json variables.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class JsonVariableReader extends JsonVariable 
                                implements VariableReader {
  /**
   * This JsonKey is used to retrieve the valeu of the associated variable from
   * the Json data.
   */
  private final JsonKey jkey;
  
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
  public JsonVariableReader(String inName, 
                              String inAddress,
                              DataType inType)
    throws IllegalArgumentException {
    super(inName, inAddress, inType);
    
    //FIXME: the default value.
    jkey = Jsoner.mintJsonKey(inAddress, null);
  }

  /**
   * Reads the value of the variable from the remote device and returns itself.
   *
   * @param inClient a {@link com.github.ilguido.jidl.jidlclient.HTTPJidlClient} as an 
   *                 <code>Object</code>
   * @return <code>this</code>
   * @throws IOException in case of a connection error
   * @throws Exception in case of error when reading a variable
   */
  @Override
  public Variable read(Object inClient)
    throws IOException, Exception {
    /* Try to get the json data from the client. */
    JsonObject jo = null;
    try {
      HTTPJidlClient client = (HTTPJidlClient) inClient;
      jo = client.getDataAsJsonObject();
    } catch (Exception e) {
      //FIXME: do something
    }
    
    /* Try to read the value of this variable from the Json data. */
    try {
      switch (this.getType()) {
        case BOOLEAN:
          value = jo.getBooleanOrDefault(jkey);
          break;
        case INTEGER:
        case DOUBLE_INTEGER:
        case BYTE:
        case WORD:
          value = jo.getIntegerOrDefault(jkey);
          break;
        case FLOAT:
        case REAL:
          value = jo.getFloatOrDefault(jkey);
          break;
        case TEXT:
          value = jo.getStringOrDefault(jkey);
          break;
        default:
          throw new Exception ("Unexpected error...");
      }
    } catch (Exception e) {
      //FIXME: do something.
    }
    
    return this;
  }
}
