package org.opentripplanner.core.model.basic;

/**
 * This interface is used to get a normalized distance between two numeric values/scalars. The distance
 * between two scalars is between 0.0 and 1.0, and symmetric. A composite type (a sum of weighted
 * distances) may exceed 1.0, and may be asymmetric, see {@link #normalizedDistance(Object)}.
 * <p>
 * How to calculate the distance between two scalars a and b:
 * <ol>
 *  <li>
 *      Normalize both values {@code (a, b) -> (a', b')} so they are between 0.0 and 1.0. If the scale is none
 *      liniear, then make sure to distribute the values between 0.0 and 1.0 roughly according to the shape of the
 *      function.
 *  </li>
 *  <li>
 *      Return the absolute value of {@code a' - b'}
 *  </li>
 * </ol>
 */
public interface NormalizedDistance<T> {
  /**
   * Calculate the distance from this to {@code other}.
   * <p>
   * The distance can be <b>asymmetric</b>: {@code a.normalizedDistance(b)} is not necessarily equal
   * to {@code b.normalizedDistance(a)}. An implementation may measure how bad it is to use
   * {@code other} in place of {@code this}, for example when a value that is too low costs more
   * than a value that is too high. Such an implementation must document which of the two is which,
   * and the caller must use the right order.
   */
  double normalizedDistance(T other);
}
