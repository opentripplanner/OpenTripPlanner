package org.opentripplanner.ext.interactivelauncher.startup;

import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.BG_STATUS_BAR;
import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.FG_STATUS_BAR;

import javax.swing.JTextField;

class StatusBar extends JTextField {

  public StatusBar() {
    setEditable(false);
    setBackground(BG_STATUS_BAR);
    setForeground(FG_STATUS_BAR);
  }
}
