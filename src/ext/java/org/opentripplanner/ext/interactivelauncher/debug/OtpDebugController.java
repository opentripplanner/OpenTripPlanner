package org.opentripplanner.ext.interactivelauncher.debug;

import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.BACKGROUND;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import org.opentripplanner.ext.interactivelauncher.Model;
import org.opentripplanner.ext.interactivelauncher.debug.logging.LogView;
import org.opentripplanner.ext.interactivelauncher.debug.raptor.RaptorDebugView;

/**
 * This controller/UI allows changing the debug loggers and setting the raptor
 * debug parameters for incoming rute requests.
 */
public class OtpDebugController {

  private final JFrame debugFrame = new JFrame("OTP Debug Controller");

  public OtpDebugController(Model model) {
    debugFrame.add(createTabbedPane(model));
    debugFrame.getContentPane().setBackground(BACKGROUND);
  }

  public void start() {
    debugFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    debugFrame.pack();
    debugFrame.setLocationRelativeTo(null);
    debugFrame.setVisible(true);
  }

  private static JTabbedPane createTabbedPane(Model model) {
    var tabPanel = new JTabbedPane();
    tabPanel.addTab("Logging", new LogView(model.getLogModel()).panel());
    tabPanel.addTab("Raptor", new RaptorDebugView(model.getRaptorDebugModel()).panel());
    return tabPanel;
  }
}
