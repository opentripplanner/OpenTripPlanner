package org.opentripplanner.ext.interactivelauncher;

import org.opentripplanner.ext.interactivelauncher.configuration.InteractiveLauncherModule;
import org.opentripplanner.ext.interactivelauncher.debug.OtpDebugController;
import org.opentripplanner.ext.interactivelauncher.startup.MainView;
import org.opentripplanner.standalone.OTPMain;

/**
 * This class provides a main method to start a GUI which can start OTPMain.
 * <p>
 * The UI allows the user to select the OTP configuration dataset. The list of data locations is
 * created by searching the root data source directory.
 * <p>
 * The user then selects what he/she wants OTP to do.
 * The settings are stored in the
 * <code>.interactive_otp_main.json</code> file in the folder InteractiveOtpMain is started.
 * The settings from the last run are loaded the next time InteractiveOtpMain is started.
 */
public class InteractiveOtpMain {

  private Model model;

  public static void main(String[] args) {
    new InteractiveOtpMain().run();
  }

  private void run() {
    this.model = Model.load();
    MainView frame = new MainView(new Thread(this::startOtp)::start, model.getStartupModel());
    frame.start();
  }

  private void startOtp() {
    model.save();
    startDebugControllerAndSetupRequestInterceptor();

    System.out.println("Start OTP: " + model + "\n");
    OTPMain.main(model.getStartupModel().asOtpArgs());
  }

  private void startDebugControllerAndSetupRequestInterceptor() {
    new OtpDebugController(model).start();
    InteractiveLauncherModule.setRequestInterceptor(model.getRaptorDebugModel());
  }
}
