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

  /**
   * Verify String value is NOT {@code null}, empty or only whitespace.
   *
   * @throws IllegalArgumentException if given value is {@code null}, empty or only whitespace.
   */
  public static String assertHasValue(String value) {
    return assertHasValue(value, "");
  }

  /**
   * Verify String value is NOT {@code null}, empty or only whitespace.
   * @param errorMessage optional custom message to be displayed as the exception message.
   * @param placeholders optional placeholders used in the error message format.
   * @throws IllegalArgumentException if given value is {@code null}, empty or only whitespace.
   */
  public static String assertHasValue(String value, String errorMessage, Object... placeholders) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(
        errorMessage.formatted(placeholders) +
        " [Value cannot be null, empty or just whitespace: " +
        (value == null ? "null]" : "'" + value + "']")
      );
    }
    return value;
  }
}
