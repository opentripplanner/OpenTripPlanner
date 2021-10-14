package org.opentripplanner.ext.interactivelauncher;

import static org.opentripplanner.ext.interactivelauncher.DebugLoggingSupport.configureDebugLogging;

import org.opentripplanner.ext.interactivelauncher.views.MainView;
import org.opentripplanner.standalone.OTPMain;


/**
 * This class provide a main method to start a GUI witch can start OTPMain.
 * <p>
 * The UI allow the user to select a OTP configuration data set. The list of
 * data location is created by searching the a root data source directory.
 * <p>
 * The user then select what he/she want OTP to do. The settings are stored
 * in the <code>.interactive_otp_main.json</code> file in the folder InteractiveOtpMain
 * is started. The settings from the last run is loaded next time InteractiveOtpMain
 * is started.
 */
public class InteractiveOtpMain {
  private Model model;

  public static void main(String[] args) {
    new InteractiveOtpMain().run();
  }

  private void run() {
    this.model = Model.load();
    MainView frame = new MainView(this::startOtp, model);
    frame.start();
  }

  private void startOtp() {
    model.save();

    configureDebugLogging(model.getDebugLogging());

    System.out.println("Start OTP: " + model + "\n");
    OTPMain.main(model.asOtpArgs());
  }

}
