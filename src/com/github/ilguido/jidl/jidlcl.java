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
import java.util.Scanner;
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
   * Main method, it starts the command line interface of jidl.  It needs a
   * configuration file among its arguments. If it is not autostarted, it waits
   * for user input.
   *
   * @param args the command line arguments <br>
   *             <code>-c filename</code> to load a configuration file<br>
   *             <code>-a</code> to autostart the data logging<br>
   *             <code>-r</code> to allow remote control through IPC
   */
  public static void main(String[] args)
    throws IllegalArgumentException, ExecutionException {
    System.out.println("JIDL version 0.8  Copyright (C) 2024  Stefano Guidoni");
    System.out.println("This program comes with ABSOLUTELY NO WARRANTY.");
    System.out.println("This is free software, and you are welcome to \n" + 
                       "redistribute it under certain conditions.");
    if (initJidl(args)) {
      dataLogger.startLogging();
    } else {
      boolean run = true;
      Scanner userInput = new Scanner(System.in);
      while (run) {
        System.out.println("Enter [s] to start logging, [p] to pause, [q] to " +
                           "quit.");

        String input = userInput.next();
        if (!input.isEmpty()) {
          if (input.equals("s")) {
            dataLogger.startLogging();
          } else if (input.equals("p")) {
            dataLogger.stopLogging();
          } else if (input.equals("q")) {
            run = false;
          } else {
            System.out.println("Unknown command: " + input);
          }
        }
      }
    }
  }
}
