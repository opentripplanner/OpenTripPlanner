package org.opentripplanner.utils.lang;

import javax.annotation.Nullable;

public class ArrayUtils {

  /**
   * Return {@code true} if array has at least one element. Return {@code false} is array is
   * {@code null} or has zero length.
   */
  public static <T> boolean hasContent(@Nullable T[] array) {
    return array != null && array.length > 0;
  }

  /**
   * Creates and returns a new array that contains the elements of the given array
   * in reverse order.
   */
  public static int[] reversedCopy(int[] array) {
    var copy = new int[array.length];
    for (int i = 0; i < array.length; ++i) {
      copy[i] = array[array.length - 1 - i];
    }
    return copy;
  }
}
