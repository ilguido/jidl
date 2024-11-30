/**
 * DummyDataLogger.java
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.github.ilguido.jidl.DataTypes;
import com.github.ilguido.jidl.utils.FileManager;

/**
 * DummyDataLogger
 * A class to test the logging activity.  This class prints to the standard
 * output the data it gets.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class DummyDataLogger extends DataLogger {
  /**
   * Path to the configuration file.
   */
  private final Path filepath;
  
  /**
   * Class constructor.  It calls the parent class constructor setting the
   * name and the working directory of the logger.
   *
   * @param inName the mnemonic name of the logger, must be a valid file name
   * @param inDir the path to the working directory of the logger; the logger
   *              loads its configuration from the file
   *              <code>inDir/inName[.cfg|.ini|]</code>, if it exists
   * @throws IllegalArgumentException if the path or name are not valid
   */
  public DummyDataLogger(String inName, String inDir) {
    super(inName, inDir);
    
    // read the configuration from a file called <inName>.ini
    String filename = inName.concat(".ini");
    filepath = Paths.get(inDir, filename);
  }

  /**
   * Returns the configuration of the database.
   *
   * @return a list of key-values pairs
   * @throws ExecutionException when cannot retrieve a configuration from the DB
   */
  @Override
  public List<Map<String, String>> getConfiguration()
    throws ExecutionException  {
    List<Map<String, String>> map = null;
    
    try {
      map = FileManager.loadIniFile(filepath.toString());
    } catch (Exception e) {
      throw new ExecutionException(filepath.toString(), e);
    }
    
    return map;
  }
  
  /**
   * Prints out a diagnostic message.
   *
   * @param inMessage the message to print
   * @param inError <code>true</code> if <code>inMessage</code> is an error
   *                message
   */
  @Override
  protected void log(String inMessage, boolean inError) {
    if (inError)
      System.err.println(inMessage);
    else
      System.out.println(inMessage);
  }
  
  /**
   * Prints a row of data.  It prints the table name and the feeded data.
   *
   * @param inTableName the name of the table where data are to be stored
   * @param inData a map of data points and their values
   */
  @Override
  protected void addEntry(String inTableName, Map<String, String> inData) {
    System.out.println("DummyDataLogger.addEntry()");
    System.out.println(inTableName);
    System.out.println(inData);
  }

  /**
   * Returns the path of the configuration file as a string.  It actually checks
   * the existence of a file with the right extension in the working directory
   * and returns its path. If no file is found, it returns <code>null</code>.
   *
   * @param inName the mnemonic name of the logger, it is also the file name
                   of the configuration file
   * @param inDir the path to the working directory of the logger, where the
   *              configuration file is stored
   * @param extensionA an array of file extensions for the configuration file
   * @return the path to the file or <code>null</code> if it does not exist
   */
  private String getConfigurationFileName(String inDir,
                                          String inName,
                                          String... extensionA) {
    for (int i = 0; i < extensionA.length; i++) {
      String filename = inName.concat(extensionA[i]);
      Path filepath = Paths.get(inDir, filename);
      
      if (FileManager.checkExistence(filepath.toString()))
        return filepath.toString();
    }
    
    return null;
  }
}
