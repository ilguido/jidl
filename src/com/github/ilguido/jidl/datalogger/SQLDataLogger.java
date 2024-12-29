/**
 * SQLDataLogger.java
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

/**
 * SQLiteDataLogger
 * A class to store data in a SQLite database.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public abstract class SQLDataLogger extends DataLogger {
  
  /**
   * Name of the column where configuration information is stored.
   */
  protected final String configurationColumnName = "DATA";

  /**
   * Name of the table where the configuration is stored.
   */
  protected final String configurationTableName = "JIDL Configuration";
  
  /**
   * Connection to the database.
   */
  protected Connection connection = null;
  
  /**
   * The name of the database engine.
   */
  protected final String databaseEngineName;
  
  /**
   * Path to the database.
   */
  protected final String databasePath;
  
  /**
   * URL to the database.
   */
  protected final String databaseURL;

  /**
   * Name of the table to store diagnostics messages.
   */
  protected final String diagnosticsTableName = "JIDL Diagnostics";

  /**
   * Name of the column to store diagnostics messages.
   */
  protected final String diagnosticsColumnName = "MESSAGE";

  /**
   * Header of the table.
   */
  protected Map<String, ArrayList<String>> sqlHeaders;
  
  /**
   * The properties of the connection to the database.
   */
  private Properties connectionConfig;
  
  /**
   * Name of the JDBC class of this database.
   */
  private final String JDBCClassName;
  
  /**
   * Class constructor.  It calls the parent class constructor setting the
   * name and the working directory of the database. It sets the URL to the
   * database, the name of the JDBC class, and the name of the database engine.
   * If available, it sets the username and the password for the database
   * connection. It also tries to connect to the database.
   *
   * @param inName the mnemonic name of the database, must be a valid file name
   * @param inDir the path to the working directory of the database
   * @throws IllegalArgumentException if the path or the name are not valid, or
   *                                  if the driver for this database is not
   *                                  available
   * @throws ExecutionException if the database cannot be initialized or
   *                            connected to
   */
  public SQLDataLogger(String inName,
                       String inDir,
                       String inURLProtocol,
                       String inPath,
                       String inJDBC,
                       String inDatabaseEngineName,
                       String inUserName,
                       String inPassword)
    throws IllegalArgumentException, ExecutionException {
    super(inName, inDir);
    
    // initialization
    sqlHeaders = new HashMap<String, ArrayList<String>>();
    
    databasePath = inPath;
    databaseURL = inURLProtocol + databasePath;
    JDBCClassName = inJDBC;
    databaseEngineName = inDatabaseEngineName;
    
    // Set the connection properties.
    connectionConfig = new Properties();
    if (inUserName != null && inPassword != null) {
      connectionConfig.setProperty("user", inUserName);
      connectionConfig.setProperty("password", inPassword);
    }
    
    // try to connect to the database
    try {
      connection = DriverManager.getConnection(databaseURL, connectionConfig);
    } catch (SQLException e) {
      throw new ExecutionException("Cannot connect to " + databaseEngineName +
                                   " database", e);
    }
  }
  
  /**
   * Starts the data logging.  It throws an exception when it is not ready to
   * log data, e.g. when the driver to connect to a database is not initialized.
   *
   * @param inHandler a handler function to catch an exception that stops the
   *                  data logging, it can be used to make the user aware of the
   *                  stoppage
   * @throws ExecutionException if the {@link com.github.ilguido.jidl.datalogger.DataLogger} 
   *                            cannot complete a necessary task, e.g. connect 
   *                            to the database
   */
  @Override
  public void startLogging(Thread.UncaughtExceptionHandler inHandler) 
    throws ExecutionException {
    if (connection == null)
      try {
        connection = DriverManager.getConnection(databaseURL,connectionConfig);
        super.startLogging(inHandler);
      } catch (SQLException e) {
        throw new ExecutionException("Cannot initialize a valid connection to "+
                                     databaseEngineName, e);
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
   * @throws SQLException if the execution of the SQL query fails
   * @throws IllegalArgumentException if the driver for the database is not
   *                                  available
   */
  protected ArrayList<String> executeQueryStatement(String sqlStatement)
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
  protected Integer executeUpdateStatement(String sqlStatement)
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
      // this throws an exception if SQLite is unavailable
      Class.forName(JDBCClassName);
    } catch (ClassNotFoundException e) {
      stopLogging();
      throw new IllegalArgumentException(databaseEngineName + 
                                         " driver is not available", e);
    }
    
    stmt = connection.createStatement();

    retValue = f.go(sqlStatement, stmt);
    stmt.close();
    
    return retValue;
  }
}
