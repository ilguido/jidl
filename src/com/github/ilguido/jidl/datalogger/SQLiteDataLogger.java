/**
 * SQLiteDataLogger.java
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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.sql.SQLException;

import static java.util.concurrent.TimeUnit.HOURS;

import com.github.ilguido.jidl.connectionmanager.ConnectionManager;
import com.github.ilguido.jidl.datalogger.dataloggerarchiver.DataLoggerArchiverHelper;
import com.github.ilguido.jidl.DataTypes;
import com.github.ilguido.jidl.utils.CalendarUtilities;
import com.github.ilguido.jidl.utils.FileManager;
import com.github.ilguido.jidl.utils.TimeString;
import com.github.ilguido.jidl.utils.Validator;

/**
 * SQLiteDataLogger
 * A class to store data in a SQLite database.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class SQLiteDataLogger extends SQLDataLogger {
  /**
   * The internal archiver for logged data.
   */
  private static ScheduledExecutorService archiver = null;

  /**
   * Class constructor.  It calls the parent class constructor setting the
   * name and the working directory of the database.
   *
   * @param inName the mnemonic name of the database, must be a valid file name
   * @param inDir the path to the working directory of the database
   * @throws IllegalArgumentException if the path or the name are not valid, or
   *                                  if the driver for this database is not
   *                                  available
   * @throws ExecutionException if the database cannot be initialized or
   *                            connected to
   */
  public SQLiteDataLogger(String inName,
                          String inDir)
    throws IllegalArgumentException, ExecutionException {
    super(inName, inDir, 
          "jdbc:sqlite:", Paths.get(inDir, inName.concat(".db")).toString(),
          "org.sqlite.JDBC", "SQLite", null, null);

    // if the file already exists, store a message in the diagnostics
    if (FileManager.checkExistence(databasePath.toString())) {
      try {
        // if the db already exists, initialize sqlHeaders
        String query = "SELECT name FROM sqlite_schema WHERE " + 
                       "type='table' AND name NOT LIKE 'sqlite_%' " +
                       "AND name NOT LIKE '" + diagnosticsTableName +"' " +
                       "AND name NOT LIKE '" + configurationTableName +"';";

        ArrayList<String> tables = executeQueryStatement(query);

        for (String s : tables) {
          query = "SELECT name FROM pragma_table_info('" + s + "') " +
                  "WHERE name NOT LIKE '" + getTimestampS() + "' ORDER BY cid;";

          ArrayList<String> headers = executeQueryStatement(query);
          headers.add(0, getTimestampS()); // the first column
          
          sqlHeaders.put(s, headers);
        }
        
        log("Loading: " + databaseURL, false);
      } catch (SQLException e) {
        throw new ExecutionException("Error retrieving configuration from " +
                                     "SQLite database", e);
      }
    } else {
      throw new IllegalArgumentException("Cannot find SQLite database");
    }
  }

  /**
   * Returns the configuration of the database.
   *
   * @return a list of key-value pairs or <code>null</code> if there is not a
   *         saved configuration to retrieve
   * @throws ExecutionException when cannot retrieve a configuration from the DB
   */
  @Override
  public List<Map<String, String>> getConfiguration()
    throws ExecutionException {
    List<Map<String, String>> map = null;
    String query = "SELECT " + configurationColumnName + " FROM '" + 
                   configurationTableName + "';";

    try {
      ArrayList<String> data = executeQueryStatement(query);
      
      if (! data.isEmpty())
        map = FileManager.loadIniData(String.join("\n", data));
    } catch (Exception e) {
      throw new ExecutionException("Cannot get configuration from SQLite " +
                                   "database", e);
    }
    
    return map;
  }
  
  /**
   * Returns true if this data logger works with an embedded database.
   *
   * @return <code>true</code> if this object implements the archiver facility
   */
  @Override
  public boolean isArchiver() {
    return true;
  }
  
  /**
   * Returns true if the archiving service is set.
   *
   * @return <code>true</code> if this object is configured to archive its data
   */
  @Override
  public boolean isArchiverSet() {
    return (archiver != null);
  }
  
  /**
    * ArchiverR
    * Private class that implements the Runnable interface, its run method do
    * the actual backup operation.
    *
    * @version 0.8
    * @author Stefano Guidoni
    */
  private class ArchiverR implements Runnable {
    /**
     * A boolean value which is <code>true</code> when backup segments are 
     * monthly.
     */
    private final boolean monthly;
    
    /**
     * The number of segments, weeks or months, to keep in the database after
     * cleaning up the old data.  Currently is set to 1.
     */
    private final int segmentSpan;
    
    /**
     * Class contructor.
     *
     * @param inMonthly set to <code>true</code> for monthly backups
     */
    public ArchiverR(boolean inMonthly) {
      monthly = inMonthly;
      segmentSpan = 1;
    }
    
    /**
     * When an object implementing interface Runnable is used to create a thread
     * starting the thread causes the object's run method to be called in that
     * separately executing thread.
     */
    @Override
    public void run() {
      // default value: 7, one week
      int daysToBackup = 7;
      
      /* If the backup is monthly, do it only on the first week of the month.
       */
      if (monthly)  {
        String today = TimeString.getTodayDateS().substring(8,2);
        if (Integer.parseInt(today) > 7)
          return;
        
        daysToBackup = 30 + Integer.parseInt(today); //FIXME: hackish
      }
      
      Set<String> tables = sqlHeaders.keySet();
      
      //backup old data
      String sql;
      sql = "backup to " + databasePath + "-" + TimeString.getTodayDateS() + 
            ".db";
      
      try {
        executeUpdateStatement(sql);
        
        //LOG
        log("backup data from: " + databasePath, false);
      } catch (SQLException e) {
        log("Failed data backup: " + sql, true);
      }
      
      tables.add(diagnosticsTableName);
      
      //delete old data
      for (final String i : tables.toArray(new String[0])) {
        sql = "DELETE FROM '" + i + "' WHERE " + getTimestampS() + 
              "< date('now', '-" + daysToBackup + " days');";
        
        try {
          executeUpdateStatement(sql);
          
          //LOG
          log("deleted old data from: " + i, false);
        } catch (SQLException e) {
          log("Failed data deletion: " + sql, true);
        }
      }
    }
  }
  
  /**
   * Sets the configuration and starts the archiving service.  On the chosen day
   * of week, on the first hour of the day, the executor service calls a backup
   * function. When the backup is monthly, it is triggered on the first 
   * occurence of that day of week that month. E.g., if the chosen day of week
   * is monday and the interval is monthly, the executor starts the backup on
   * the first monday of each month.
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
    //WARNING: this overwrites previous calls        
    archiver = DataLoggerArchiverHelper.setArchivingService(inDayOfWeek,
                                                            inInterval,
                                                            inUseMonths,
                                                            new ArchiverR(
                                                                  inUseMonths));
  }
  
  /**
   * Stops the archiving service.
   */
  @Override
  public void stopArchivingService() {
    DataLoggerArchiverHelper.stopArchivingService(archiver);
  }

  /**
   * Inserts a new row into a table.  It adds a new row to a target table of the
   * database.
   *
   * @param inTableName the name of the target table
   * @param inData a map of datapoints
   */
  @Override
  protected void addEntry(String inTableName,
                          Map<String, String> inData) {
    String sql;
    ArrayList<String> headers = sqlHeaders.get(inTableName);

    // build the VALUES and HEADER part of the SQL statement
    // the first element is always the timestamp and it is always there
    String sqlValues =  "VALUES ('" + inData.get(getTimestampS()) + "'";
    String sqlHeader = "(" + getTimestampS();

    // try to read all the other data
    for (int i = 1; i < headers.size(); i++) {
      /* Let's insert a value only if it is not null.
       * SQLite automatically sets to null column values which are not
       * inserted, when adding a new row.
       */
      if (inData.get(headers.get(i)) != null) {
        sqlValues += "," + inData.get(headers.get(i));
        sqlHeader += ",'" + headers.get(i) + "'";
      }
    }
    sqlHeader += ") ";
    sql = "INSERT INTO '" +  inTableName + "' " + sqlHeader + sqlValues + ");";

    try {
      executeUpdateStatement(sql);
    } catch (SQLException e) {
      log("Failed addEntry: " + sql, true);
    }
  }

  /**
   * Adds a message to the diagnostics log.
   *
   * @param inMessage the message for the log
   * @param inError <code>true</code> if <code>inMessage</code> is an error
   *                message
   * @throws IllegalStateException when cannot insert data into the database
   */
  @Override
  protected void log(String inMessage, boolean inError)
    throws IllegalStateException {
    String logTimestamp = TimeString.getCurrentTimeAsString(getDateFormat());
    
    if (inError)
      inMessage = "[E] " + inMessage.replace("'", "");

    try {
      executeUpdateStatement("INSERT INTO '" + diagnosticsTableName + "' ('" +
                            getTimestampS() + "','" + diagnosticsColumnName + 
                            "') VALUES('" + logTimestamp + "','" + 
                            inMessage + "');");
    } catch (SQLException e) {
      if (inError)
        throw new IllegalStateException("Datalogger cannot insert data");
    }
  }
}
