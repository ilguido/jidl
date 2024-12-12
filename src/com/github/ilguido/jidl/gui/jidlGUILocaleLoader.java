/**
 * jidlGUILocaleLoader.java
 *
 * Copyright (c) 2024 Stefano Guidoni
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

package com.github.ilguido.jidl.gui;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * jidlGUILocaleLoader
 * A class to load and store the localization for the GUI.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class jidlGUILocaleLoader {
  /**
  * A resource bundle for the localization of the GUI.
  */
  private static ResourceBundle rb;
  
  /**
   * Gets the default locale and, accordingly, loads a 
   * <code>ResourceBundle</code>.
   */
  public jidlGUILocaleLoader() {
    Locale locallocale = Locale.getDefault();
    if (locallocale.getLanguage().equals("it")) {
      Locale itlocale = new Locale("it");
      rb = ResourceBundle.getBundle("com.github.ilguido.jidl.gui.locale." +
                                    "jidlResources", itlocale);
    } else {
      Locale nulllocale = new Locale("");
      rb = ResourceBundle.getBundle("com.github.ilguido.jidl.gui.locale." + 
                                    "jidlResources", nulllocale);
    }
  }
  
  /**
   * Returns the <code>ResourceBundle</code> for the default locale.
   *
   * @return a <code>ResourceBundle</code> object
   */
  public static ResourceBundle getResourceBundle() {
    return rb;
  }
}
