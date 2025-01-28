/**
 * SQLHeader.java
 *
 * Copyright (c) 2025 Stefano Guidoni
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

package com.github.ilguido.jidl.datalogger.sqlheader;

import com.github.ilguido.jidl.DataTypes;
import com.github.ilguido.jidl.utils.Validator;

/**
 * SQLHeader
 * A class for SQL headers.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class SQLHeader implements DataTypes {
  /**
   * The data type of the column.
   */
  private final DataType dataType;
  
  /**
   * The name of the column, that is the header.
   */
  private final String header;
  
  /**
   * Class constructor.
   *
   * @param inColumnName an SQL column name as a string
   * @param inDataType the data type from {@link DataTypes}
   * @throws IllegalArgumentException if the column name is not valid for an
   *                                  SQL column name
   */
  public SQLHeader(String inColumnName, DataType inDataType) {
    header = Validator.validateString(inColumnName);
    dataType = inDataType;
  }
  
  /**
   * Returns the column name.
   *
   * @return the column name as a string
   */
  public DataType getDataType() {
    return dataType;
  }
  
  /**
   * Returns the data type.
   *
   * @return the data type as defined in {@link DataTypes}
   */
  public String getHeader() {
    return header;
  }
}
