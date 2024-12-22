/**
 * jidlStatusWindow.java
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

package com.github.ilguido.jidl.gui;

import java.awt.Dimension;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import java.util.List;
import java.util.ResourceBundle;

import com.github.ilguido.jidl.connectionmanager.ConnectionManager;

/**
 * jidlStatusWindow
 * A window with a report of the status of the connections.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class jidlStatusWindow extends JFrame
  implements ListSelectionListener {
  /**
   * A pane with the description of the selected connection.
   */
  private JTextPane description;
  
  /**
   * The list of connections to put in a scroll pane.
   */
  private JList<String> list;
  
  /**
   * A split pane, the main container of the status window.
   */
  private JSplitPane splitPane;
  
  /**
   * The list of connections.
   */
  private String[] listItems;
  
  /**
   * The list of {@link com.github.ilguido.jidl.connectionmanager.ConnectionManager} objects.
   */
  private List<ConnectionManager> connectionList;
  
  /**
   * A resource bundle for the localization of the GUI.
   */
  private final ResourceBundle rb;

  /**
   * Class constructor.  It creates the contents of the status window and put
   * them in a JSplitPane.
   *
   * @param inRB the <code>ResourceBundle</code> used for the localization of
   *             the GUI
   * @param inList a list of {@link com.github.ilguido.jidl.connectionmanager.ConnectionManager}
   *               objects
   */
  public jidlStatusWindow(final ResourceBundle inRB, 
                          List<ConnectionManager> inList) {
    int i = 0;
    rb = inRB;
    connectionList = inList;
    listItems = new String[connectionList.size()];

    // build an array of strings:
    // it is the list of connections
    for (ConnectionManager cm : connectionList) {
      listItems[i] = cm.getName();
      i += 1;
    }

    //Create the list of connections and put it in a scroll pane.
    list = new JList<>(listItems);
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setSelectedIndex(0);
    list.addListSelectionListener(this);

    // Add it to a scroll pane
    JScrollPane listScrollPane = new JScrollPane(list);

    // the text area with the description of the selected connection
    description = new JTextPane();
    StyledDocument doc = description.getStyledDocument();
    addStylesToDocument(doc);

    // Disable text editing.
    description.setEditable(false);
    
    //Create a split pane with the two scroll panes in it.
    splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                 listScrollPane, description);
    splitPane.setOneTouchExpandable(true);
    splitPane.setDividerLocation(150);

    //Provide minimum sizes for the two components in the split pane.
    Dimension minimumSize = new Dimension(100, 50);
    listScrollPane.setMinimumSize(minimumSize);
    //descriptionScrollPane.setMinimumSize(minimumSize);

    //Provide a preferred size for the split pane.
    splitPane.setPreferredSize(new Dimension(400, 200));
    updateDescription(listItems[list.getSelectedIndex()]);
  }

  /**
   * Listener for the Jlist.
   *
   * @param e a <code>ListSelectionEvent</code>
   */
  public void valueChanged(ListSelectionEvent e) {
    JList list = (JList)e.getSource();
    updateDescription(listItems[list.getSelectedIndex()]);
  }

  /**
   * Updates the description to match the selected connection.
   *
   * @param name the name of the selected connection
   */
  protected void updateDescription (String name) {
    StyledDocument doc = description.getStyledDocument();

    try {
      doc.remove(0, doc.getLength());
      doc.insertString(0, name + "\n\n", doc.getStyle("title"));
      for (ConnectionManager cm : connectionList) {
        if (name.equals(cm.getName())) {
          String[] parameters = cm.getParameterNames();
          
          for (int i = 2; i < parameters.length; i++) {
            String s = "";
            try {
              Object o = cm.getParameterByName(parameters[i]);
              
              if (o instanceof String) {
                s = (String) o;
              } else if (o instanceof Integer) {
                s = String.valueOf((Integer) o);
              } else if (o instanceof Boolean) {
                s = String.valueOf((Boolean) o);
              }
              
              doc.insertString(doc.getLength(),
                               rb.getString(parameters[i]) + ": ",
                               doc.getStyle("regular"));
              doc.insertString(doc.getLength(),
                               s + "\n",
                               doc.getStyle("italic"));
            } catch (Exception e) {
              doc.insertString(doc.getLength(),
                               "...\n",
                               doc.getStyle("regular"));
            }
          }

          doc.insertString(doc.getLength(),
                           rb.getString("sample time") + ": ",
                           doc.getStyle("regular"));
          doc.insertString(doc.getLength(),
                           String.valueOf(cm.getSampleTime() / 10.0) + "s\n\n",
                           doc.getStyle("italic"));
          
          // connection status
          doc.insertString(doc.getLength(), 
                           rb.getString("status") + ": ",
                           doc.getStyle("regular"));
          if (!cm.isInitialized()) {
            doc.insertString(doc.getLength(),
                             rb.getString("ERROR!") + "\n",
                             doc.getStyle("italic"));
          } else if (cm.getStatus()) {
            doc.insertString(doc.getLength(),
                             rb.getString("connected") + "\n",
                             doc.getStyle("italic"));
          } else {
            doc.insertString(doc.getLength(),
                             rb.getString("not connected") + "\n",
                             doc.getStyle("italic"));
          }
        }
      }
    } catch (BadLocationException ble) {
        System.err.println("Couldn't insert initial text into text pane.");
    }
  }

  /**
   * Adds a number of styles to a <code>StyledDocument</code> object.
   *
   * @param doc a <code>StyledDocument</code> object
   */
  protected void addStylesToDocument(StyledDocument doc) {
    //Initialize some styles.
    Style def = StyleContext.getDefaultStyleContext().
                   getStyle(StyleContext.DEFAULT_STYLE);

    StyleConstants.setFontFamily(def, "Monospaced");
    Style regular = doc.addStyle("regular", def);
    Style s = doc.addStyle("italic", regular);
    StyleConstants.setItalic(s, true);
    s = doc.addStyle("title", regular);
    StyleConstants.setBold(s, true);
    StyleConstants.setFontSize(s, 18);
  }
  
  /**
   * Returns the status window.
   *
   * @return a JSplitPane
   */
  public JSplitPane getSplitPane() {
    return splitPane;
  }
}
