package org.opentripplanner.framework.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Exception is used to signal that the current (HTTP) request has timed out.
 */
public class OTPRequestTimeoutException extends RuntimeException {

  private static final Logger LOG = LoggerFactory.getLogger(OTPRequestTimeoutException.class);
  public static final String MESSAGE = "TIMEOUT! The request is too resource intensive.";

  @Override
  public String getMessage() {
    return MESSAGE;
  }

  /**
   * The Grizzly web server is configured with a transaction timeout and will set the interrupt
   * flag on the current thread. OTP does not have many blocking operations which check the
   * interrupted flag, so instead we need to do the check manually. The check has a small
   * performance overhead so try to place the check in the beginning of significantly big block of
   * calculations.
   */
  public static void checkForTimeout() {
    // We call yield() to allow monitoring thread to interrupt current thread. If this work or not
    // is hard to document and test, and the result would only apply the environment tested - but
    // it does not hurt. The logic does not relay on the yield() to work, it only aborts sooner.
    Thread.yield();

    if (Thread.currentThread().isInterrupted()) {
      logDebug();
      throw new OTPRequestTimeoutException();
    }
  }

  /**
   * This method can be used to investigate where the execution aborts, this is interesting
   * when debugging this feature. Breakpoint is no very useful, because they interfere with the
   * timeout logic. To make the code timeout use the
   * {@link org.opentripplanner.framework.time.TimeUtils#busyWaitOnce(int)} method.
   */
  private static void logDebug() {
    if (LOG.isDebugEnabled()) {
      var st = Thread.currentThread().getStackTrace();
      if (st.length > 5) {
        LOG.debug(
          "checkForTimeout() - Thread {} is interrupted!\n    {}\n    {}\n    {}",
          Thread.currentThread().getName(),
          st[3],
          st[4],
          st[5]
        );
      }
    }
  }
}
