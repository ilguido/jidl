/**
 * Validator.java
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

package com.github.ilguido.jidl.utils;

/**
 * Validator
 * A class of static functions used to validate data.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class Validator {

  /**
   * Validates an IP address.
   *
   * @param inIP the IP address to validate
   * @return the validated IP address
   * @throws IllegalArgumentException if the IP address is not valid
   */
  public static String validateIPAddress(String inIP)
    throws IllegalArgumentException {
    if (inIP == null)
      throw new IllegalArgumentException("String is null");
      
    if (inIP.matches("^\\d+\\.\\d+\\.\\d+\\.\\d+$")) {
      String[] numbers = inIP.split("\\.");

      for (int i = 0; i < 4; i++) {
        if (Integer.parseInt(numbers[i]) > 255 ||
            Integer.parseInt(numbers[i]) < 0)
          throw new IllegalArgumentException("Illegal IP address: " + inIP);
      }
    } else {
      throw new IllegalArgumentException("Illegal IP address: " + inIP);
    }

    return inIP;
  }

  /**
   * Validates that an integer number is inside a given range.
   *
   * @param inNumber the number to validate
   * @param inMin the lower bound for the number
   * @param inMax the upper bound for the number
   * @return the validated number
   * @throws IllegalArgumentException if the number is not the given range
   */
  public static int validateRange(int inNumber, int inMin, int inMax)
    throws IllegalArgumentException {
    if (inNumber < inMin ||
        inNumber > inMax)
      throw new IllegalArgumentException("Number out of range: " + inNumber);

    return inNumber;
  }
  
  /**
   * Validates an input string according to the rules of Jidl.  A string must be
   * a valid SQL variable name.
   *
   * @param inString the string to validate
   * @return the validated string
   * @throws IllegalArgumentException if the string contains illegal characters
   */
  public static String validateString(String inString)
    throws IllegalArgumentException {
    if (inString == null)
      throw new IllegalArgumentException("String is null");
      
    if (!inString.matches("^[a-zA-Z_][a-zA-Z0-9_]*$"))
      throw new IllegalArgumentException("Illegal string: " + inString);
    
    return inString;
  }
}
