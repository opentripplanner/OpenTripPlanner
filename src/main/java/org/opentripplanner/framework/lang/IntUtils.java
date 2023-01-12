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

  /**
   * Convert a hex string to a human-readable decimal number like this:
   * <pre>
   *    '0'  ->  0
   *    '1'  ->  1
   *    'f'  ->  15
   *   '10'  ->  100
   *   'f0'  ->  1500
   *   'ff'  ->  1515
   * 'a3f7'  ->  10031507
   * </pre>
   * @throws NullPointerException if the given value is {@code null}
   * @throws IllegalArgumentException if the value is more than 4 characters or not a hex number.
   */
  public static int hexToReadableInt(String value) {
    if (value.length() > 4) {
      throw new IllegalArgumentException(
        "The value have too many characters(max 4): '" + value + "'"
      );
    }
    if (!value.matches("^[\\da-fA-F]+$")) {
      throw new IllegalArgumentException("The value is not a hex string: '" + value + "'");
    }
    int r = 0;
    for (int i = 0; i < value.length(); ++i) {
      r *= 100;
      r += Integer.parseInt(value.substring(i, i + 1), 16);
    }
    return r;
  }
}
