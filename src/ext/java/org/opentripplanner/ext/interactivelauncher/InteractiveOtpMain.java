package org.opentripplanner.ext.interactivelauncher;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import java.util.Map;
import org.opentripplanner.ext.interactivelauncher.views.MainView;
import org.opentripplanner.standalone.OTPMain;
import org.slf4j.LoggerFactory;


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
  private Model model = new Model();

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
    enableDebugLogging(model.getDebugLogging());

    System.out.println("Start OTP: " + model);
    OTPMain.main(model.asOtpArgs());
  }

  private static void enableDebugLogging(Map<String, Boolean> debugLogging) {
    LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    for (Logger log : context.getLoggerList()) {
      if(debugLogging.getOrDefault(log.getName(), false))  {
        log.setLevel(Level.DEBUG);
      }
    }
  }
}
