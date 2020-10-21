package org.opentripplanner.util;

public class AssertUtils {

  /** Private constructor to prevent creation of this utility class */
  private AssertUtils() {}

  /**
   * Verify String value is NOT {@code null}, empty or only whitespace.
   * @throws IllegalArgumentException if given value is {@code null}, empty or only whitespace.
   */
  public static void assertHasValue(String value) {
    if(value == null || value.isBlank()) {
      throw new IllegalArgumentException("Value can not be null, empty or just whitespace: " +
          (value==null ? "null" : "'" + value + "'"));
    }
  }
}
