package org.opentripplanner.core.model.basic;

/**
 * This interface is used to get a normalized distance between to numeric values/scalars. The value should be between
 * 0.0 and 1.0.
 * <p>
 * How to calculate the distance between a and b:
 * <ol>
 *  <li>
 *      Normalize both values {@code (a, b) -> (a', b')} so they are between 0.0 and 1.0. If the scale is none
 *      liniear, then make sure to distribute the values between 0.0 and 1.0 roughly according to the shape of the
 *      function.
 *  </li>
 *  <li>
 *      The return the absolute value of {@code a' - b'}
 *  </li>
 * <ol>
 */
public interface NormalizedDistance<T> {
  float normalizedDistance(T other);

  /// Calculate the normalized distance between two enum values.
  public static <E extends Enum<E>> float calculateNormalizedDistance(E a, E b) {
    float size = a.getDeclaringClass().getEnumConstants().length - 1;
    return a == b ? 0 : Math.abs(a.ordinal() - b.ordinal()) / size;
  }
}
