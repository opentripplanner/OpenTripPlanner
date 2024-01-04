package org.opentripplanner.ext.interactivelauncher.startup;

import static java.awt.GridBagConstraints.CENTER;
import static java.awt.GridBagConstraints.HORIZONTAL;
import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.BACKGROUND;
import static org.opentripplanner.ext.interactivelauncher.support.ViewUtils.debugLayout;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.Insets;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class MainView {

  private static final int M_IN = 10;
  private static final int M_OUT = 2 * M_IN;
  private static final Insets DEFAULT_INSETS = new Insets(M_OUT, M_OUT, M_IN, M_OUT);
  private static final Insets SMALL_INSETS = new Insets(M_OUT, M_OUT, M_IN, M_OUT);
  private static int Y = 0;

  /*
   The application have the following panels:
   +-----------------------------------+
   |  Root dir                 [Open]  |
   +-----------------------------------+
   |         Config Dirs Panel         |
   +-----------------------------------+
   |          Options Panel            |
   +-----------------------------------+
   |           [ Start OTP ]           |
   +-----------------------------------+
   |            Status Bar             |
   +-----------------------------------+
  */

  private static final GridBagConstraints DATA_SOURCE_ROOT_PANEL_CONSTRAINTS = gbc(0f);
  private static final GridBagConstraints DATA_SOURCE_LIST_PANEL_CONSTRAINTS = gbc(1f);
  private static final GridBagConstraints OPTIONS_PANEL_CONSTRAINTS = gbc(1f);
  private static final GridBagConstraints START_BUTTON_PANEL_CONSTRAINTS = gbc(0f);
  private static final GridBagConstraints STATUS_BAR_CONSTRAINTS = gbc(0f, SMALL_INSETS, 40);

  private final JFrame mainFrame = new JFrame("Setup and Run OTP Main");

  private final DataSourcesView dataSourcesView;
  private final OptionsView optionsView;
  private final StartOtpButtonView startOtpButtonView;
  private final Runnable otpStarter;
  private final StartupModel model;

  public MainView(Runnable otpStarter, StartupModel model) throws HeadlessException {
    var innerPanel = new JPanel();
    var statusBarTxt = new StatusBar();

    this.otpStarter = otpStarter;
    this.model = model;

    mainFrame.setContentPane(new JScrollPane(innerPanel));
    GridBagLayout layout = new GridBagLayout();
    innerPanel.setLayout(layout);
    innerPanel.setBackground(BACKGROUND);

    var sourceDirectoryView = new DataSourceRootView(
      model.getRootDirectory(),
      this::onRootDirChanged
    );
    this.dataSourcesView = new DataSourcesView(model);
    this.optionsView = new OptionsView(model);
    this.startOtpButtonView = new StartOtpButtonView();

    innerPanel.add(sourceDirectoryView.panel(), DATA_SOURCE_ROOT_PANEL_CONSTRAINTS);
    innerPanel.add(dataSourcesView.panel(), DATA_SOURCE_LIST_PANEL_CONSTRAINTS);
    innerPanel.add(optionsView.panel(), OPTIONS_PANEL_CONSTRAINTS);
    innerPanel.add(startOtpButtonView.panel(), START_BUTTON_PANEL_CONSTRAINTS);
    innerPanel.add(statusBarTxt, STATUS_BAR_CONSTRAINTS);

    // Setup action listeners
    startOtpButtonView.addActionListener(e -> startOtp());

    debugLayout(
      sourceDirectoryView.panel(),
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

    startOtpButtonView.grabFocus();
  }

  private void startOtp() {
    mainFrame.setVisible(false);
    mainFrame.dispose();
    otpStarter.run();
  }

  private static GridBagConstraints gbc(float weighty) {
    return gbc(weighty, DEFAULT_INSETS, 0);
  }

  private static GridBagConstraints gbc(float weighty, Insets insets, int ipadx) {
    return new GridBagConstraints(0, Y++, 1, 1, 1.0, weighty, CENTER, HORIZONTAL, insets, ipadx, 0);
  }
}
