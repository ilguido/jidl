/**
 * MariaDBDataLogger.java
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

package com.github.ilguido.jidl.datalogger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.sql.*;

import com.github.ilguido.jidl.connectionmanager.ConnectionManager;
import com.github.ilguido.jidl.DataTypes;
import com.github.ilguido.jidl.utils.FileManager;
import com.github.ilguido.jidl.utils.TimeString;

/**
 * MariaDBDataLogger
 * A class to store data in a Maria DB database.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

 /* NOTE: for compatibility with MonetDB, which is case sensitive, column names
  *       are always converted to lower case.
  */
public class MariaDBDataLogger extends DataLogger {
  /**
   * URL to the database.
   */
  private final String dbURL;
  
  /**
   * The user name for the Maria DB database.
   */
  private final String userName;
  
  /**
   * The password for the Maria DB database.
   */
  private final String password;

  /**
   * Name of the column where configuration information is stored.
   */
  private final String configurationColumnName = "DATA";

  /**
   * Name of the table where the configuration is stored.
   */
  private final String configurationTableName = "JIDL Configuration";

  /**
   * Name of the table to store diagnostics messages.
   */
  private final String diagnosticsTableName = "JIDL Diagnostics";

  /**
   * Name of the column to store diagnostics messages.
   */
  private final String diagnosticsColumnName = "MESSAGE";
  
  /**
   * Connection to the database.
   */
  private Connection connection = null;
  
  /**
   * Header of the table.
   */
  private Map<String, ArrayList<String>> sqlHeaders;

  /**
   * Class constructor.  It calls the parent class constructor setting the
   * name and the working directory of the database.
   *
   * @param inName the mnemonic name of the database, must be a valid file name
   * @param inDir the path to the working directory of the database
   * @param inServer the server name or IP address
   * @param inPort the Maria DB port on the server
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
  public MariaDBDataLogger(String inName,
                           String inDir,
                           String inServer,
                           String inPort,
                           String inUserName,
                           String inPassword)
    throws IllegalArgumentException, ExecutionException {
    super(inName, inDir);

    dbURL = "jdbc:mariadb://" + inServer + ":" + inPort + "/" + inName;

    // initialization
    sqlHeaders = new HashMap<String, ArrayList<String>>();

    // try to connect to the database
    Properties connConfig = new Properties();
    connConfig.setProperty("user", inUserName);
    connConfig.setProperty("password", inPassword);
    try {
      connection = DriverManager.getConnection(dbURL, connConfig);
    } catch (SQLException e) {
      throw new ExecutionException("Cannot connect to MariaDB database", e);
    }
    
    userName = inUserName;
    password = inPassword;
    
    try {
      // if the db already exists, initialize sqlHeaders
      String query = "SHOW TABLES WHERE Tables_in_" +  inName +
                      " <> '" + diagnosticsTableName +"' AND Tables_in_" +
                      inName + " <> '" + configurationTableName +"';";

      ArrayList<String> tables = executeQueryStatement(query);

      for (String s : tables) {
        query = "SELECT column_name FROM information_schema.COLUMNS WHERE " +
                "TABLE_NAME='" + s + "' AND column_name <> '" + 
                getTimestampS().toLowerCase() + "'";

        ArrayList<String> headers = executeQueryStatement(query);
        headers.add(0, getTimestampS().toLowerCase()); // the first column
        
        sqlHeaders.put(s, headers);
      }
      
      log("Loading: " + dbURL, false);
    } catch (SQLException e) {
      throw new ExecutionException("Error retrieving configuration from " +
                                    "MariaDB database", e);
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
    String query = "SELECT `" + configurationColumnName.toLowerCase() + 
                   "` FROM `" + configurationTableName + "`;";

    try {
      ArrayList<String> data = executeQueryStatement(query);
      
      if (! data.isEmpty())
        map = FileManager.loadIniData(String.join("\n", data));
    } catch (Exception e) {
      throw new ExecutionException("Cannot get configuration from MariaDB " +
                                   "database", e);
    }
    
    return map;
  }

  /**
   * Starts the data logging.  It throws an exception, if it cannot get a valid
   * connection to the database.
   *
   * @throws ExecutionException if it cannot initialize a connection to the
   *                            database
   */
  @Override
  public void startLogging() throws ExecutionException {
    if (connection == null)
      try {
        connection = DriverManager.getConnection(dbURL, userName, password);
        super.startLogging();
      } catch (SQLException e) {
        throw new ExecutionException("Cannot initialize a valid connection to "+
                                     "MariaDB", e);
      }
  }
  
