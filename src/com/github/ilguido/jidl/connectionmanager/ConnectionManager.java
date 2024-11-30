/**
 * ConnectionManager.java
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

package com.github.ilguido.jidl.connectionmanager;

import java.io.IOException;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import javax.management.AttributeNotFoundException;

import com.github.ilguido.jidl.DataTypes;
import com.github.ilguido.jidl.utils.Validator;
import com.github.ilguido.jidl.variable.Variable;
import com.github.ilguido.jidl.variable.VariableReader;
import com.github.ilguido.jidl.variable.VariableWriter;

/**
 * ConnectionManager
 * Superclass to manage a connection to an industrial device.  Each connection
 * allows polling an industrial device and reading a number of variables from
 * that device.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public abstract class ConnectionManager implements DataTypes {
  /**
   * Status of the connection.  It is false if the connection has not been
   * established or failed.
   */
  protected boolean status;

  /**
   * List of variables that can be read through this connection.  It is a list
   * of {@link com.github.ilguido.jidl.variable.VariableReader} objects.
   */
  protected ArrayList<VariableReader> variableReaderList;

  /**
   * List of variables that can be written to through this connection.  It is a
   * list of {@link com.github.ilguido.jidl.variable.VariableWriter} objects.
   */
  protected ArrayList<VariableWriter> variableWriterList;
  
  /**
   * The connection address.
   */
  private final String address;
  
  /**
   * Mnemonic name of the connection.
   */
  private final String name;

  /**
   * Sample time in seconds.  It is the time interval between readings and its
   * default value is 1 second.
   */
  private int sampleTime = 1;

  /**
   * Timestamp of the last reading.
   */
  private Date timestamp = null;
  
  /**
   * Type of the connection.  This must be set by the constructor of the child
   * class.
   */
  private final String type;
  
  /**
   * Class constructor.  It sets the name of the connection and initializes the
   * lists of variables.
   *
   * @param inName the mnemonic name of the connection
   * @param inType the mnemonic name of the type of the connection
   * @param inAddress the address of the connection
   * @throws IllegalArgumentException if <code>inName</code> is not a valid name
   */
  public ConnectionManager(String inName, String inType, String inAddress)
    throws IllegalArgumentException {
    status = false;
    name = Validator.validateString(inName);
    type = inType;
    address = inAddress;

    variableReaderList = new ArrayList<VariableReader>();
    variableWriterList = new ArrayList<VariableWriter>();
  }

  /**
   * Returns the address of the connection.
   *
   * @return the address of the connection as a <code>String</code> object
   */
  public String getAddress() {
    return address;
  }
  
  /**
   * Returns a map with each variable name as key and its value as value. Each
   * item of the map is a pair consisting of the name of a variable, as a
   * string, and its value converted into a text string.
   *
   * @return a map of name-value pairs
   */
  public Map<String, String> getAllDataAsText() {
    Map<String, String> map = new HashMap<>();

    for (final Variable v : variableReaderList) {
      map.put(v.getName(), v.toString());
    }

    return map;
  }

  /**
   * Returns the name of the connection.
   *
   * @return the mnemonic name of the connection
   */
  public String getName() {
    return name;
  }
  
  /**
   * Returns the requested parameter as a generic object.
   *
   * @param inParName the name of the requested parameter
   * @return the value of the requested parameter as a generic object
   * @throws IllegalArgumentException when the argument is null
   * @throws AttributeNotFoundException if the parameter is not part of this
   *                                    class
   */
  public Object getParameterByName(String inParName) 
    throws AttributeNotFoundException,
           IllegalArgumentException {
    // Check if inParName is null
    if (inParName == null) {
        throw new IllegalArgumentException("Parameter name cannot be null");
    }
    
    // Check parameter names one by one and return the right getter
    if (inParName.equals("name")) {
      return getName();
    } else if (inParName.equals("sample time")) {
      return getSampleTime();
    } else if (inParName.equals("type")) {
      return getType();
    }
  
    // If attribute_name has not been recognized
    throw new AttributeNotFoundException("Unknown parameter name: "+ inParName);
  }

  /**
   * Returns the string array of the names of the parameters.
   *
   * @return string array of parameter names
   */
  public String[] getParameterNames() {
    return new String[]{"name", "sample time", "type"};
  }

  /**
   * Returns the sample time.
   *
   * @return the sample time in seconds
   */
  public Integer getSampleTime() {
    return Integer.valueOf(sampleTime);
  }

  /**
   * Returns the status of the connection.
   *
   * @return <code>true</code> if the connection is established,
   *         <code>false</code> otherwise
   */
  public boolean getStatus() {
    return status;
  }
  
  /**
   * Returns the timestamp of the last reading.
   *
   * @return the timestamp as a date
   * @throws IllegalStateException if the first reading did not happen yet
   */
  public Date getTimestamp()
    throws IllegalStateException {
    if (timestamp == null)
      throw new IllegalStateException("No timestamp available");

    return timestamp;
  }
  
  /**
   * Returns the type of the connection.
   *
   * @return the type as a string
   */
  public String getType() {
    return type;
  }

  /**
   * Returns the list of the names of the variable readers.
   *
   * @return an arraylist of strings, the list of the names of the variables
   */
  public ArrayList<String> getVariableReaderNamesList() {
    return getVariableNamesList(variableReaderList);
  }

  /**
   * Returns the list of the names of the variable writers.
   *
   * @return an arraylist of strings, the list of the names of the variables
   */
  public ArrayList<String> getVariableWriterNamesList() {
    return getVariableNamesList(variableWriterList);
  }
  
  /**
   * Returns the type of the variable by the specified name.
   *
   * @param inName the name of the variable
   * @return the data type of the variable as defined in {@link com.github.ilguido.jidl.DataTypes}
   * @throws NoSuchElementException if a variable by the name of
   *                                <code>inName</code> does not exist
   */
  public DataType getVariableTypeByName(String inName)
    throws NoSuchElementException {
    Variable v = getVariableReaderByName(inName);

    return v.getType();
  }
  
  /**
   * Returns <code>true</code> if the variable reader list is empty.
   *
   * @return <code>true</code> if the variable reader list is empty,
   *         <code>false</code> otherwise
   */
  public boolean isReaderListEmpty() {
    return (variableReaderList.size() == 0);
  }

  /**
   * Returns <code>true</code> if the variable writer list is empty.
   *
   * @return <code>true</code> if the variable writer list is empty,
   *         <code>false</code> otherwise
   */
  public boolean isWriterListEmpty() {
    return (variableWriterList.size() == 0);
  }
  
  /**
   * Set the sample time.
   *
   * @param inSeconds time interval between readings, in seconds
   * @throws IllegalArgumentException if the <code>inSeconds</code> is zero or
   *                                  a negative number
   */
  public void setSampleTime(int inSeconds)
    throws IllegalArgumentException {
    if (inSeconds < 1)
      throw new IllegalArgumentException("Sample time must be a positive int");

    sampleTime = inSeconds;
  }
  
  /**
   * Adds a new {@link com.github.ilguido.jidl.variable.VariableReader} object to the
   * connection. Each connection is a link to a number of variables of the
   * remote device, stored in a list.
   *
   * @param inName the mnemonic name of the variable
   * @param inAddress the address of the variable, its format depends on the
   *                  connection
   * @param inType the type of the variable as defined in {@link com.github.ilguido.jidl.DataTypes}
   * @throws IllegalArgumentException if the <code>inType</code> is an invalid
   *                                  type
   * @return the created {@link com.github.ilguido.jidl.variable.VariableReader} object
   */
  public abstract VariableReader addVariableReader(String inName, 
                                                   String inAddress,
                                                   DataType inType)
    throws IllegalArgumentException;
  
  /**
   * Establishes a connection to the industrial device.
   *
   * @throws IOException if the connection cannot be established
   */
  public abstract void connect() throws IOException;
  
  /**
   * Closes the connection to the industrial device.
   */
  public abstract void disconnect();
  
  /**
   * Returns the object used for connecting to the industrial device.
   *
   * @return an object or null if there is none
   */
  public abstract Object getClient();
  
  /**
   * Initializes the connection.
   *
   * @throws IllegalArgumentException if something goes wrong
   */
  public abstract void initialize() throws IllegalArgumentException;
    
  /**
   * Returns <code>true</code> if the connection is correctly initialized.
   *
   * @return <code>true</code> if the member <code>client</code> is initialized,
   *         <code>false</code> otherwise
   */
  public abstract boolean isInitialized();
  
  /**
   * Reads all the variables listed in this connection and returns itself.
   *
   * @return this {@link com.github.ilguido.jidl.connectionmanager.ConnectionManager} object
   */
  public abstract ConnectionManager read();
    
  /**
   * Updates the timestamp to now.  The <code>timestamp</code> property is set
   * to now.
   */
  protected void updateTimestamp() {
    Date now = new Date();
    timestamp = now;
  }

  /**
   * Returns the variable specified by its name.
   *
   * @param inName the name of an existing variable
   * @param inList the list where to search
   * @return a {@link com.github.ilguido.jidl.variable.Variable} object
   * @throws NoSuchElementException if no variable is found by the given name
   */
  private Variable getVariableByName(String inName, 
                                     ArrayList<? extends Variable> inList)
    throws NoSuchElementException {
    for (Variable v : inList) {
      if (inName.equals(v.getName()))
        return v;
    }

    throw new NoSuchElementException("Variable \"" + inName + 
                                     "\" not found in connection: " + 
                                     getName());
  }
  
  /**
   * Returns the variable specified by its name from the list of variable
   * readers.
   *
   * @param inName the name of an existing variable
   * @return a {@link com.github.ilguido.jidl.variable.Variable} object
   * @throws NoSuchElementException if no variable is found by the given name
   */
  private Variable getVariableReaderByName(String inName)
    throws NoSuchElementException {
    return getVariableByName(inName, variableReaderList);
  }
    
  /**
   * Returns the list of the names of the variables from a given list.
   *
   * @param inList the list of the variable
   * @return an arraylist of strings, the list of the names of the variables
   */
  private ArrayList<String>
                    getVariableNamesList(ArrayList<? extends Variable> inList) {
    ArrayList<String> names = new ArrayList<String>();

    for (final Variable v : inList) {
      names.add(v.getName());
    }

    return names;
  }
}
