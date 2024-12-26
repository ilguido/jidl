/**
 * DataTypes.java
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

package com.github.ilguido.jidl;

/**
 * DataTypes
 * Container interface for some shared data
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public interface DataTypes {
  /**
   * An <code>enum</code> of the data types known to jidl.
   */
  enum DataType {
    BOOLEAN,
    INTEGER,
    DOUBLE_INTEGER,
    FLOAT,
    REAL,
    BYTE,
    WORD,
    DOUBLE_WORD,
    TEXT;

    /**
     * DataTypes can have variable size.
     */
    private Integer size = null;
    
    /**
     * Parses a string and returns the <code>DataType</code> corresponding to
     * that string.  E.g. if the string is <code>"BOOLEAN"</code>, the return
     * value is <code>DataType.BOOLEAN</code>. This function also sets the size
     * of the <code>DataType</code>, which can be specified between brackets.
     * E.g. if the string is <code>"TEXT(6)"</code>, the return value is
     * <code>DataType.TEXT</code> and the size is set to 6.
     *
     * @param inType the name of a <code>DataType</code> as a string
     * @return the <code>DataType</code> corresponding to the input string
     * @throws IllegalArgumentException when the name of the type is malformed
     */
    public static DataType valueOfDataType(String inType) 
      throws IllegalArgumentException {
      Integer newSize = null;
      int indexStartSize = inType.indexOf("(");
      
      if (indexStartSize > -1) {
        newSize = Integer.parseInt(inType.substring(indexStartSize + 1, 
                                   inType.indexOf(")")));
        inType = inType.substring(0, indexStartSize);
      }
      
      for (DataType dt : values()) {
        if (dt.toString().equals(inType)) {
            dt.setSize(newSize);
            return dt;
        }
      }
      throw new IllegalArgumentException("Unrecognized DataType");
    }
    
    /**
     * Returns the size of this <code>DataType</code>.
     *
     * @return the size of the <code>DataType</code> as a positive integer or
     *         <code>null</code>
     */
    public Integer getSize() {
      return size; 
    }
    
    /**
     * Sets the size of this <code>DataType</code>.  Size cannot be negative.
     *
     * @param inSize the requested size for this <code>DataType</code>
     * @throws IllegalArgumentException if inSize is negative
     */
    public void setSize(Integer inSize) throws IllegalArgumentException {
      if (inSize != null) {
        if (Integer.signum(inSize) < 0) {
          throw 
            new IllegalArgumentException("DataType can't have negative size");
        }
      }
      size = inSize; 
    }
    
    /**
     * Returns the SQLite type corresponding to this <code>DataType</code> as a
     * string.
     *
     * @return the name of a SQLite data type as a string
     */
    public String toSQLite() {
      switch(this) {
        case BOOLEAN:
          return "NUMERIC";
        case INTEGER:
        case DOUBLE_INTEGER:
        case BYTE:
        case WORD:
        case DOUBLE_WORD:
          return "INTEGER";
        case FLOAT:
        case REAL:
          return "REAL";
        case TEXT:
          return "TEXT";
        default:
          return null;
      }
    }
    
    /**
     * Throws an <code>IllegalArgumentException</code> if this is not a valid
     * <code>DataType</code>.
     *
     * @throws IllegalArgumentException if this is not a valid 
     *                                  <code>DataType</code>
     */
    public DataType validate() throws IllegalArgumentException {
      switch(this) {
        case BOOLEAN:
        case INTEGER:
        case DOUBLE_INTEGER:
        case BYTE:
        case WORD:
        case DOUBLE_WORD:
        case FLOAT:
        case REAL:
        case TEXT:
          return this;
        default:
          throw new IllegalArgumentException("Invalid DataType");
      }
    }
  };
}
