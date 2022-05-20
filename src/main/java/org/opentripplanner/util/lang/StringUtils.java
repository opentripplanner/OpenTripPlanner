package org.opentripplanner.util.lang;

/**
 * OTP String utils extending the Java lang String...
 */
public class StringUtils {

  private StringUtils() {}

  /** true if the given text is not {@code null} or has at least one none white-space character. */
  public static boolean hasValue(String text) {
    return text != null && !text.isBlank();
  }
}
