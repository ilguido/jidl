/**
 * CalendarUtilities.java
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

import java.time.LocalDate;
import java.util.Calendar;

/**
 * CalendarUtilities
 * A class of static functions used to work with calendar days.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class CalendarUtilities {
  /**
   * Returns the day of the month as a number.
   *
   * @return the day of month as a <code>int</code> 1 to 31
   */
  public static int getDayOfMonth() {
    return Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
  }
  
  /**
   * Returns the day of the week as a number.
   *
   * @return the day of week as a <code>int</code> 1 to 7
   */
  public static int getTodayWeekDay() {
    return LocalDate.now().getDayOfWeek().getValue();
  }
  
  /**
   * Returns the curret hour, local time.
   *
   * @return the current hour as a <code>int</code> 0 to 23
   */
  public static int getHourOfDay() {
    return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
  }
  
  /**
   * Returns the number of weeks to the next month.  This function is used by
   * the archiver, which makes a backup copy of the logged data on a weekly
   * basis. If the archiver is set to make a backup every month, it really skips
   * a number of weeks until the first full week of the next month, then makes
   * the backup. This function is used to compute the number of weeks to skip.
   *
   * @return the number weeks to the next month as <code>(first full week of 
   *         next month) - (current week)</code>
   */
  public static int getWeeksToNextMonth() {
    Calendar cal = Calendar.getInstance();
    int thisWeekNumber = cal.get(Calendar.WEEK_OF_YEAR);
    
    int archivingWeekNumber = 0;
    if (cal.get(Calendar.MONTH) == 11) {
      // December
      archivingWeekNumber = cal.getWeeksInWeekYear() + 1;
    } else {
      cal.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, 1);
      archivingWeekNumber = cal.get(Calendar.WEEK_OF_YEAR);
    }
    
    return archivingWeekNumber - thisWeekNumber;
  }
}
