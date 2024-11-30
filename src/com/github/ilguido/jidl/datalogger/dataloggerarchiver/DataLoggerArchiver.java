/**
 * DataLoggerArchiver.java
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

package com.github.ilguido.jidl.datalogger.dataloggerarchiver;

/**
 * DataLoggerArchiver
 * Interface for managing the archival of data as backup files.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public interface DataLoggerArchiver {
  /**
   * Returns true if this data logger works with an embedded database.
   *
   * @return <code>true</code> if this object implements the archiver facility
   */
  boolean isArchiver();
  
  /**
   * Returns true if the archiviving service is set.
   *
   * @return <code>true</code> if this object is configured to archive its data
   */
  boolean isArchiverSet();
  
  /**
   * Sets the configuration and starts the archiving service.
   *
   * @param inDayOfWeek the day of week
   * @param inInterval the interval between archivals
   * @param inUseMonths whether the interval is days or months
   */
  void setArchivingService(int inDayOfWeek,
                           int inInterval,
                           boolean inUseMonths)
    throws IllegalArgumentException;
    
  /**
   * Stops the archiving service.
   */
  void stopArchivingService();
}
