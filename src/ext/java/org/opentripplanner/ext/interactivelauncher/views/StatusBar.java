package org.opentripplanner.ext.interactivelauncher.views;

import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.BG_STATUS_BAR;
import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.FG_STATUS_BAR;

import javax.swing.JTextField;

public class StatusBar extends JTextField {

  public StatusBar() {
    setEditable(false);
    setBackground(BG_STATUS_BAR);
    setForeground(FG_STATUS_BAR);
  }
}
