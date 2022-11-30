package org.opentripplanner.framework.application;

import java.util.Arrays;
import java.util.IllegalFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * When an error situation happens inside OTP this exception can be used to terminate OTP or the
 * current request (http server). The message should contain enough information to the user to fix
 * the problem. Before OTP terminates the message will be logged WITHOUT any stacktrace.
 * <p>
 * Typical use cases for this exception is:
 * <ul>
 *     <li>The configuration is not correct.</li>
 *     <li>
 *         The input data is missing or have severe errors and can not be processed. The later
 *         may be difficult to verify, in which case another exception would be a better choice.
 *      </li>
 *      <li>The command line parameters don´t match the input files.</li>
 *      <li>
 *          When a exception occurs it is preferable to catch it at the right level, were most
 *          context information is available. At this point the exception should be logged
 *          with a stacktrace. Then this exception can be re-thrown to terminate the application
 *          or request.
 *      </li>
 * </ul>
 * <p>
 * Do not use this exception to terminate OTP in case of an unknown/unexpected event, then
 * the {@link IllegalArgumentException}, {@link IllegalStateException} or {@link RuntimeException}
 * should be used. These will be logged with a stacktrace.
 */
public class OtpAppException extends RuntimeException {

  private static final Logger LOG = LoggerFactory.getLogger(OtpAppException.class);

  public OtpAppException(String message) {
    super(message);
  }

  /**
   * This method uses {@link String#format(String, Object...)} to format the message.
   */
  public OtpAppException(String message, Object... args) {
    super(format(message, args));
  }

  public static String format(String message, Object... args) {
    try {
      return String.format(message, args);
    } catch (IllegalFormatException e) {
      LOG.error(e.getMessage(), e);
      return message + " " + Arrays.toString(args);
    }
  }
}
