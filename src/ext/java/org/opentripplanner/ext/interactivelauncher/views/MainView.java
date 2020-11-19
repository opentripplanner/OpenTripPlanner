package org.opentripplanner.ext.interactivelauncher.views;

import org.opentripplanner.ext.interactivelauncher.Model;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

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
      0, 0, 2, 1, 1.0, 0.0, NORTH, BOTH, new Insets(M_OUT, M_OUT, M_IN, M_IN), 0, 0
  );

  // List of locations
  private static final GridBagConstraints CONFIG_DIRS_PANEL_CONSTRAINTS = new GridBagConstraints(
    0, 1, 1, 1, 1.0, 1.0, NORTH, NONE, new Insets(M_OUT, M_OUT, M_IN, M_IN), 0, 0
  );

  // Options panel
  private static final GridBagConstraints OPTIONS_PANEL_CONSTRAINTS = new GridBagConstraints(
      1, 1, 1, 1, 1.0, 1.0, NORTH, NONE, new Insets(M_OUT, M_IN, M_IN, M_OUT), 0, 0
  );

  // Run btn and status
  private static final GridBagConstraints START_OTP_BUTTON_PANEL_CONSTRAINTS = new GridBagConstraints(
      0, 2, 2, 1, 1.0, 1.0, CENTER, BOTH, new Insets(M_IN, M_OUT, M_IN, M_OUT), 0, 0
  );

  // Run btn and status
  private static final GridBagConstraints STATUS_BAR_CONSTRAINTS = new GridBagConstraints(
      0, 3, 2, 1, 1.0, 0.0, CENTER, BOTH, new Insets(M_IN, 0, 0,0), 40, 0
  );

  private final DataSourcesView dataSourcesView;
  private final OptionsView optionsView;
  private final StatusBar statusBarTxt = new StatusBar();

  private final Runnable otpStarter;
  private final Model model;

  public MainView(Runnable otpStarter, Model model) throws HeadlessException {
    super("Setup and Run OTP Main");
    this.otpStarter = otpStarter;
    this.model = model;

    GridBagLayout layout = new GridBagLayout();
    getContentPane().setLayout(layout);
    getContentPane().setBackground(BACKGROUND);

    var sourceDirectoryView = new SearchDirectoryView(
        model.getRootDirectory(),
        this::onRootDirChanged
    );
    this.dataSourcesView = new DataSourcesView(model, this::updateStatusBar);
    this.optionsView = new OptionsView(model);
    StartOtpButtonView startOtpButtonView = new StartOtpButtonView();

    add(sourceDirectoryView.panel(), CONFIG_SOURCE_DIR_PANEL_CONSTRAINTS);
    add(dataSourcesView.panel(), CONFIG_DIRS_PANEL_CONSTRAINTS);
    add(optionsView.panel(), OPTIONS_PANEL_CONSTRAINTS);
    add(startOtpButtonView.panel(), START_OTP_BUTTON_PANEL_CONSTRAINTS);
    add(statusBarTxt, STATUS_BAR_CONSTRAINTS);

    // Setup action listeners
    optionsView.addActionListener(e -> this.updateStatusBar());
    startOtpButtonView.addActionListener(this::startOtp);

    debugLayout(
        dataSourcesView.panel(),
        optionsView.panel(),
        startOtpButtonView.panel(),
        statusBarTxt
    );
  }

  public void onRootDirChanged(String newRootDir) {
    model.setRootDirectory(newRootDir);
    dataSourcesView.onRootDirChange();
    updateStatusBar();
    pack();
    repaint();
  }

  public void start() {
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    optionsView.initState();
    updateStatusBar();

    pack();
    setLocationRelativeTo(null);
    setVisible(true);
  }

  private void startOtp(ActionEvent e) {
    setVisible(false);
    dispose();
    updateModel();
    otpStarter.run();
  }

  private void updateModel() {
    dataSourcesView.updateModel(model);
    optionsView.updateModel(model);
  }

  private void updateStatusBar() {
    statusBarTxt.setText(model.toCliString());
  }
}
