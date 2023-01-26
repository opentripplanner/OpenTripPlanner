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
   * Check if each value in the array is equal to the given {@code value}. If all values
   * are the same and equals to {@code value} or the array is empty this method returns
   * {@code true}.
   */
  public static boolean arrayEquals(int[] array, int value) {
    for (int i : array) {
      if (value != i) {
        return false;
      }
    }
    return true;
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
   * Add a given {@code delta} value to all elements in an array, except {@code notSet}
   * elements.
   */
  public static int[] arrayPlus(int[] array, int delta) {
    if (array.length == 0) {
      return array;
    }
    int[] newArray = new int[array.length];
    for (int i = 0; i < array.length; i++) {
      newArray[i] = array[i] + delta;
    }
    return newArray;
  }
}
