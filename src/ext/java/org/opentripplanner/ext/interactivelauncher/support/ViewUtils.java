package org.opentripplanner.ext.interactivelauncher.support;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;

public final class ViewUtils {

  private static final boolean DEBUG_LAYOUT = false;
  static final int SECTION_SPACE = 10;
  public static final Color BACKGROUND = new Color(0xe0, 0xf0, 0xff);
  public static final Color BG_STATUS_BAR = new Color(0xd0, 0xe0, 0xf0);
  public static final Color FG_STATUS_BAR = new Color(0, 0, 0x80);

  public static void addVerticalSectionSpace(Box panel) {
    panel.add(Box.createVerticalStrut(SECTION_SPACE));
  }

  public static void addHorizontalGlue(Box box) {
    box.add(Box.createHorizontalGlue());
  }

  public static void addLabel(String label, Container panel) {
    addComp(new JLabel(label), panel);
  }

  public static void addComp(JComponent c, Container panel) {
    if (DEBUG_LAYOUT) {
      c.setBorder(BorderFactory.createLineBorder(Color.green));
    }
    panel.add(c);
  }

  public static void debugLayout(JComponent... components) {
    if (DEBUG_LAYOUT) {
      for (JComponent c : components) {
        c.setBorder(BorderFactory.createLineBorder(Color.red));
      }
    }
  }

  public static void adjustSize(JComponent c, int dWidth, int dHeight) {
    Dimension d0 = c.getPreferredSize();
    Dimension d = new Dimension(d0.width + dWidth, d0.height + dHeight);
    c.setMinimumSize(d);
    c.setPreferredSize(d);
    c.setMaximumSize(d);
  }
}
