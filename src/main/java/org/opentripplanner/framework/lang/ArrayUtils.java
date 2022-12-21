package org.opentripplanner.framework.lang;

import java.util.function.Function;

public class ArrayUtils {

  private ArrayUtils() {}

  /**
   * Check if all values, mapped using {@param mapper}, of an array are equal.
   */
  public static <T> boolean allValuesEquals(T[] arr, Function<T, Object> mapper) {
    if (arr.length < 2) {
      return true;
    }

    Object reference = mapper.apply(arr[0]);

    for (int i = 1; i < arr.length; i++) {
      if (!reference.equals(mapper.apply(arr[i]))) {
        return false;
      }
    }

    return true;
  }

  /**
   * Check if all values of an array are equal.
   */
  public static boolean allValuesEquals(Object[] arr) {
    return allValuesEquals(arr, Function.identity());
  }
}
