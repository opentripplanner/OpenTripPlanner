package org.opentripplanner.framework.error;

import java.util.Locale;

/**
 * A generic representation of an error. The error should have a code used to group the same
 * type of errors together. To avoid filling up memory with error strings during graph build
 * we store errors in memory "decomposed". The {@link #messageTemplate()} and
 * {@link #messageArguments()} is used to construct the message. Use the {@link Locale#ROOT}
 * when constructing the message - we only support english with SI formatting.
 */
public interface OtpError {
  /**
   * An error code used to identify the error type. This is NOT an enum, but feel free
   * to use an inum in the implementation, then use the enum NAME as the code or
   * enum TYPE:NAME. Then name should be unique within OTP.
   */
  String errorCode();

  /**
   * The string template used to create a human-readable error message. Use the
   * {@link String#format(Locale, String, Object...)} format.
   */
  String messageTemplate();

  /**
   * The arguments to inject into the message.
   */
  Object[] messageArguments();

  /**
   * Construct a message.
   */
  default String message() {
    return String.format(Locale.ROOT, messageTemplate(), messageArguments());
  }

  /**
   * Factory method to create an OTPError.
   */
  static OtpError of(String errorCode, String messageTemplate, Object... messageArguments) {
    return new DefaultOtpError(errorCode, messageTemplate, messageArguments);
  }
}
