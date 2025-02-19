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
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.SSLContext;

import com.github.ilguido.jidl.datalogger.*;
import com.github.ilguido.jidl.datalogger.dataloggerarchiver.DataLoggerArchiver;
import com.github.ilguido.jidl.connectionmanager.*;
import com.github.ilguido.jidl.utils.Decrypter;
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
   * When this is true, the data logger can be started or stopped through IPC.
   */
  private static boolean remoteControl = false;
  
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
      } else if (args[i].equals("-r")) {
        // controls through IPC
        remoteControl = true;
      }
    }

    Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
      public void run() {
        System.out.println("Shutting down...");
        if (dataLogger != null) {
          if (dataLogger.getStatus())
            dataLogger.stopLogging();
            
          if (dataLogger.getIPCStatus())
            dataLogger.stopIPCServer();
            
          if (dataLogger.isArchiver() && dataLogger.isArchiverSet()) {
            dataLogger.stopArchivingService();
          }
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
   * @throws ExecutionException if an error occurs when initializing objects
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
            throw new IllegalArgumentException("type = " + 
                                               sectionMap.get("type"));
        }
        Decrypter.setSecretKey(sectionMap.get("key"));
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
    
    /* Reset everything. */
    dataLogger.stopLogging();
    
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
   * @throws ExecutionException if an error occurs when initializing objects
   */
  private 
  static List<ConnectionManager> 
        parseInitializationData(List<Map<String, String>> inInitializationData)
    throws IllegalArgumentException, ExecutionException {
    List<ConnectionManager> list = new LinkedList<>();
    /* A hashmap of VariableReader objects that we could use as a source when
     * creating new VariableWriters objects.
     */ 
    HashMap<String, VariableReader> vrmap = new HashMap<String, 
                                                        VariableReader>();
    
    if (inInitializationData != null)
      // Parse the configuration
      for (final Map<String, String> sectionMap : inInitializationData) {
        String[] sectionArray = ConnectionManager
                                  .splitQualifier(sectionMap.get("section"));
        
        /* We distinguish the type of each entry in the configuration table by
         * the format of its name.
         *
         * "Type"     | "Format"
         * main conf. | (empty, no name, only one such section)
         * connection | xyz 
         * tag reader | abc::xyz
         * tag writer | def::tuw<-abc::xyz
         */

        if (sectionArray[0] == null) {
          /* The only empty section is for the general configuration. */
          
          /* IPC server configuration. */
          if (sectionMap.get("ipc_port") != null &&
              sectionMap.get("ipc_keystore") != null &&
              sectionMap.get("ipc_keystorepw") != null &&
              sectionMap.get("ipc_truststore") != null &&
              sectionMap.get("ipc_truststorepw") != null) {
            /* Passwords can be (should be?) encrypted. */
            String keystorePassword = 
              Decrypter.decrypt(sectionMap.get("ipc_keystorepw"), 
                                sectionMap.get("salt"), 
                                sectionMap.get("iv"));
            String truststorePassword = 
              Decrypter.decrypt(sectionMap.get("ipc_truststorepw"),
                                sectionMap.get("salt"), 
                                sectionMap.get("iv"));

            /* Let's generate an SSL context. */
            SSLContext sslctx = null;
            try {
              /* Procedure:
               *  1.generate a keystore from the keystore file and keystore
               *    password
               *  2.generate a truststore from the truststore file and 
               *    truststore password
               *  3.generate a KeyManagerFactory from the keystore and the
               *    keystore password
               *  4.generate a TrustManagerFactory from the truststore
               *  5.generate an SSLContext, initialize it with the 
               *    KeyManagerFactory and the TrustManagerFactory
               *
               *  An SSLContext is used to generate an SSLServerSocket with
               *  specific (not system-wide, not JVM-wide) certificates.
               */
              KeyStore keyStore = KeyStore
                                    .getInstance(KeyStore.getDefaultType());
              keyStore.load(new FileInputStream(sectionMap
                                                  .get("ipc_keystore")),
                            keystorePassword.toCharArray());

              KeyStore trustStore = KeyStore.getInstance(KeyStore
                                                          .getDefaultType());
              trustStore.load(new FileInputStream(sectionMap
                                                    .get("ipc_truststore")), 
                              truststorePassword.toCharArray());

              KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                                        KeyManagerFactory
                                          .getDefaultAlgorithm());
              kmf.init(keyStore, keystorePassword.toCharArray());
              TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                                          TrustManagerFactory
                                            .getDefaultAlgorithm());
              tmf.init(trustStore);

              sslctx = SSLContext.getInstance("TLSv1.2");
              sslctx.init(kmf.getKeyManagers(), 
                          tmf.getTrustManagers(),
                          SecureRandom.getInstanceStrong());
            } catch (Exception e) {
              throw new ExecutionException("Failed to initialize IPC server",
                                           e);
            }
            
            dataLogger
              .addIPCServer(Integer.parseInt(sectionMap.get("ipc_port")),
                            remoteControl,
                            sslctx);
          }
        } else if (sectionArray[1] == null) {
          ConnectionManager newc = null;
          // This entry should be the configuration for a connection.
          if (sectionMap.get("type") == null) {
            throw new IllegalArgumentException(sectionMap.get("section") +
                                                   ": missing type!");
          }
          
          try {
            switch (sectionMap.get("type")) {
              case "s7":
                newc = new S7ConnectionManager(sectionMap.get("section"),
                                               sectionMap.get("address"),
                                Integer.parseInt(sectionMap.get("rack")),
                                Integer.parseInt(sectionMap.get("slot")),
                               parseSampleTime(sectionMap.get("seconds"),
                                         sectionMap.get("deciseconds")));
                break;
              case "modbus-tcp":
                newc = new ModbusConnectionManager(sectionMap.get("section"),
                                                   sectionMap.get("address"),
                                    Integer.parseInt(sectionMap.get("port")),
                            Boolean.parseBoolean(sectionMap.get("reversed")),
                                   parseSampleTime(sectionMap.get("seconds"),
                                             sectionMap.get("deciseconds")));
                break;
              case "opcua":
                newc = new OPCUAConnectionManager(sectionMap.get("section"),
                                                  sectionMap.get("address"),
                                   Integer.parseInt(sectionMap.get("port")),
                                                  sectionMap.get("path"),
                          Boolean.parseBoolean(sectionMap.get("discovery")),
                                                  sectionMap.get("username"),
                                                  sectionMap.get("password"),
                                                  sectionMap.get("salt"),
                                                  sectionMap.get("iv"),
                                 parseSampleTime(sectionMap.get("seconds"),
                                            sectionMap.get("deciseconds")));
                break;
              case "json":
                newc = new JsonConnectionManager(sectionMap.get("section"),
                                                 sectionMap.get("address"),
                                 parseSampleTime(sectionMap.get("seconds"),
                                           sectionMap.get("deciseconds")));
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
          String name = sectionArray[0];
          String address = sectionMap.get("address");
          String type = sectionMap.get("type");
          String connection = sectionArray[1];
          String source = sectionArray[2];

          if (address != null && type != null) {
            for (ConnectionManager cm : list) {
              if (connection.equals(cm.getName())) {
                if (source != null) {
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
                  
                  wc.addVariableWriter(name, 
                                       address, 
                                       DataType.valueOfDataType(type),
                                       vrSource);
                } else {
                  /* Add a new VariableReader to the connection and to the map
                   * used to assign a source to VariableWriters.
                   */
                  vrmap.put(fullname,
                            cm.addVariableReader(name, 
                                                 address, 
                                                 DataType.valueOfDataType(type)
                                                 ));
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
   * Parses the sample time setting of a connection.  The sample time of a 
   * connection can be set in seconds or deciseconds, but not both. Sample times
   * longer than 1 second are rounded to the nearest second. The default return
   * value is zero.
   *
   * @param inSeconds the desired sample time in seconds
   * @param inDeciseconds the desired sample time in deciseconds
   * @return the sample time in deciseconds, or zero when no valid sample time
   *         was parsed
   * @throws IllegalArgumentException when both seconds and deciseconds are set
   */
  private static int parseSampleTime(String inSeconds, String inDeciseconds)
    throws IllegalArgumentException {
    if (inSeconds != null && inDeciseconds != null) {
      throw new IllegalArgumentException ("Connection has both seconds and " +
                                          "deciseconds fields set.");
    }
    
    if (inSeconds != null) {
      /* Multiply by ten: jidl uses deciseconds internally. */
      return 10 * Integer.parseInt(inSeconds);
    }
    
    if (inDeciseconds != null) {
      if (inDeciseconds.length() > 1) {
        /* Sample times above 1 second are rounded to the nearest second. */
        return (int) Math.round(Integer.parseInt(inDeciseconds) / 10.0) * 10;
      } else {
        return Integer.parseInt(inDeciseconds);
      }
    }
    
    return 0;
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
