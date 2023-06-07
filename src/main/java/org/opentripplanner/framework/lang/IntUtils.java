package org.opentripplanner.framework.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A utility class for integer functions.
 */
public final class IntUtils {

  /** The constructor is private to protect this class from being instantiated. */
  private IntUtils() {}

  /**
   * Convert an integer to a String, if the value equals the {@code notSetValue} parameter an empty
   * string is returned.
   */
  public static String intToString(int value, int notSetValue) {
    return value == notSetValue ? "" : Integer.toString(value);
  }

  /**
   * Create a new int array and initialize all values with the given {@code initialValue}.
   */
  public static int[] intArray(int size, int initialValue) {
    int[] array = new int[size];
    Arrays.fill(array, initialValue);
    return array;
  }

  /**
   * Copy int array and shift all values by the given {@code offset}.
   */
  public static int[] shiftArray(int offset, int[] array) {
    int[] a = new int[array.length];
    for (int i = 0; i < array.length; i++) {
      a[i] = array[i] + offset;
    }
    return a;
  }

  /**
   * Concatenate list a and b and convert them to int arrays.
   */
  public static int[] concat(Collection<Integer> a, Collection<Integer> b) {
    List<Integer> all = new ArrayList<>(a);
    all.addAll(b);
    return all.stream().mapToInt(it -> it).toArray();
  }

  public static double standardDeviation(List<Integer> v) {
    double average = v.stream().mapToInt(it -> it).average().orElse(0d);

    double sum = 0.0;
    for (double num : v) {
      sum += Math.pow(num - average, 2);
    }

    return Math.sqrt(sum / v.size());
  }

  public static int requireNotNegative(int value) {
    if (value < 0) {
      throw new IllegalArgumentException("Negative value not expected: " + value);
    }
    return value;
  }

  public static int requireInRange(int value, int minInclusive, int maxInclusive) {
    return requireInRange(value, minInclusive, maxInclusive, "value");
  }

  public static int requireInRange(int value, int minInclusive, int maxInclusive, String field) {
    if (value < minInclusive || value > maxInclusive) {
      throw new IllegalArgumentException(
        "The %s is not in range[%d, %d]: %d".formatted(field, minInclusive, maxInclusive, value)
      );
    }
    return value;
  }
}
