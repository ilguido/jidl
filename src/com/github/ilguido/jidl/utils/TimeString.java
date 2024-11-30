/**
 * TimeString.java
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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * TimeString
 * A class of static functions used to manipulate textual representations
 * of time and date.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class TimeString {
  /**
   * Converts a date to a string, according to the given format.
   *
   * @param inDate a <code>Date</code> object
   * @param inFormat the format of the date as a string
   * @return the input date converted into a text string
   */
  public static String convertDateToString(Date inDate, String inFormat) {
    DateFormat formatter = new SimpleDateFormat(inFormat);
    return formatter.format(inDate);
  }
  
  /**
   * Returns the current time as a string, according to the given format.
   *
   * @param inFormat the format of the date as a string
   * @return the input date converted into a text string
   */
  public static String getCurrentTimeAsString(String inFormat) {
    Date now = new Date();
    return convertDateToString(now, inFormat);
  }
  
  /**
   * Returns the actual date as a string of text. The format of the string is
   * fixed: "yyyy-MM-dd".
   *
   * @return today date converted into a text string
   */
  public static String getTodayDateS() {
    Date date = new Date() ;
    return convertDateToString(date, "yyyy-MM-dd");
  }
} 
