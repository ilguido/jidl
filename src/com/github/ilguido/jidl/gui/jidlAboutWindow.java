/**
 * jidlAboutWindow.java
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

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.text.*;

import java.util.*;
import java.util.Locale;
import java.util.ResourceBundle;


/**
 * jidlAboutWindow
 * A window with a notice about copyright, free software and NO WARRANTY.
 *
 * @version 0.8
 * @author Stefano Guidoni
 */

public class jidlAboutWindow extends JFrame {
  /**
   * A pane with the contents of the window.
   */
  private JTextPane textarea;
  
  /**
   * A resource bundle for the localization of the GUI.
   */
  private ResourceBundle rb;

  /**
   * Class constructor.  It creates the contents of the about window and put
   * them in a JTextPane.
   */
  public jidlAboutWindow() {
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

    // the text area and all its contents
    textarea = new JTextPane();
    StyledDocument doc = textarea.getStyledDocument();
    addStylesToDocument(doc);
    
    // the "about" notice
    try {
      doc.insertString(doc.getLength(),
                       "JIDL\n",
                       doc.getStyle("title"));
      doc.insertString(doc.getLength(),
                       rb.getString("a Java industrial data logger") + "\n",
                       doc.getStyle("subtitle"));
      doc.insertString(doc.getLength(),
                       rb.getString("version") + " 0.8\n",
                       doc.getStyle("subtitle"));
      doc.insertString(doc.getLength(),
                       "Copyright © 2024  Stefano Guidoni\n\n",
                       doc.getStyle("regular"));
      int titlesend = doc.getLength();
      doc.insertString(doc.getLength(),
                       rb.getString("GNU GPL 3 line 1") + "\n",
                       doc.getStyle("regular"));
      doc.insertString(doc.getLength(),
                       rb.getString("GNU GPL 3 line 2") + "\n",
                       doc.getStyle("regular"));
      doc.insertString(doc.getLength(),
                       rb.getString("GNU GPL 3 line 3"),
                       doc.getStyle("regular"));
      SimpleAttributeSet center = new SimpleAttributeSet();
      StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
      doc.setParagraphAttributes(0, titlesend, center, false);
      SimpleAttributeSet justified = new SimpleAttributeSet();
      StyleConstants.setAlignment(justified, StyleConstants.ALIGN_JUSTIFIED);
      doc.setParagraphAttributes(titlesend, doc.getLength(), justified, false);
    } catch (BadLocationException ble) {
        System.err.println("Couldn't insert text into text pane.");
    }
    
    // Provide a preferred size for the text pane.
    textarea.setPreferredSize(new Dimension(400, 320));
    
    // Disable text editing.
    textarea.setEditable(false);
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

    StyleConstants.setFontFamily(def, "SansSerif");
    Style regular = doc.addStyle("regular", def);
    Style title = doc.addStyle("title", def);
    StyleConstants.setBold(title, true);
    StyleConstants.setFontSize(title, 18);
    Style subtitle = doc.addStyle("subtitle", def);
    StyleConstants.setFontSize(subtitle, 14);
  }
  
  /**
   * Returns the about window.
   *
   * @return a JTextPane
   */
  public JTextPane getTextPane() {
    return textarea;
  }
}
