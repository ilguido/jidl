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
    TEXT;

    public String toSQLite() {
      switch(this) {
        case BOOLEAN:
          return "NUMERIC";
        case INTEGER:
        case DOUBLE_INTEGER:
        case BYTE:
        case WORD:
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
    
    public DataType validate() throws IllegalArgumentException {
      switch(this) {
        case BOOLEAN:
        case INTEGER:
        case DOUBLE_INTEGER:
        case BYTE:
        case WORD:
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
