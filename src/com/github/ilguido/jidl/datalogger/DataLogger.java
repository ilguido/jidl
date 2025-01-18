/**
 * DataLogger.java
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

package com.github.ilguido.jidl.datalogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import com.github.ilguido.jidl.DataTypes;
import com.github.ilguido.jidl.connectionmanager.ConnectionManager;
import com.github.ilguido.jidl.connectionmanager.WriteableConnection;
import com.github.ilguido.jidl.datalogger.dataloggerarchiver.DataLoggerArchiver;
import com.github.ilguido.jidl.ipc.DataLoggerRequestHandler;
import com.github.ilguido.jidl.ipc.JidlProtocolServer;
import com.github.ilguido.jidl.utils.TimeString;

/**
 * DataLogger
 * Superclass to manage the logging of data.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public abstract class DataLogger implements DataTypes, DataLoggerArchiver {
  /**
   * List of connections that are managed by this logger.  It is a list
   * of {@link com.github.ilguido.jidl.connectionmanager.ConnectionManager} objects.
   */
  protected ArrayList<ConnectionManager> connectionList;

  /**
   * Timer for logging data at fixed intervals.
   */
  private ScheduledExecutorService timer;

  /**
   * A counter for the seconds elapsed since the logger started.
   */
  private int internalCounter;

  /**
   * A server for IPC.
   */
  private JidlProtocolServer ipcServer = null;
   
  /**
   * Date format.
   */
  private String dateFormat = "yyyy-MM-dd HH:mm:ss,S";

  /**
   * Path to the working directory.  The directory where the data base is
   * stored.
   */
  private final String directory;

  /**
   * Name of the data base.  The name of the data base is also the file name
   * of the database and must be unique.
   */
  private final String name;

  /**
   * The name of the timestamp field.  Every table has a timestamp field, where
   * to store the timestamp of the data reading.
   */
  private final String timestampS = "TIMESTAMP";

  /**
   * Class constructor.  It sets the name of the data base and the path to the
   * working directory.
   *
   * @param inName the file name of the data base
   * @param inDir the path to the working directory where the data base is
   *              stored
   * @throws IllegalArgumentException if the path or name are not valid
   */
  public DataLogger(String inName, String inDir)
    throws IllegalArgumentException {
    Path path;

    try {
      path = Paths.get(inDir);
    } catch (InvalidPathException | NullPointerException e) {
      throw new IllegalArgumentException("Cannot access path: " + inDir, e);
    }

    if (Files.isDirectory(path))
      directory = inDir;
    else
      throw new IllegalArgumentException("This is not a directory: " + inDir);

    try {
      path = Paths.get(inName);
    } catch (InvalidPathException | NullPointerException e) {
      throw new IllegalArgumentException("Not a valid name: " + inName, e);
    }

    name = inName;
    timer = null;

    internalCounter = 0;

    connectionList = new ArrayList<ConnectionManager>();
  }

  /**
   * Adds a new {@link com.github.ilguido.jidl.connectionmanager.ConnectionManager}
   * object to the <code>connectionList</code> list.  It throws an exception
   * if there is already an object with the same name.
   *
   * @param inConnection the name of the new connection
   * @throws IllegalArgumentException if a connection by the same name is
   *                                  already served by this logger
   */
  public void addConnection(ConnectionManager inConnection)
    throws IllegalArgumentException {
    addConnectionCommon(inConnection); // maybe there will be a split between
                                       // addConnectionWithoutVariables and
                                       // addConnectionWithVariables
  }
  
  /**
   * Initializes an IPC server.  It requires the certificates to establish 
   * trusted connections. It can start and stop the data logger, if enabled.
   * The server is immediately started.
   *
   * @param inPort the port of the server
   * @param inControlEnable a boolean value, which is <code>true</code> if the
   *                        data logger can be started or stopped by IPC
   * @param inKeyStore the path to the certificate file of the server
   * @param inKeyStorePassword the password to open the certificate file
   * @param inTrustStore the path to the file with the trusted certificates
   * @param inTrustStorePassword the password to open the file with the trusted
   *                             certificates
   * @throws IllegalArgumentException if at least one of the files does not 
   *                                  exist, or the path is <code>null</code>
   * @throws RuntimeException if a server was already added or if the server
   *                          failed to start
   */
  public void addIPCServer(final int inPort, 
                           final boolean inControlEnable,
                           final String inKeyStore,
                           final String inKeyStorePassword,
                           final String inTrustStore,
                           final String inTrustStorePassword)
    throws IllegalArgumentException, RuntimeException {
    if (ipcServer != null)
      throw new RuntimeException("DataLogger: addIPCServer: ipcServer != null");
    
    DataLoggerRequestHandler 
      dlrh = new DataLoggerRequestHandler(inControlEnable, this);
    
    ipcServer = new JidlProtocolServer(inPort,
                                       dlrh,
                                       inKeyStore,
                                       inKeyStorePassword,
                                       inTrustStore,
                                       inTrustStorePassword);
    try {
      ipcServer.start();
    } catch (Exception e) {
      throw new RuntimeException("DataLogger: addIPCServer: ipcServer.start()");
    }
  }
  
  /**
   * Returns the date format.
   *
   * @return the date format as a string
   */
  public String getDateFormat() {
    return dateFormat;
  }
  
  /**
   * Returns the status of the IPC server.  If the server is up and running,
   * the status is <code>true</code>.
   *
   * @return a boolean value, <code>true</code> if IPC is working
   */
  public boolean getIPCStatus() {
    if (ipcServer != null) {
      if (ipcServer.isStarted())
        return true;
    }
    
    return false;
  }
  
  /**
   * Returns the status of the data logger.  If the data logging is active,
   * the status is <code>true</code>.
   *
   * @return a boolean value, <code>true</code> for an active data logging
   */
  public boolean getStatus() {
    return (timer != null);
  }
  
  /**
   * Returns true if this data logger works with an embedded database.
   *
   * @return <code>true</code> if this object implements the archiver facility
   */
  @Override
  public boolean isArchiver() {
    return false;
  }
  
  /**
   * Returns true if the archiving service is set.
   *
   * @return <code>true</code> if this object is configured to archive its data
   */
  @Override
  public boolean isArchiverSet() {
    return false;
  }

  /**
   * Sets the configuration and starts the archiving service.
   *
   * @param inDayOfWeek the day of the week for the backup, as an integer
   *                    between 1 and 7
   * @param inInterval weeks or months between a backup and the next, 1 means
   *                   every week or month
   * @param inUseMonths when this is true, the interval is the number of months
   *                    between a backup and the next
   * @throws IllegalArgumentException when some parameters is out of range
   */
  @Override
  public void setArchivingService(int inDayOfWeek,
                                  int inInterval,
                                  boolean inUseMonths)
    throws IllegalArgumentException {
    /* nothing to do */
  }
    
  /**
   * Stops the archiving service.
   */
  @Override
  public void stopArchivingService() {
    /* nothing to do */
  }

  /**
   * Starts the data logging.  It throws an exception when it is not ready to
   * log data, e.g. when the driver to connect to a database is not initialized.
   *
   * @throws ExecutionException if the {@link com.github.ilguido.jidl.datalogger.DataLogger} cannot
   *                            complete a necessary task, e.g. connect to the
   *                            database
   */
  public void startLogging()
    throws ExecutionException {
    startLogging(null);
  }

  /**
   * Starts the data logging.  It throws an exception when it is not ready to
   * log data, e.g. when the driver to connect to a database is not initialized.
   *
   * @param inHandler a handler function to catch an exception that stops the
   *                  data logging, it can be used to make the user aware of the
   *                  stoppage
   * @throws ExecutionException if the {@link com.github.ilguido.jidl.datalogger.DataLogger} cannot
   *                            complete a necessary task, e.g. connect to the
   *                            database
   */
  public void startLogging(Thread.UncaughtExceptionHandler inHandler)
    throws ExecutionException {
    log(name + ": startLogging()", false);
    
    /* This flag is true if there are connections having sample times below 
     * 1 second.
     */
    boolean decisecondStep = false;
    
    for (final ConnectionManager connection : connectionList) {
      /* Are there uninitialized connections? */
      if (!connection.isInitialized()) {
        log(connection.getName() + " failed initialization", false);
      }
      /* Are there connections with a sample time below 1 second? */
      if (connection.getSampleTime() < 10) {
        /* Yes, there are. Set the timestep to 1 decisecond. */
        decisecondStep = true;
      }
    }
    
    /* Set the time step in deciseconds, its default value is 1 second. */
    final int timestep = (decisecondStep? 1 : 10);
    
    if (timer == null) {
      timer = Executors.newSingleThreadScheduledExecutor();
      
      timer.scheduleAtFixedRate(new Runnable() {
        @Override
        public void run() {
          internalCounter += timestep;
          /* CountDownLatch is used to synchronized the concurrent readings from
           * each connection.
           */
          CountDownLatch rlatch = new CountDownLatch(connectionList.size());
          /* Main reading cycle */
          for (final ConnectionManager connection : connectionList) {
            /* Start a new thread for each connection. */
            Thread t = new Thread(() -> {
              if ((internalCounter % connection.getSampleTime()) == 0 &&
                  !connection.isReaderListEmpty()) {
                if (connection.getStatus()) {
                  connection.read();

                  try {
                    Map<String, String> data = connection.getAllDataAsText();

                    String timestamp = 
                       TimeString.convertDateToString(connection.getTimestamp(),
                                                      dateFormat);
                    data.put(timestampS, timestamp);

                    addEntry(connection.getName(), data);
                  } catch (IllegalStateException ise) {
                    /* Datalogger cannot write data, i.e. cannot insert data 
                    * into a database.
                    * This could be due to some internal error of JIDL or it
                    * could be that the database server is unavailable or there
                    * is no more available space on disk etc.
                    * If it is not an internal error, it can be solved and the
                    * data logger started again.
                    * So we stop the data logging and throw the exception up.
                    */
                    Thread ct = Thread.currentThread();
                    if (inHandler != null) {
                      ct.setUncaughtExceptionHandler(inHandler);
                    }
                    ct.getUncaughtExceptionHandler().uncaughtException(ct, ise);
                    
                    stopLogging();
                  } catch (Exception e) {
                    connection.disconnect();
                    log(connection.getName() + ": " + e.getMessage(), false);
                  }
                } else {
                  if (connection.isInitialized()) {
                    try {
                      connection.connect();
                      log(connection.getName() + " connected", false);
                    } catch (IOException e) {
                      //TODO: deinitialize the connection?
                      log(connection.getName() + " cannot connect", false);
                    }
                  } else {
                    try {
                      connection.initialize();
                      log(connection.getName() + " initialized", false);
                    } catch (IllegalArgumentException e) {
                      //TODO: log the failure?
                    }
                  }
                }
              }
              rlatch.countDown();
            });
            t.start();
          }
          
          try {
            /* Wait here the completion of all reading threads. */
            rlatch.await();
          } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
          }
          
          /* Now let's do the writings. 
           * The writings are asynchronous.
           */
          for (final ConnectionManager connection : connectionList) {
            /* Start a new thread for each connection. */
            Thread t = new Thread(() -> {
              if ((internalCounter % connection.getSampleTime()) == 0 &&
                  !connection.isWriterListEmpty() && 
                  connection instanceof WriteableConnection) {
                if (connection.getStatus()) {
                  WriteableConnection wc = (WriteableConnection) connection;
                  wc.write();
                }
              }
            });
            t.start();
          }
        }
      /* Our basic unit is the decisecond. */
      }, (timestep * 100), (timestep * 100), MILLISECONDS);
    }
  }

  /**
   * Stops the IPC server.
   *
   * @throws RuntimeException if the server is not initialized
   */
  public void stopIPCServer() {
    if (ipcServer == null)
      throw new RuntimeException("DataLogger: stopIPCServer: ipcServer==null");
    
    ipcServer.stop();
  }
  
  /**
   * Stops the data logging.
   */
  public void stopLogging() {
    internalCounter = 0;
    
    // if the timer is running stop it
    if (timer != null) {
      /* If the timer is running, we are now stopping a running data logger.
       * Must log the event.
       */
      log(name + ": stopLogging()", false);
      
      timer.shutdown();
      try {
        if (!timer.awaitTermination(3, SECONDS)) {
          timer.shutdownNow();
        } 
      } catch (InterruptedException e) {
        timer.shutdownNow();
      }
      timer = null;
    }
    
    // disconnect all connections
    for (final ConnectionManager connection : connectionList) {
      connection.disconnect();
    }
  }
  
  /**
   * Returns the saved configuration of the database.
   *
   * @return a list of key-values pairs
   * @throws ExecutionException when cannot retrieve a configuration
   */
  public abstract List<Map<String, String>> getConfiguration()
    throws ExecutionException;
  
  /**
   * Adds a new {@link com.github.ilguido.jidl.connectionmanager.ConnectionManager}
   * object to the connection list.  It throws an exception if there is already
   * an object with the same name.
   *
   * @param inConnection a {@link com.github.ilguido.jidl.connectionmanager.ConnectionManager}
   *                     object
   * @throws IllegalArgumentException if a connection by the same name is
   *                                  already served by this logger
   */
  protected void addConnectionCommon(ConnectionManager inConnection)
  throws IllegalArgumentException {
  for (final ConnectionManager cm : connectionList) {
      if (cm.getName().equals(inConnection.getName()))
        throw new IllegalArgumentException("Duplicate connection name :" + 
                                           inConnection.getName());
    }

    connectionList.add(inConnection);
  }

  /**
   * Returns <code>timestampS</code>.
   *
   * @return <code>private final String timestampS</code>
   */
  protected String getTimestampS() {
    return timestampS;
  }
  
  /**
   * Prints out a diagnostic message.
   *
   * @param inMessage the message to print
   * @param inError <code>true</code> if <code>inMessage</code> is an error
   *                message
   */
  protected abstract void log(String inMessage, boolean inError);
  
  /**
   * Adds an entry to the database.  It adds an entry to the database for each
   * data point in <code>inData</code>.
   *
   * @param inTableName the name of the table where data are to be stored
   * @param inData a map of data points and their values
   */
  protected abstract void addEntry(String inTableName,
                                   Map<String, String> inData);
  
  /**
   * Searches and returns a {@link com.github.ilguido.jidl.connectionmanager.ConnectionManager}
   * object from the <code>connectionList</code> list, by its given name.
   * When there is not an object with that name, this function throws an
   * exception.
   *
   * @param inConnectionName the name of the required connection
   * @return the connection, if found
   * @throws IllegalArgumentException when no connection is found
   */
  public ConnectionManager getConnectionByName(String inConnectionName)
    throws IllegalArgumentException {
    for (final ConnectionManager connection : connectionList) {
      if (connection.getName().equals(inConnectionName)) {
        return connection;
      }
    }

    throw new IllegalArgumentException("No such connection: " +
                                       inConnectionName);
  }
}
