package com.github.ilguido.jidl.gui.locale;

import java.util.ListResourceBundle;

public class jidlResources extends ListResourceBundle
{
   public Object [][] getContents()
   {
      return contents;
   }
   static final Object[][] contents =
   {
      {"status", "status"},
      {"name", "name"},
      {"type", "type"},
      {"sample time", "sample time"},
      {"ip address", "IP address"},
      {"port", "port"},
      {"rack", "rack"},
      {"slot", "slot"},
      {"order", "reversed order"},
      {"url", "URL"},
      {"connected", "connected"},
      {"not connected", "not connected"},
      {"ERROR!", "ERROR!"},
      {"JIDL Simple Systemtray utility", "JIDL Simple Systemtray utility"},
      {"Load configuration", "Load configuration"},
      {"Data logging", "Data logging"},
      {"Start", "Start"},
      {"Stop", "Stop"},
      {"Status", "Status"},
      {"Quit", "Quit"},
      {"Data logging unexpectedly stopped: ", "Data logging unexpectedly stopped: "},
      {"Configuration not loaded", "Configuration not loaded"},
      {"Cannot start the data logging: ", "Cannot start the data logging: "},
      {"Data logging already running", "Data logging already running"},
      {"Data logging already stopped", "Data logging already stopped"},
      {"JIDL connections", "JIDL connections"},
      {"Could not perform the action while data logging is running", "Could not perform the action while data logging is running"},
   };
}