  /**
   * Stops the data logging.  Close the connection with the database.
   */
  @Override
  public void stopLogging() {
    super.stopLogging();
    
    if (connection != null) {
      try {
        connection.close();
      } catch (SQLException e) {
        e.printStackTrace();
      } finally {
        connection = null;
      }
    }
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
      sqlValues += "," + inData.get(headers.get(i));
      sqlHeader += ",`" + headers.get(i) + "`";
    }
    sqlHeader += ") ";
    sql = "INSERT INTO `" + inTableName + "` " + sqlHeader + sqlValues + ");";

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
      executeUpdateStatement("INSERT INTO `" + diagnosticsTableName + "` (`"+
                           getTimestampS().toLowerCase() + "`,`" + 
                           diagnosticsColumnName.toLowerCase() + "`) VALUES('"+
                           logTimestamp + "','" + inMessage + "');");
    } catch (SQLException e) {
      if (inError) {
        throw new IllegalStateException("Datalogger cannot insert data");
      }
    }
  }

  /**
   * ExecuteStatement
   * A common interface for the Java functions used to execute SQL statements.
   *
   * @version 0.8
   * @author Stefano Guidoni
   */
  private interface ExecuteStatement<A> {
    /**
     * Execute the statement <code>s</code> with the Statement object
     * <code>stmt</code>.
     *
     * @param s the SQL statement to execute
     * @param stmt a Statement object connected to a database
     * @return the return value of the actual function used to execute the
     *         statement
     * @throws SQLException if there is an error with the execution of the SQL
     *                      statement
     */
    public A go(String s, Statement stmt) throws SQLException;
  }
  
  
  /**
   * Executes an SQL query on the current database and returns an array list.
   *
   * @param sqlStatement the SQL statement to execute
   * @return the array list
   * @throws SQLException if the execution of the SQL statement fails
   * @throws IllegalArgumentException if the driver for the database is not
   *                                  available
   */
  private ArrayList<String> executeQueryStatement(String sqlStatement)
    throws SQLException, IllegalArgumentException {
    return executeStatement(sqlStatement,
                            new ExecuteStatement<ArrayList<String>>() {
                              public ArrayList<String> go(String s,
                                                          Statement stmt) 
                                throws SQLException {
                                ArrayList<String> al = new ArrayList<>();
                                ResultSet rs = stmt.executeQuery(s);
                                while (rs.next()) {
                                  al.add(rs.getString(1));
                                }
                                return al;
                              }; 
                            });
  }
  
  /**
   * Executes an SQL statement on the current database and returns the number
   * of affected rows.
   *
   * @param sqlStatement the SQL statement to execute
   * @return the number of affected rows
   * @throws SQLException if the execution of the SQL statement fails
   * @throws IllegalArgumentException if the driver for the database is not
   *                                  available
   */
  private Integer executeUpdateStatement(String sqlStatement)
    throws SQLException, IllegalArgumentException {
    return executeStatement(sqlStatement,
                            new ExecuteStatement<Integer>() {
                              public Integer go(String s, Statement stmt) 
                                throws SQLException {
                                return Integer.valueOf(stmt.executeUpdate(s));
                              };
                            });
  }
  
  /**
   * Executes an SQL statement on the current database.
   *
   * @param sqlStatement the SQL statement to execute
   * @param f the function used to execute the SQL statement
   * @return the return value of <code>f</code>
   * @throws SQLException if the execution of the SQL statement fails
   * @throws IllegalArgumentException if the driver for the database is not
   *                                  available
   */
  private <A> A executeStatement(String sqlStatement, ExecuteStatement<A> f)
    throws SQLException, IllegalArgumentException {
    Statement stmt = null;
    A retValue = null;

    try {
      // this throws an exception if a MariaDB driver is unavailable
      Class.forName("org.mariadb.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      stopLogging();
      throw new IllegalArgumentException("MariaDB driver is not available", e);
    }
    
    stmt = connection.createStatement();

    retValue = f.go(sqlStatement, stmt);
    stmt.close();
    
    return retValue;
  }
}