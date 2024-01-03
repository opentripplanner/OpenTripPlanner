package org.opentripplanner.ext.interactivelauncher;

import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.BACKGROUND;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import org.opentripplanner.ext.interactivelauncher.logging.LogModel;
import org.opentripplanner.ext.interactivelauncher.logging.LogView;

public class OtpDebugController {

  private final JFrame debugFrame = new JFrame("OTP Debug Controller");

  public OtpDebugController(Model model) {
    var tabPanel = new JTabbedPane();
    tabPanel.addTab("Logging", createLogPanel(model.getLogModel()));
    tabPanel.addTab("Raptor", new JPanel());
    debugFrame.add(tabPanel);
    debugFrame.getContentPane().setBackground(BACKGROUND);
    start();
  }

  private static JComponent createLogPanel(LogModel logModel) {
    return new LogView(logModel).panel();
  }

  public void start() {
    debugFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    debugFrame.pack();
    debugFrame.setLocationRelativeTo(null);
    debugFrame.setVisible(true);
  }
}
