package org.opentripplanner.framework.lang;

import javax.annotation.Nullable;

public class ArrayUtils {

  /**
   * Return {@code true} if array has at least one element. Return {@code false} is array is
   * {@code null} or has zero length.
   */
  public static <T> boolean hasContent(@Nullable T[] array) {
    return array != null && array.length > 0;
  }
}
