/**
 * FileManager.java
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

package com.github.ilguido.jidl.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * FileManager
 * A utility class for the management of INI files.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */
 
public class FileManager {
  /** 
   * Pattern of a section.
   */
  private static Pattern  sectionP  = Pattern.compile("^\\s*\\[([^]]*)\\]\\s*");
  
  /** 
   * Pattern of an entry.
   */
  private static Pattern  keyValueP = Pattern.compile("^\\s*([^=]*)=(.*)");
  
  /**
   * Pattern of a comment.
   */
  private static Pattern commentP = Pattern.compile("^\\s*#.*");
  
  /**
   * Checks if a file exists.
   *
   * @param path the file path
   * @return a boolean value: true if the file exists, false otherwise
   */
  public static boolean checkExistence(String path) {
    File file = new File(path);

    if (file.exists())
      return true;

    return false;
  }

  /**
   * Returns the content of an INI data block.
   *
   * @param inData a string with the content of an INI file
   * @return a list of maps, each map is a key-value pair from the INI data
   * @throws IOException if there is an error reading the data
   */
  public static List<Map<String, String>> loadIniData(String inData)
    throws IOException {
    return loadIniCommon(new StringReader(inData));
  }

  /**
   * Returns the content of an INI file.
   *
   * @param path the path to the file
   * @return a list of maps, each map is a key-value pair from the INI file
   * @throws IOException if there is an error reading the file
   */
  public static List<Map<String, String>> loadIniFile(String path)
    throws IOException {
    return loadIniCommon(new FileReader(path));
  }

  /**
   * Renames a file.
   *
   * @param inName the name as a full path of the file to rename
   * @param inNewName the new name as a full path of the file
   * @throws IOException when the file does not exists, or the renaming failed
   */
  public static void renameFile(String inName, String inNewName)
    throws IOException {
    // File with old name
    File fileOld = new File(inName);

    // File with new name
    File fileNew = new File(inNewName);

    if (!fileOld.exists())
      throw new java.io.IOException(inName);
      // FIXME: what if newfile exists?

    // Rename file (or directory)
    boolean success = fileOld.renameTo(fileNew);

    if (!success) {
      // renameTo is notoriously unreliable,
      // so, in case of a failure,
      // let's just wait a few seconds and retry one more time
      try {
        TimeUnit.SECONDS.sleep(3);
      } catch (InterruptedException e) {
        ; // FIXME: unhandled exception
      }
      success = fileOld.renameTo(fileNew);

      if (!success)
        throw new java.io.IOException(inNewName);
    }
  }
  
  /**
   * Parses an INI text stream.  This function reads a stream of text and parses
   * it as it were a INI configuration file. Every section of the INI is stored
   * as the value of a <code>Map</code> object and the name, unique, of the
   * section is stored as the key of the <code>Map</code>.
   * There are two kinds of INI configuration for JIDL: data logger 
   * configuration, and connection configuration.
   * The first one should have only one or two sections: <code>datalogger</code>
   * and <code>dataarchiver</code> (optional).
   * The second one has a number of connection and variable sections: connection
   * sections are named <code>connection_name</code>, variable reader sections
   * are named <code>variable_reader_name::connection_name</code>, and variable
   * writer sections are named <code>variable_writer_name::connection_name<-
   * variable_reader_name::connection_name</code>.
   *
   * @param inReader the text stream as a <code>Reader</code> object
   * @return a list of maps, each map is a key-value pair from the INI data
   * @throws IOException if the <code>BufferedReader</code> fails
   */
  private static List<Map<String, String>> loadIniCommon(Reader inReader)
    throws IOException {
    LinkedList<Map<String, String>> sectionList = new LinkedList<>();

    // read the file
    try(BufferedReader br = new BufferedReader(inReader)) {
      String line;
      String section = null;
      Map<String, String> loggerSection = null;
      Map<String, String> kv = null;
      while ((line = br.readLine()) != null) {
        Matcher m = commentP.matcher(line);
        // if it is not a comment...
        if(!m.matches()) {
          m = sectionP.matcher(line);
          // it is a new section
          if( m.matches()) {
              section = m.group(1).trim();
              kv = null; // use a new key-value map for the new section
          } else if (section != null) {
            m = keyValueP.matcher(line);
            // it is a new key value pair
            if( m.matches()) {
              String key   = m.group(1).trim();
              String value = m.group(2).trim();

              if (kv == null) {
                kv = new HashMap<>();
                if (section.equals("datalogger")) {
                  sectionList.addFirst(kv);
                } else if (section.equals("dataarchiver")) {
                  sectionList.addLast(kv);
                } else if (section.indexOf("::") == -1) {
                  sectionList.addFirst(kv);
                } else {
                  sectionList.addLast(kv);
                }
                kv.put("section", section);
              }
              kv.put(key, value);
            }
          }
        }
      }
      if (loggerSection != null)
        sectionList.addFirst(loggerSection);
      /* Now the linked list is:
       * { connection_1, connection_2 ... connection_m, variable_1,
       *   variable_2, ... variable_n }
       * Or:
       * { datalogger, dataarchiver }
       */
    }
    return sectionList;
  }
}

