package org.opentripplanner.ext.interactivelauncher.startup;

import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.adjustSize;

import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.JButton;

class StartOtpButtonView {

  private static final int BUTTON_D_WIDTH = 160;
  private static final int BUTTON_D_HEIGHT = 4;

  private final Box panel = Box.createHorizontalBox();
  private final JButton startOtpBtn = new JButton("Start OTP");

  public StartOtpButtonView() {
    adjustSize(startOtpBtn, BUTTON_D_WIDTH, BUTTON_D_HEIGHT);

    panel.add(Box.createGlue());
    panel.add(startOtpBtn);
    panel.add(Box.createGlue());
  }

  public void addActionListener(ActionListener l) {
    startOtpBtn.addActionListener(l);
  }

  Box panel() {
    return panel;
  }

  void grabFocus() {
    startOtpBtn.grabFocus();
  }
}
