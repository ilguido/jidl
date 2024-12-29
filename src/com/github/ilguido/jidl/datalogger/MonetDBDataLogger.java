/**
 * MonetDBDataLogger.java
 *
 * Copyright (c) 2023 Stefano Guidoni
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
import java.util.concurrent.ExecutionException;
import java.sql.SQLException;

import com.github.ilguido.jidl.connectionmanager.ConnectionManager;
import com.github.ilguido.jidl.DataTypes;
import com.github.ilguido.jidl.utils.FileManager;
import com.github.ilguido.jidl.utils.TimeString;

/**
 * MonetDBDataLogger
 * A class to store data in a Monet DB database.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

 /* NOTE: MonetDB is case sensitive and column names are always converted to
  *       lower case.
  */
public class MonetDBDataLogger extends SQLDataLogger {
  /**
   * Class constructor.  It calls the parent class constructor setting the
   * name and the working directory of the database.
   *
   * @param inName the mnemonic name of the database, must be a valid file name
   * @param inDir the path to the working directory of the database
   * @param inServer the server name or IP address
   * @param inPort the Monet DB port on the server
   * @param inUserName the user name to log in to the server
   * @param inPassword the password to log in to the server
   * @throws IllegalArgumentException if the path or the name are not valid, or
   *                                  if the driver for this database is not
   *                                  available
   * @throws ExecutionException if a connection to the database cannot be 
   *                            established, or if the database cannot be
   *                            initialized, or, in short, if the object
   *                            construction fails
   */
  public MonetDBDataLogger(String inName,
                           String inDir,
                           String inServer,
                           String inPort,
                           String inUserName,
                           String inPassword)
    throws IllegalArgumentException, ExecutionException {
    super(inName, inDir, 
          "jdbc:monetdb:", "//" + inServer + ":" + inPort + "/" + inName,
          "org.monetdb.jdbc.MonetDriver", "MonetDB",
          inUserName, inPassword);

    try {
      // if the db already exists, initialize sqlHeaders
      String query = "SELECT name FROM sys.tables WHERE NOT system " + 
                      "AND name <> '" + diagnosticsTableName +"' " +
                      "AND name <> '" + configurationTableName +"';";

      ArrayList<String> tables = executeQueryStatement(query);

      for (String s : tables) {
        query = "SELECT column FROM sys.statistics('" + inName + "') " +
                "WHERE column <> '" + getTimestampS().toLowerCase() + 
                "' AND table = '" + s + "' ORDER BY column_id;";

        ArrayList<String> headers = executeQueryStatement(query);
        headers.add(0, getTimestampS().toLowerCase()); // the first column
        
        sqlHeaders.put(s, headers);
      }
      
      log("Loading: " + databaseURL, false);
    } catch (SQLException e) {
      throw new ExecutionException("Error retrieving configuration from " +
                                    "MonetDB database", e);
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
    String query = "SELECT \"" + configurationColumnName.toLowerCase() + 
                   "\" FROM \"" + configurationTableName + "\";";

    try {
      ArrayList<String> data = executeQueryStatement(query);
      
      if (! data.isEmpty())
        map = FileManager.loadIniData(String.join("\n", data));
    } catch (Exception e) {
      throw new ExecutionException("Cannot get configuration from MonetDB " +
                                   "database", e);
    }
    
    return map;
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
    String sqlValues = "VALUES ('" + inData.get(getTimestampS()) + "'";
    String sqlHeader = "(" + getTimestampS().toLowerCase();

    // try to read all the other data
    for (int i = 1; i < headers.size(); i++) {
      /* Let's insert a value only if it is not null.
       * MariaDB automatically sets to null column values which are not
       * inserted, when adding a new row.
       */
      if (inData.get(headers.get(i)) != null) {
        sqlValues += "," + inData.get(headers.get(i));
        sqlHeader += ",\"" + headers.get(i) + "\"";
      }
    }
    sqlHeader += ") ";
    sql = "INSERT INTO \"" + inTableName + "\" " + sqlHeader + sqlValues + ");";

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
      executeUpdateStatement("INSERT INTO \"" + diagnosticsTableName + "\" (\""+
                           getTimestampS().toLowerCase() + "\",\"" + 
                           diagnosticsColumnName.toLowerCase() + "\") VALUES('"+
                           logTimestamp + "','" + inMessage + "');");
    } catch (SQLException e) {
      if (inError) {
        throw new IllegalStateException("Datalogger cannot insert data");
      }
    }
  }
}
