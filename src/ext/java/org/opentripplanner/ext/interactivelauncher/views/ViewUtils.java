package org.opentripplanner.ext.interactivelauncher.views;

import javax.swing.*;
import java.awt.*;

final class ViewUtils {
  private static final boolean DEBUG_LAYOUT = false;
  static final int SECTION_SPACE = 10;
  static final Color BACKGROUND = new Color(0xe0, 0xf0, 0xff);
  static final Color BG_STATUS_BAR = new Color(0xd0, 0xe0, 0xf0);
  static final Color FG_STATUS_BAR = new Color(0, 0, 0x80);

  static void addSectionSpace(Box panel) {
    panel.add(Box.createVerticalStrut(SECTION_SPACE));
  }

  static void addSectionDoubleSpace(Box panel) {
    panel.add(Box.createVerticalStrut(2 * SECTION_SPACE));
  }

  static void addComp(JComponent c, Box panel) {
    if(DEBUG_LAYOUT) { c.setBorder(BorderFactory.createLineBorder(Color.green)); }
    panel.add(c);
  }

  static void debugLayout(JComponent ... components) {
    if(DEBUG_LAYOUT) {
      for (JComponent c : components) {
        c.setBorder(BorderFactory.createLineBorder(Color.red));
      }
    }
  }

  static void adjustSize(JComponent c, int dWidth, int dHeight) {
    Dimension d0 = c.getPreferredSize();
    Dimension d = new Dimension(d0.width + dWidth, d0.height + dHeight);
    c.setMinimumSize(d);
    c.setPreferredSize(d);
    c.setMaximumSize(d);
  }
}
