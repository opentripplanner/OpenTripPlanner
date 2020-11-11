package org.opentripplanner.ext.interactivelauncher.views;

import org.opentripplanner.ext.interactivelauncher.SetupResult;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.function.Consumer;

import static java.awt.GridBagConstraints.BOTH;
import static java.awt.GridBagConstraints.CENTER;
import static java.awt.GridBagConstraints.NONE;
import static java.awt.GridBagConstraints.NORTH;
import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.BACKGROUND;
import static org.opentripplanner.ext.interactivelauncher.views.ViewUtils.debugLayout;

public class MainView extends JFrame {

  /** Margins between components (IN) */
  private static final int M_IN = 10;

  /** Margins around frame boarder (OUT) */
  private static final int M_OUT = 2 * M_IN;

  /*
   The application have the following 4 panels:
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

  // List of locations
  private static final GridBagConstraints CONFIG_DIRS_PANEL_CONSTRAINTS = new GridBagConstraints(
    0, 0, 1, 1, 1.0, 1.0, NORTH, NONE, new Insets(M_OUT, M_OUT, M_IN, M_IN), 0, 0
  );

  // Options panel
  private static final GridBagConstraints OPTIONS_PANEL_CONSTRAINTS = new GridBagConstraints(
      1, 0, 1, 1, 1.0, 1.0, NORTH, NONE, new Insets(M_OUT, M_IN, M_IN, M_OUT), 0, 0
  );

  // Run btn and status
  private static final GridBagConstraints START_OTP_BUTTON_PANEL_CONSTRAINTS = new GridBagConstraints(
      0, 1, 2, 1, 1.0, 1.0, CENTER, BOTH, new Insets(M_IN, M_OUT, M_IN, M_OUT), 0, 0
  );

  // Run btn and status
  private static final GridBagConstraints STATUS_BAR_CONSTRAINTS = new GridBagConstraints(
      0, 2, 2, 1, 1.0, 0.0, CENTER, BOTH, new Insets(M_IN, 0, 0,0), 40, 0
  );

  private final ConfigDirsView configDirsView;
  private final OptionsView optionsView;
  private final StatusBar statusBarTxt = new StatusBar();

  private final Consumer<SetupResult> resultHandler;

  public MainView(Consumer<SetupResult> resultHandler, java.util.List<File> configDirs, String configDirPrefix) throws HeadlessException {
    super("Setup and Run OTP Main");
    this.resultHandler = resultHandler;

    GridBagLayout layout = new GridBagLayout();
    getContentPane().setLayout(layout);
    getContentPane().setBackground(BACKGROUND);

    this.configDirsView = new ConfigDirsView(configDirs, configDirPrefix);
    this.optionsView = new OptionsView();
    StartOtpButtonView startOtpButtonView = new StartOtpButtonView();

    add(configDirsView.panel(), CONFIG_DIRS_PANEL_CONSTRAINTS);
    add(optionsView.panel(), OPTIONS_PANEL_CONSTRAINTS);
    add(startOtpButtonView.panel(), START_OTP_BUTTON_PANEL_CONSTRAINTS);
    add(statusBarTxt, STATUS_BAR_CONSTRAINTS);

    // Setup action listeners
    configDirsView.addActionListener(this::updateStatusBar);
    optionsView.addActionListener(this::updateStatusBar);
    startOtpButtonView.addActionListener(this::startOtp);

    debugLayout(
        configDirsView.panel(),
        optionsView.panel(),
        startOtpButtonView.panel(),
        statusBarTxt
    );
  }

  public void start() {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    optionsView.initState();
    updateStatusBar(null);

    pack();
    setLocationRelativeTo(null);
    setVisible(true);
  }

  private SetupResult getSetup() {
    return new SetupResult(
        configDirsView.getSelectedOtpConfigDataDir(),
        optionsView.buildStreet(),
        optionsView.buildTransit(),
        optionsView.saveGraph(),
        optionsView.startOptServer()
    );
  }

  private void startOtp(ActionEvent e) {
    SetupResult setup = getSetup();
    setVisible(false);
    dispose();
    resultHandler.accept(setup);
  }

  private void updateStatusBar(ActionEvent e) {
    statusBarTxt.setText(getSetup().toCliString());
  }
}
