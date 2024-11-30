/**
 * S7VariableReader.java
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

package com.github.ilguido.jidl.variable.s7;

import java.io.IOException;
import java.util.Objects;

import static java.util.concurrent.TimeUnit.SECONDS;

import com.github.ilguido.jidl.variable.Variable;
import com.github.ilguido.jidl.variable.VariableReader;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.messages.PlcReadRequest;
import org.apache.plc4x.java.api.messages.PlcReadResponse;
import org.apache.plc4x.java.api.types.PlcResponseCode;

/**
 * S7VariableReader
 * Class for reading a variable from a Siemens industrial device through a Step
 * 7 connection.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class S7VariableReader extends S7Variable
                              implements VariableReader {
  /**
   * Class constructor.  It calls the parent class constructor and then sets the
   * client property.
   *
   * @param inName the mnemonic name of the variable
   * @param inAddress the address of the variable, its format depends on the
   *                  connection
   * @param inType the type of the variable as defined in {@link com.github.ilguido.jidl.DataTypes}
   * @param inClient a <code>PlcConnection</code> object from the PLC4J library
   * @throws IllegalArgumentException if address is not a valid address
   */
  public S7VariableReader(String inName,
                          String inAddress,
                          DataType inType,
                          PlcConnection inClient)
    throws IllegalArgumentException {
    super(inName, inAddress, inType, inClient);

    /* If there is a working client, check the connection to the variable. */
    if (inClient != null) {
      PlcReadRequest readRequest = inClient.readRequestBuilder()
        .addItem(inName, "%" + inAddress + ":" + getPLC4JDataType(inType))
        .build();
    }
  }

  /**
   * Reads the value of the variable from the remote device and returns itself.
   *
   * @param inClient a <code>PlcConnection</code> object from the PLC4J library
   * @return <code>this</code>
   * @throws IOException in case of a connection error
   * @throws Exception in case of error when reading a variable
   */
  @Override
  public Variable read(Object inClient)
    throws IOException, Exception {
    PlcConnection client = (PlcConnection) inClient;
    PlcReadRequest readRequest = client.readRequestBuilder()
      .addItem(this.getName(), "%" + this.getAddress() + ":" + 
               getPLC4JDataType(this.getType()))
      .build();
      
    //TODO: differentiate between connection errors and configuration errors
    PlcReadResponse response = readRequest.execute().get(1, SECONDS);
    for (String fieldName : response.getFieldNames()) {
      if(response.getResponseCode(fieldName) != PlcResponseCode.OK) {
        throw new Exception("Cannot read from connection");
      } else {
        if (this.getType() == DataType.TEXT) {
          Object items[] = new Object[254];
          for(int i = 0; i < 254; i++) {
            items[i] = response.getObject(fieldName, i);
          }
          String s = "";
          for (Object o : items) {
            int i = Integer.parseInt(o.toString());
            s += (char) i;
          }
          value = s;
        } else {
          value = response.getObject(fieldName);
        }
      }
    }
    
    return this;
  }
}
