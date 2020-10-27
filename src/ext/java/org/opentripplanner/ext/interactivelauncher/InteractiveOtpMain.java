package org.opentripplanner.ext.interactivelauncher;

import org.opentripplanner.ext.interactivelauncher.views.MainView;
import org.opentripplanner.standalone.OTPMain;


/**
 * This class provide a main method to start a GUI witch can start OTPMain.
 * <p>
 * The UI allow the user to select a OTP configuration data set. The list of
 * data location is created by searching the provided root directory(ies).
 * <p>
 * The user than select what he/she want OTP to do, the default is to launch
 * the HTTP Server and serve a prebuild graph.
 */
public class InteractiveOtpMain {
  private final CliArguments config;

  public InteractiveOtpMain(CliArguments config) {
    this.config = config;
  }

  public static void main(String[] args) {
    CliArguments config = CliArguments.parseArgs(args);
    if(config.dirs.isEmpty()) {
      printHelp();
    }
    else {
      new InteractiveOtpMain(config).run();
    }
  }

  private void run() {
    MainView frame = new MainView(this::startOtp, config.dirs, config.absolutePathPrefix);
    frame.start();;
  }

  private void startOtp(SetupResult setup) {
    System.out.println("Start OTP: " + setup);
    OTPMain.main(setup.asOtpArgs());
  }

  private static void printHelp() {
    System.out.println();
    System.out.println("Start InteractiveOtpMain with a list of data configuration root directories.");
    System.out.println("The `config-root-dir` and all sub-directories are searched for OTP config");
    System.out.println("files: 'otp-config.json', 'build-config.json', and/or 'build-config.json'.");
    System.out.println("The user then select one of the OTP data config directories for OTP to use.");
    System.out.println();
    System.out.println("    $ java ... InteractiveOtpMain [config-root-dir]+");
    System.out.println("        config-dir-root : The directory to search (recursively) for OTP data");
    System.out.println("                          configuration sources.");
    System.out.println();
    System.out.println();
  }
}
