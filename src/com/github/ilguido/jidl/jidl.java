/**
 * jidl.java
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

package com.github.ilguido.jidl;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.github.ilguido.jidl.datalogger.*;
import com.github.ilguido.jidl.datalogger.dataloggerarchiver.DataLoggerArchiver;
import com.github.ilguido.jidl.connectionmanager.*;
import com.github.ilguido.jidl.utils.FileManager;
import com.github.ilguido.jidl.variable.VariableReader;

/**
 * jidl
 * Class to glue together the various parts of jidl.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class jidl implements DataTypes {
  /**
   * The unique data logger.
   */
  protected static DataLogger dataLogger = null;

  /**
   * The list of connections to industrial devices, as a
   * <code>ConnectionManager</code> object list.
   */
  protected static List<ConnectionManager> connectionList;
  
  /**
   * Initializes Jidl parsing the command line arguments.
   *
   * @param args the command line arguments
   * @return a boolean value, whether jidl is ready to start or not
   * @throws IllegalArgumentException if there is an error parsing
   *                                  <code>args</code>
   * @throws ExecutionException if there is an error initializing objects
   */
  protected static boolean initJidl(String[] args) 
    throws IllegalArgumentException, ExecutionException {
    File configurationFile = null;
    boolean autostart = false;
    
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-c")) {
        // configuration file
        i++;
        if (i < args.length) {
          configurationFile = new File(args[i]);
        } else {
          //error: there must be a value associated to the switch
          throw new IllegalArgumentException("-c ?");
        }
      } else if (args[i].equals("-a")) {
        // autostart
        autostart = true;
      }
    }

    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      public void run() {
        System.out.println("Shutting down...");
        if (dataLogger.getStatus())
          dataLogger.stopLogging();
          
        if (dataLogger.isArchiver() && dataLogger.isArchiverSet()) {
          dataLogger.stopArchivingService();
        }
          
      }
    }, "Shutdown-thread"));
    
    if (configurationFile != null) {
      loadConfiguration(configurationFile);
    }
    
    // JIDL is ready to start if autostart is set and the configuration loaded.
    return (autostart && dataLogger != null);
  }
  
  /**
   * Reads the configuration from the configuration file.
   *
   * @param inSelectedFile the configuration File
   * @throws IllegalArgumentException when the <code>inSelectedFile</code> is
   *                                  not a valid configuration file or it is
   *                                  not readable
   * @throws ExecutionException if there is an error initializing objects
   * @return the configuration of the database as a list of maps
   */
  protected
  static List<Map<String, String>> loadConfiguration(File inSelectedFile)
    throws IllegalArgumentException, ExecutionException {
    List<Map<String, String>> configuration, outConfiguration = null;

    // First step: setup the database, according to the configuration
    try {
      configuration = FileManager.loadIniFile(inSelectedFile.getAbsolutePath());
    } catch (Exception e) {
      throw new IllegalArgumentException(inSelectedFile.getAbsolutePath(), e);
    }

    // Parse the configuration
    for (final Map<String, String> sectionMap : configuration) {
      if (sectionMap.get("section").equals("datalogger")) {
        switch (sectionMap.get("type")) {
          case "dummy":
            dataLogger = new DummyDataLogger(sectionMap.get("name"),
                                             sectionMap.get("dir"));
            break;
          case "mariadb":
            dataLogger = new MariaDBDataLogger(sectionMap.get("name"),
                                               sectionMap.get("dir"),
                                               sectionMap.get("server"),
                                               sectionMap.get("port"),
                                               sectionMap.get("username"),
                                               sectionMap.get("password"));
            break;
          case "monetdb":
            dataLogger = new MonetDBDataLogger(sectionMap.get("name"),
                                               sectionMap.get("dir"),
                                               sectionMap.get("server"),
                                               sectionMap.get("port"),
                                               sectionMap.get("username"),
                                               sectionMap.get("password"));
            break;
          case "sqlite":
            dataLogger = new SQLiteDataLogger(sectionMap.get("name"),
                                              sectionMap.get("dir"));
            break;
          default:
            throw new IllegalArgumentException("type = ");
        }
      } else if (sectionMap.get("section").equals("dataarchiver")) {
        if (dataLogger.isArchiver()) {
          dataLogger.setArchivingService(
                            DayOfWeek.valueOf(sectionMap.get("day")).getValue(),
                                   Integer.parseInt(sectionMap.get("interval")),
                               Boolean.parseBoolean(sectionMap.get("monthly")));
        } else {
          throw new IllegalArgumentException("dataarchiver");
        }
      }
    }
    
    // Second step: load the configuration from the database
    // throws: ExecutionException
    outConfiguration = dataLogger.getConfiguration();

    connectionList = parseInitializationData(outConfiguration);
    
    // Final step
    for (ConnectionManager cm : connectionList) {
      dataLogger.addConnection(cm);
    }
    
    return outConfiguration;
  }
  
  /**
   * Parses the configuration data and returns the list of configured
   * connections.
   * 
   * @param inInitializationData the configuration data
   * @return a list of configured {@link jidl.ConnectionManager} objects
   * @throws IllegalArgumentException if the parsing fails, e.g. the
   *                                  configuration misses some required
   *                                  parameter or some parameter is invalid
   *                                  etc.
   */
  private 
  static List<ConnectionManager> 
        parseInitializationData(List<Map<String, String>> inInitializationData) 
    throws IllegalArgumentException {
    List<ConnectionManager> list = new LinkedList<>();
    /* A hashmap of VariableReader objects that we could use as a source when
     * creating new VariableWriters objects.
     */ 
    HashMap<String, VariableReader> vrmap = new HashMap<String, 
                                                        VariableReader>();
    
    if (inInitializationData != null)
      // Parse the configuration
      for (final Map<String, String> sectionMap : inInitializationData) {
        int atIndex = sectionMap.get("section").indexOf("::");
        int toIndex = sectionMap.get("section").indexOf("<-");
        boolean isWriter = false;
        
        /* We distinguish the type of each entry in the configuration table by
         * the format of its name.
         *
         * "Type"     | "Format"
         * connection | xyz 
         * tag reader | abc::xyz
         * tag writer | def::tuw<-abc::xyz
         */
        if (toIndex != -1) {
          isWriter = true;
        } else {
          toIndex = sectionMap.get("section").length();
        }

        if (atIndex == -1) {
          ConnectionManager newc = null;
          // This entry should be the configuration for a connection.
          if (sectionMap.get("type") == null) {
            throw new IllegalArgumentException(sectionMap.get("section") +
                                                   ": missing type!");
          }
          
          try {
            switch (sectionMap.get("type")) {
              case "S7":
                newc = new S7ConnectionManager(sectionMap.get("section"),
                                               sectionMap.get("address"),
                                Integer.parseInt(sectionMap.get("rack")),
                                Integer.parseInt(sectionMap.get("slot")),
                             Integer.parseInt(sectionMap.get("seconds")));
                break;
              case "modbus":
                newc = new ModbusConnectionManager(sectionMap.get("section"),
                                                   sectionMap.get("address"),
                                    Integer.parseInt(sectionMap.get("port")),
                            Boolean.parseBoolean(sectionMap.get("reversed")),
                                 Integer.parseInt(sectionMap.get("seconds")));
                break;
              case "opcua":
                newc = new OPCUAConnectionManager(sectionMap.get("section"),
                                                  sectionMap.get("address"),
                                   Integer.parseInt(sectionMap.get("port")),
                                                  sectionMap.get("path"),
                          Boolean.parseBoolean(sectionMap.get("discovery")),
                                                  sectionMap.get("username"),
                                                  sectionMap.get("password"),
                                 Integer.parseInt(sectionMap.get("seconds")));
                break;
              case "json":
                newc = new JsonConnectionManager(sectionMap.get("section"),
                                                  sectionMap.get("address"),
                                 Integer.parseInt(sectionMap.get("seconds")));
                break;
              default:
                throw new IllegalArgumentException(sectionMap.get("section") +
                                                   ": type = " +
                                                   sectionMap.get("type"));
            }
            /* If there is already a client for this connection, use that. */
            setExistingClientIfAvailable(list, newc);
            /* Add the connection to the list. */
            list.add(newc);
          } catch (NumberFormatException e) {
            throw new IllegalArgumentException(sectionMap.get("section") +
                                               ": illegal number value", e);
          }
        } else {
          // This entry should be the configuration for a variable reader or
          // writer.
          String fullname = sectionMap.get("section");
          String name = fullname.substring(0, atIndex);
          String address = sectionMap.get("address");
          String type = sectionMap.get("type");
          String connection = fullname.substring(atIndex + 2, toIndex);
          String source = null;
          if (isWriter) {
            source = fullname.substring(toIndex + 2);
          }

          if (name != null && address != null &&
              type != null && connection != null &&
              (source != null || !isWriter)) {
            for (ConnectionManager cm : list) {
              if (connection.equals(cm.getName())) {
                if (isWriter) {
                  /* Check whether the connection is writeable. */
                  WriteableConnection wc;
                  if (cm instanceof WriteableConnection) {
                    wc = (WriteableConnection) cm;
                  } else {
                    throw new IllegalArgumentException("Cannot write to a "+
                                                        cm.getType() + 
                                                        " connection!");
                  }
                  
                  /* A new VariableWriter. */
                  /* Look for the source variable of the VariableWriter. */
                  VariableReader vrSource = vrmap.get(source);
                  
                  if (vrSource == null) {
                    /* The source does not exist! */
                    throw new IllegalArgumentException("Configuration error: " +
                                                       source +
                                                       " is not a valid source."
                                                       );
                  }
                  
                  wc.addVariableWriter(name, address, DataType.valueOf(type),
                                       vrSource);
                } else {
                  /* Add a new VariableReader to the connection and to the map
                   * used to assign a source to VariableWriters.
                   */
                  vrmap.put(fullname,
                            cm.addVariableReader(name, 
                                                  address, 
                                                  DataType.valueOf(type)));
                }
                break;
              }
            }
          } else {
            throw new IllegalArgumentException("Configuration error: missing " +
                                               "parameter(s): " + 
                                               sectionMap.get("section"));
          }
        }
      }
    
    return list;
  }
  
  /**
   * Searches for and assigns an existing client to the connection, if found.
   * Connections to the same device can reuse the same client.
   *
   * @param inList the list of created {@link jidl.ConnectionManager} objects
   * @param inCM the newly created {@link jidl.ConnectionManager}
   */
  private
  static void setExistingClientIfAvailable(List<ConnectionManager> inList, 
                                           ConnectionManager inCM) {
    ShareableConnection sc;
    /* Check if this ConnectionManager can share its client. */
    if (inCM instanceof ShareableConnection) {
      sc = (ShareableConnection) inCM;
    } else {
      return;
    }
    
    for (final ConnectionManager cm : inList) {
      if (inCM.getType().equals(cm.getType())) {
        if (inCM.getAddress().equals(cm.getAddress())) {
          try {
            sc.setClient(cm.getClient());
          } catch (IllegalArgumentException iae) {
            //TODO: something?
          }
        }
      }
    }
  }
}
