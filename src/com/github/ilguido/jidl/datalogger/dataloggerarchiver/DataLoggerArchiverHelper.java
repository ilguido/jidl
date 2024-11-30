/**
 * DataLoggerArchiverHelper.java
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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.concurrent.TimeUnit.HOURS;

import com.github.ilguido.jidl.utils.CalendarUtilities;
import com.github.ilguido.jidl.utils.Validator;

/**
 * DataLoggerArchiverHelper
 * Interface for managing the archival of data as backup files.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class DataLoggerArchiverHelper {
  /**
   * The number of hours in one week.
   */
  private static long HOURS_PER_WEEK = 168;
  
  /**
   * Sets the configuration and starts the archiving service.  On the chosen day
   * of week, on the first hour of the day, the executor service calls a backup
   * function. When the backup is monthly, it is triggered on the first 
   * occurence of that day of week that month. E.g., if the chosen day of week
   * is monday and the interval is monthly, the executor starts the backup on
   * the first monday of each month.
   *
   * @param inDayOfWeek the day of the week for the backup, as an integer
   *                    between 1 and 7
   * @param inInterval weeks or months between a backup and the next, 1 means
   *                   every week or month
   * @param inUseMonths when this is true, the interval is the number of months
   *                    between a backup and the next
   * @param inRunnable a runnable object that executes the backup
   * @return the scheduler for the archiving service
   * @throws IllegalArgumentException when some parameters is out of range
   */
  public static ScheduledExecutorService setArchivingService(int inDayOfWeek,
                                                      int inInterval,
                                                      boolean inUseMonths,
                                                      Runnable inRunnable)
    throws IllegalArgumentException {
    int dayOfWeek = Validator.validateRange(inDayOfWeek, 1, 7);
      
    // Set the scheduler
    ScheduledExecutorService archivingService = 
                                  Executors.newSingleThreadScheduledExecutor();
    
    // The maximum interval is one year
    int maxRange = inUseMonths ? 12 : 52;
    int interval = Validator.validateRange(inInterval, 1, maxRange);
    
    // When using months, we need to check if this is the first week of the
    // month, every week
    int weekInterval = inUseMonths ? 1 : interval;
    
    // Compute the next day for archiving
    int today = CalendarUtilities.getTodayWeekDay();
    int daysToNextArchiving = 0;
    int weeksToNextArchiving = 0;
    
    if (dayOfWeek <= today) {
        weeksToNextArchiving += 1;
    }
    
    if (inUseMonths) {
      weeksToNextArchiving += CalendarUtilities.getWeeksToNextMonth() - 1;
    }
    
    daysToNextArchiving = 7 * weeksToNextArchiving + dayOfWeek - today;

    int hourOfDay = CalendarUtilities.getHourOfDay();
    
    // The next archiving of logged data will start at the first hour of the
    // next chosen day
    long startDelay = 24 * daysToNextArchiving - hourOfDay;
    
    archivingService.scheduleAtFixedRate(inRunnable,
                                         startDelay,
                                         interval * HOURS_PER_WEEK,
                                         HOURS);
                                        
    return archivingService;
  }
  
  /**
   * Stops the archiving service.
   *
   * @param ses the scheduler of the archiving service
   */
  public static void stopArchivingService(ScheduledExecutorService ses) {
    if (ses != null) {
      try {
        if (!ses.awaitTermination(5, SECONDS)) {
          ses.shutdownNow();
        } 
      } catch (InterruptedException e) {
        ses.shutdownNow();
      }
    }
  }
}
