/**
 * jidlcl.java
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

package com.github.ilguido.jidl;

import java.io.File;
import java.util.concurrent.ExecutionException;

import com.github.ilguido.jidl.datalogger.*;
import com.github.ilguido.jidl.jidl;


/**
 * jidlcl
 * A simple command line interface for jidl.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

class jidlcl extends jidl {
  /**
   * The level of logging activity.
   */
  public int loglevel = 0;

  /**
   * Main method.
   */
  public static void main(String[] args)
    throws IllegalArgumentException, ExecutionException {
    if (initJidl(args))
      dataLogger.startLogging();
  }
}
