package org.opentripplanner.ext.interactivelauncher.views;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.CENTER;
import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.NORTH;
import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.BACKGROUND;
import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.debugLayout;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import org.opentripplanner.ext.interactivelauncher.Model;

public class MainView {

  /** Margins between components (IN) */
  private static final int M_IN = 10;

  /** Margins around frame boarder (OUT) */
  private static final int M_OUT = 2 * M_IN;

  /*
   The application have the following 4 panels:
   +-----------------------------------+
   |  Root dir [Open]                  |
   +-------------------+---------------+
   |                   |               |
   | Config Dirs Panel | Options Panel |
   |                   |               |
   +-------------------+---------------+
   |       Start OTP Main Panel        |
   +-----------------------------------+
   |            Status Bar             |
   +-----------------------------------+
  */

  // Root dir view
  private static final GridBagConstraints CONFIG_SOURCE_DIR_PANEL_CONSTRAINTS = new GridBagConstraints(
    0,
    0,
    2,
    1,
    1.0,
    0.0,
    NORTH,
    BOTH,
    new Insets(M_OUT, M_OUT, M_IN, M_IN),
    0,
    0
  );

  // List of locations
  private static final GridBagConstraints CONFIG_DIRS_PANEL_CONSTRAINTS = new GridBagConstraints(
    0,
    1,
    1,
    1,
    1.0,
    1.0,
    NORTH,
    NONE,
    new Insets(M_OUT, M_OUT, M_IN, M_IN),
    0,
    0
  );

  // Options panel
  private static final GridBagConstraints OPTIONS_PANEL_CONSTRAINTS = new GridBagConstraints(
    1,
    1,
    1,
    1,
    1.0,
    1.0,
    NORTH,
    NONE,
    new Insets(M_OUT, M_IN, M_IN, M_OUT),
    0,
    0
  );

  // Run btn and status
  private static final GridBagConstraints START_OTP_BUTTON_PANEL_CONSTRAINTS = new GridBagConstraints(
    0,
    2,
    2,
    1,
    1.0,
    1.0,
    CENTER,
    BOTH,
    new Insets(M_IN, M_OUT, M_IN, M_OUT),
    0,
    0
  );

  // Run btn and status
  private static final GridBagConstraints STATUS_BAR_CONSTRAINTS = new GridBagConstraints(
    0,
    3,
    2,
    1,
    1.0,
    0.0,
    CENTER,
    BOTH,
    new Insets(M_IN, 0, 0, 0),
    40,
    0
  );

  private final JFrame mainFrame = new JFrame("Setup and Run OTP Main");

  private final DataSourcesView dataSourcesView;
  private final OptionsView optionsView;
  private final Runnable otpStarter;
  private final Model model;

  public MainView(Runnable otpStarter, Model model) throws HeadlessException {
    var innerPanel = new JPanel();
    var statusBarTxt = new StatusBar();

    this.otpStarter = otpStarter;
    this.model = model;

    mainFrame.setContentPane(new JScrollPane(innerPanel));
    GridBagLayout layout = new GridBagLayout();
    innerPanel.setLayout(layout);
    innerPanel.setBackground(BACKGROUND);

    var sourceDirectoryView = new SearchDirectoryView(
      model.getRootDirectory(),
      this::onRootDirChanged
    );
    this.dataSourcesView = new DataSourcesView(model);
    this.optionsView = new OptionsView(model);
    StartOtpButtonView startOtpButtonView = new StartOtpButtonView();

    innerPanel.add(sourceDirectoryView.panel(), CONFIG_SOURCE_DIR_PANEL_CONSTRAINTS);
    innerPanel.add(dataSourcesView.panel(), CONFIG_DIRS_PANEL_CONSTRAINTS);
    innerPanel.add(optionsView.panel(), OPTIONS_PANEL_CONSTRAINTS);
    innerPanel.add(startOtpButtonView.panel(), START_OTP_BUTTON_PANEL_CONSTRAINTS);
    innerPanel.add(statusBarTxt, STATUS_BAR_CONSTRAINTS);

    // Setup action listeners
    startOtpButtonView.addActionListener(e -> startOtp());

    debugLayout(
      dataSourcesView.panel(),
      optionsView.panel(),
      startOtpButtonView.panel(),
      statusBarTxt
    );
    model.subscribeCmdLineUpdates(statusBarTxt::setText);

    statusBarTxt.setText(model.toCliString());
  }

  public void onRootDirChanged(String newRootDir) {
    model.setRootDirectory(newRootDir);
    dataSourcesView.onRootDirChange();
    mainFrame.pack();
    mainFrame.repaint();
  }

  public void start() {
    mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    optionsView.initState();

    mainFrame.pack();
    mainFrame.setLocationRelativeTo(null);
    mainFrame.setVisible(true);
  }

  private void startOtp() {
    mainFrame.setVisible(false);
    mainFrame.dispose();
    otpStarter.run();
  }
}
