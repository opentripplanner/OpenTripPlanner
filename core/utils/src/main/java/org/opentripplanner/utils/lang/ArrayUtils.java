package org.opentripplanner.utils.lang;

import org.jspecify.annotations.Nullable;

public class ArrayUtils {

  /**
   * Return {@code true} if array has at least one element. Return {@code false} is array is
   * {@code null} or has zero length.
   */
  public static <T> boolean hasContent(T @Nullable [] array) {
    return array != null && array.length > 0;
  }
}
