/**
 * DataLoggerRequestHandler.java
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

package com.github.ilguido.jidl.datalogger;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.github.ilguido.jidl.connectionmanager.ConnectionManager;
import com.github.ilguido.jidl.datalogger.DataLogger;
import com.github.ilguido.jidl.ipc.RequestHandlerInterface;
import com.github.cliftonlabs.json_simple.JsonArray;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;

/**
 * DataLoggerRequestHandler
 * A class that implements a request handler for the DataLogger class with jidl
 * IPC.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */
 
public class DataLoggerRequestHandler implements RequestHandlerInterface {
  /**
   * This is <code>true</code> if the data logger can started or stopped through
   * IPC.
   */
  private final boolean controlEnable;
  
  /**
   * The data logger served by this request handler.
   */
  private final DataLogger datalogger;
  
  /**
   * Initializes the request handler.
   *
   * @param inControlEnable a boolean value, which is <code>true</code> if the
   *                        data logger can be started or stopped by IPC
   * @param inDatalogger the data logger served by this request handler
   */
  public DataLoggerRequestHandler(final boolean inControlEnable,
                                  final DataLogger inDatalogger) {
    controlEnable = inControlEnable;
    datalogger = inDatalogger;
  }
  
  /**
   * A request handler for the {@link DataLogger DataLogger} of the jidl 
   * server IPC.
   *
   * @param inMethod the requested method
   * @param inPayload the requested payload, must be a JSON value
   * @return the result, must be a JSON value
   * @throws IllegalArgumentException if the method is an unknown or invalid
   *                                  method
   * @throws RuntimeException if starting the data logger fails
   */
  public JsonObject handleRequest(String inMethod, 
                                  JsonObject inPayload) {
    switch (inMethod) {
      /* values: request one or more actual values */
      case "values":
        /* Expected format of the input payload:
            { "connection_name_1": ["var_1", "var_2", "var_3"],
              "connection_name_2": ["var_4", "var_5", "var_6"],
              ...
            }
                        
            Output payload:
            { "var_1::connection_name_1": value_1,
              "var_2::connection_name_1": value_2,
              ...
              "var_4::connection_name_2": value_4,
              ...             
            }
            
            If some variable or connection does not exist, return a bad value
            and a null payload.
          */
        return handleValueRequest(inPayload);

      /* start: start the data logger, if allowed */
      case "start":
        if (!controlEnable)
          throw new IllegalArgumentException("handleRequest: inMethod: " +
                                             inMethod);
        
        if (!datalogger.getStatus())
          try {
            datalogger.startLogging();
          } catch (ExecutionException ee) {
            /* encapsulate this in a runtime exception, because the interface
               does not expect an execution exception */
            throw new RuntimeException(ee);
          }
        return null;

      /* stop: stop the data logger, if allowed */
      case "stop":
        if (!controlEnable)
          throw new IllegalArgumentException("handleRequest: inMethod: " +
                                             inMethod);
          
        if (datalogger.getStatus())
          datalogger.stopLogging();
        return null;

      /* trends: request one or more trends of variables */
      case "trends":
        //TODO: add a method for this in the data logger class.
        return null;
      default:
        throw new IllegalArgumentException("handleRequest: inMethod: " +
                                            inMethod);
    }
  }
  
  /**
   * Returns a JsonObject, containing the actual values for the requested
   * variables.
   *
   * @param inRequestMap a map containing names of connections as keys and 
   *                     arrays of variable names as values
   * @return a JsonObject containing the full qualifiers of variables and their
   *         value
   */
  private JsonObject handleValueRequest(JsonObject inRequestMap) {
    JsonObject outJO = new JsonObject();
    
    /* iterations through all requested connection names */
    for (Object requestedConnection : inRequestMap.keySet()) {
      String rcS = (String) requestedConnection;
      
      JsonArray varList= (JsonArray) inRequestMap.get(rcS);
      ConnectionManager cm = datalogger.getConnectionByName(rcS);

      /* iterations through all requested variable names of a connection */
      for (Object requestedValue : varList) {
        outJO.put((String) requestedValue + "::" + rcS, 
                  cm.getVariableValueByName((String) requestedValue));
      }
    }
    
    return outJO;
  }
} 
