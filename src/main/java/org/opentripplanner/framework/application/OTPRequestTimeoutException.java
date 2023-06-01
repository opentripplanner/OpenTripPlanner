package org.opentripplanner.framework.application;

/**
 * This Exception is used to signal that the current (HTTP) request has timed out.
 */
public class OTPRequestTimeoutException extends RuntimeException {

  public static final String MESSAGE = "TIMEOUT! The request is too resource intensive.";

  @Override
  public String getMessage() {
    return MESSAGE;
  }

  /**
   * The Grizzly web server is configured with a transaction timeout and
   * will set the interrupt flag on the current thread. OTP does not have many blocking operations
   * which check the interrupted flag, so instead we need to do the check manually. The check has
   * a small performance overhead so try to place the check in the beginning of significantly big
   * finite block of calculations.
   */
  public static void checkForTimeout() {
    if (Thread.currentThread().isInterrupted()) {
      throw new OTPRequestTimeoutException();
    }
  }
}
