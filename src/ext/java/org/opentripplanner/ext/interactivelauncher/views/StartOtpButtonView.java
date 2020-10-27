package org.opentripplanner.ext.interactivelauncher.views;

import javax.swing.*;

import java.awt.event.ActionListener;

import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.adjustSize;

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

  Box panel() { return panel; }
}
