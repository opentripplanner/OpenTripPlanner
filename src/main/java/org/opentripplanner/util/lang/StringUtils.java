package org.opentripplanner.util.lang;

import java.util.Arrays;

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

  /**
   * Pad the {@code buffer} so that the length equals the new length. The {@code ch} is appended as
   * many times as required. If the {@code buffer} length is equals or longer then the
   * {@code newLenght} then the {@code buffer} is returned unchanged.
   *
   * @return the given buffer input for convenient chaining.
   */
  public static StringBuilder pad(StringBuilder buffer, char ch, int newLength) {
    while (buffer.length() < newLength) {
      buffer.append(ch);
    }
    return buffer;
  }

  /**
   * Create a new String with the given {@code length} and all characters set to the given {@code ch}
   * character.
   */
  public static String fill(char ch, int length) {
    char[] array = new char[length];
    Arrays.fill(array, ch);
    return new String(array);
  }
}
