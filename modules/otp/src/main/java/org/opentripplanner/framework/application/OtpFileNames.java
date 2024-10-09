package org.opentripplanner.framework.application;

/**
 * This class defile a list of Otp file names. These are used in various
 * places, hence need to be defined in a common place.
 */
public class OtpFileNames {

  public static final String OTP_CONFIG_FILENAME = "otp-config.json";
  public static final String BUILD_CONFIG_FILENAME = "build-config.json";
  public static final String ROUTER_CONFIG_FILENAME = "router-config.json";

  /**
   * Check if a file is a config file using the configuration file name. This method returns {@code
   * true} if the file match {@code (otp|build|router)-config.json}.
   */
  public static boolean isConfigFile(String filename) {
    return (
      OTP_CONFIG_FILENAME.equals(filename) ||
      BUILD_CONFIG_FILENAME.equals(filename) ||
      ROUTER_CONFIG_FILENAME.equals(filename)
    );
  }
}
