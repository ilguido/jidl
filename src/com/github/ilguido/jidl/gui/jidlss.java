/**
 * jidlss.java
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

package com.github.ilguido.jidl.gui;;

import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import javax.swing.*;
import javax.swing.filechooser.*;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

import com.github.ilguido.jidl.datalogger.*;
import com.github.ilguido.jidl.connectionmanager.*;
import com.github.ilguido.jidl.gui.jidlStatusWindow;
import com.github.ilguido.jidl.jidl;


/**
 * jidlss
 * Jidl Simple System tray, a minimal GUI for jidl.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class jidlss extends jidl {
  /**
   * A boolean value that is <code>true</code> when the autostart parameter is
   * set and the configuration is valid.  JIDL runs automatically when autostart
   * is requested and a valid configuration is loaded.
   */
  private static boolean autostarted = false;
  
  /**
   * A resource bundle for the localization of the GUI.
   */
  private static ResourceBundle rb;
  
  /**
   * Launches a simple system tray application.
   *
   * @param args the command line arguments <br>
   *             <code>-c filename</code> to load a configuration file
   *             <code>-a</code> to autostart the data logging
   */
  public static void main(String[] args) {
    /* Use an appropriate Look and Feel */
    try {
      // if on Windows try to use the Windows style
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      //UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    try {
      if (initJidl(args))
        autostarted = true;
    } catch (Exception e) {
      JOptionPane.showMessageDialog(null, e.getMessage(), "JIDL",
                                    JOptionPane.ERROR_MESSAGE);
      System.exit(1);
    }
    
    //Schedule a job for the event-dispatching thread:
    //adding TrayIcon.
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        createAndShowGUI();
      }
    });
  }
  
  /**
   * Creates the GUI elements of the simple system tray application.
   */
  private static void createAndShowGUI() {
    //FIXME: move to a method in a different class
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
    
    //Check the SystemTray support
    if (!SystemTray.isSupported()) {
      System.out.println("SystemTray is not supported");
      return;
    }

    final PopupMenu popup = new PopupMenu();
    final TrayIcon trayIcon = 
                 new TrayIcon(createImage("icons/jidlss_nr.png", "not ready"));
    trayIcon.setImageAutoSize(true);
    trayIcon.setToolTip(rb.getString("JIDL Simple Systemtray utility"));
    final SystemTray tray = SystemTray.getSystemTray();
        
    // Create a popup menu components
    MenuItem loadItem = new MenuItem(rb.getString("Load configuration"));
    Menu logMenu = new Menu(rb.getString("Data logging"));
    MenuItem startItem = new MenuItem(rb.getString("Start"));
    MenuItem stopItem = new MenuItem(rb.getString("Stop"));
    MenuItem statusItem = new MenuItem(rb.getString("Status"));
    MenuItem exitItem = new MenuItem(rb.getString("Quit"));
        
    //Add components to popup menu
    if (dataLogger != null)
      // JIDL was launched with a configuration file
      trayIcon.setImage(createImage("icons/jidlss_r.png", "ready"));
    else
      popup.add(loadItem);
    popup.add(logMenu);
    logMenu.add(startItem);
    logMenu.add(stopItem);
    logMenu.addSeparator();
    logMenu.add(statusItem);
    popup.addSeparator();
    popup.add(exitItem);
        
    trayIcon.setPopupMenu(popup);
        
    try {
      tray.add(trayIcon);
    } catch (AWTException e) {
      System.out.println("TrayIcon could not be added.");
      return;
    }
        
    // A custom UncaughtExceptionHandler for the logger:
    // it shows a warning message when the logging stops due to an internal
    // error.
    Thread.UncaughtExceptionHandler h = new Thread.UncaughtExceptionHandler() {
      @Override
      public void uncaughtException(Thread inT, Throwable inE) {
        trayIcon.displayMessage("JIDL", 
                           rb.getString("Data logging unexpectedly stopped: ") +
                                inE.toString() + inE.getMessage(), 
                                TrayIcon.MessageType.ERROR);
      }
    };
    
    ActionListener listener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        MenuItem item = (MenuItem)e.getSource();
        //System.out.println(item.getLabel());

        if (dataLogger == null) {
          trayIcon.displayMessage("JIDL",
                                  rb.getString("Configuration not loaded"), 
                                  TrayIcon.MessageType.ERROR);
        } else if (rb.getString("Start").equals(item.getLabel())) {
          if (dataLogger.getStatus() == false) {
            try {
              dataLogger.startLogging(h);
              trayIcon.setImage(createImage("icons/jidlss_s.png", "started"));
            } catch (ExecutionException ee) {
              trayIcon.displayMessage("JIDL",
                               rb.getString("Cannot start the data logging: ") +
                                      ee.toString() + ee.getMessage(),
                                      TrayIcon.MessageType.ERROR);
            }
          } else {
            trayIcon.displayMessage("JIDL",
                                   rb.getString("Data logging already running"),
                                   TrayIcon.MessageType.WARNING);
          }
        } else if (rb.getString("Stop").equals(item.getLabel())) {
          if (dataLogger.getStatus() == true) {
            dataLogger.stopLogging();
            trayIcon.setImage(createImage("icons/jidlss_r.png", "ready"));
          } else {
            trayIcon.displayMessage("JIDL",
                                   rb.getString("Data logging already stopped"), 
                                   TrayIcon.MessageType.WARNING);
          }
        } else if (rb.getString("Status").equals(item.getLabel())) {
          //Create and set up the window.
          JFrame frame = new JFrame(rb.getString("JIDL connections"));
          frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
          jidlStatusWindow jsw = new jidlStatusWindow(connectionList);
          frame.getContentPane().add(jsw.getSplitPane());
          frame.setIconImage(createImage("icons/jidlss.png", "popup icon"));

          //Display the window.
          frame.pack();
          frame.setVisible(true);
        }
      }
    };
        
    startItem.addActionListener(listener);
    stopItem.addActionListener(listener);
    statusItem.addActionListener(listener);
        
    // exit action
    exitItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        tray.remove(trayIcon);
        if (dataLogger != null && dataLogger.getStatus() == true)
          dataLogger.stopLogging();
        System.exit(0);
      }
    });
        
    // load configuration
    loadItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent actionEvent) 
        throws IllegalArgumentException {
          if (dataLogger != null && dataLogger.getStatus() == true) {
            trayIcon.displayMessage("JIDL",
     rb.getString("Could not perform the action while data logging is running"),
                                    TrayIcon.MessageType.ERROR);
          } else {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setCurrentDirectory(new File(
                                              System.getProperty("user.home")));
            FileNameExtensionFilter filter = new FileNameExtensionFilter(
                                      "JIDL configuration", "cfg", "ini", "rc");
            fileChooser.addChoosableFileFilter(filter);

            int result = fileChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
              File selectedFile = fileChooser.getSelectedFile();
              System.out.println("Selected file: " +
                                 selectedFile.getAbsolutePath());
              try {
                loadConfiguration(selectedFile);
                trayIcon.setImage(createImage("icons/jidlss_r.png", "ready"));
              } catch (IllegalArgumentException|ExecutionException e) {
                trayIcon.displayMessage("JIDL",
                    e.toString() + e.getMessage(), TrayIcon.MessageType.ERROR);
              }
            }
          }
      }
    });
    
    // autostart
    //FIXME: duplicated code!
    if (autostarted) {
      try {
        dataLogger.startLogging(h);
        trayIcon.setImage(createImage("icons/jidlss_s.png", "started"));
      } catch (ExecutionException ee) {
        trayIcon.displayMessage("JIDL",
                               rb.getString("Cannot start the data logging: ") +
                                ee.toString() + ee.getMessage(),
                                TrayIcon.MessageType.ERROR);
      }
    }
  }
    
  /**
   * Loads an image from a file.
   *
   * @param path the path to the file
   * @param description the description of the image
   * @return an image as a <code>Image</code> object
   */
  protected static Image createImage(String path, String description) {
      URL imageURL = jidlss.class.getResource(path);
      
      if (imageURL == null) {
          System.err.println("Resource not found: " + path);
          return null;
      } else {
          return (new ImageIcon(imageURL, description)).getImage();
      }
  }
}
