package org.opentripplanner.utils.lang;

import static java.lang.Math.abs;

import java.util.Locale;

/**
 * This utility can be used to perform sanity checks on common number types. It will also normalize
 * the numbers. Supported types are:
 * <ul>
 *   <li>cost - A generalized-cost</li>
 *   <li>reluctance - Reluctance or factor.</li>
 *   <li>slack - Time/slack/duration. Unit: seconds (s)</li>
 *   <li>speed - Unit: meters per second (m/s)</li>
 *   <li>acceleration - Unit: meters per second squared (m/s^2)</li>
 *   <li>ratio - Unit: scalar</li>
 * </ul>
 *
 * @deprecated Convert primitive types to type-safe value objects. The need for this class should
 *             then go away.
 */
@Deprecated
public class Units {

  private static final double ONE_MACH = 340.0;

  /** This is a utility class, it is never instantiated; Hence, private constructor. */
  private Units() {}

  /**
   * Reluctance or factor from zero(0) to positive infinity.
   * Bucketed so that close-but-distinct values share a single canonical representation:
   * step 0.1 below 3.0, step 0.5 in [3.0, 10.0), step 1.0 at or above 10.0. See
   * {@link #normalizedFactor(double, double, double)} for the rationale.
   * <p>
   * Unit: Human cost per second of actual time (scalar)
   */
  public static double reluctance(double value) {
    return normalizedFactor(value, 0.0, Double.MAX_VALUE);
  }

  /**
   * Normalized factor in given range between {@param minValue} and {@param maxValue}.
   * Bucketing is tiered so that fewer distinct request-cache keys are produced from
   * close-but-distinct client-supplied values:
   * <ul>
   *   <li>step 0.1 for absolute value less than 3.0. Example: 1.94 -> 1.9, 1.96 -> 2.0</li>
   *   <li>step 0.5 for absolute value in [3.0, 10.0). Example: 3.2 -> 3.0, 3.3 -> 3.5</li>
   *   <li>step 1.0 for absolute value at or above 10.0. Example: 10.4 -> 10, 10.6 -> 11</li>
   * </ul>
   * Ties are broken to the even neighbour (HALF_EVEN, banker's rounding), implemented
   * via BigDecimal so that decimal inputs are pinned exactly and the tie detection is
   * not subject to IEEE-754 drift.
   * <p>
   * Unit: scalar
   */
  public static double normalizedFactor(double value, double minValue, double maxValue) {
    DoubleUtils.requireInRange(value, minValue, maxValue);
    return DoubleUtils.roundToStep(value, factorStep(value));
  }

  private static double factorStep(double value) {
    double abs = abs(value);
    if (abs < 3.0) {
      return 0.1;
    }
    if (abs < 10.0) {
      return 0.5;
    }
    return 1.0;
  }

  /**
   * Convert a factor to string using the same precision as the bucket grid in
   * {@link #normalizedFactor(double, double, double)}: 1 decimal below 10.0, 0 decimals
   * at or above 10.0.
   */
  public static String factorToString(double value) {
    if (abs(value) < 10.0) {
      return String.format(Locale.ROOT, "%.1f", value);
    }
    return String.format(Locale.ROOT, "%.0f", value);
  }

  /**
   * Amount of time/slack/duration in seconds - A constant amount of time.
   */
  public static int duration(int seconds) {
    return IntUtils.requireNotNegative(seconds);
  }

  /**
   * Sanity check and bucketed normalization of the speed. Bucketing collapses close-but-
   * distinct client-supplied values into a single canonical representation so that fewer
   * distinct request-cache keys are produced (e.g. walk speeds 1.38 and 1.39 both snap
   * to 1.40):
   * <ol>
   *   <li>Less than 0.0 -> throw IllegalArgumentException.</li>
   *   <li>[0.0 .. 0.1) -> round up to 0.1 - Avoid divide by zero.</li>
   *   <li>[0.1 .. 2) -> step 0.05 m/s.</li>
   *   <li>[2 .. 10) -> step 0.1 m/s.</li>
   *   <li>[10 .. 1 mach (340 m/s)) -> step 1.0 m/s.</li>
   *   <li>Greater than 1 Mach (speed of sound) -> throw IllegalArgumentException.</li>
   * </ol>
   * Ties are broken to the even neighbour (HALF_EVEN, banker's rounding), via
   * BigDecimal so that decimal inputs are pinned exactly and the tie detection is not
   * subject to IEEE-754 drift.
   *
   * <p>
   * Unit: meters per second (m/s)
   */
  public static double speed(double metersPerSecond) {
    if (metersPerSecond < 0.0) {
      throw new IllegalArgumentException(
        "Negative speed not expected: " + metersPerSecond + " m/s"
      );
    }
    if (metersPerSecond < 0.1) {
      return 0.1;
    }
    if (metersPerSecond > ONE_MACH) {
      throw new IllegalArgumentException(
        "Are you flying in supersonic speed: " + metersPerSecond + " m/s"
      );
    }
    return DoubleUtils.roundToStep(metersPerSecond, speedStep(metersPerSecond));
  }

  private static double speedStep(double metersPerSecond) {
    if (metersPerSecond < 2.0) {
      return 0.05;
    }
    if (metersPerSecond < 10.0) {
      return 0.1;
    }
    return 1.0;
  }

  /**
   * Acceleration or deceleration. Must be greater or equals to zero.
   * <p>
   * Unit: meters per second squared (m/s^2)
   */
  public static double acceleration(double metersPerSecondSquared) {
    if (metersPerSecondSquared < 0.0) {
      throw new IllegalArgumentException(
        "Negative acceleration or deceleration not expected: " + metersPerSecondSquared
      );
    }
    if (metersPerSecondSquared < 0.1) {
      return 0.1;
    }
    return DoubleUtils.roundTo1Decimal(metersPerSecondSquared);
  }

  /**
   * A number between 0.000 and 1.000 (0% - 100%) - theoretical values above 1 can happen,
   * but is not allowed here - use {@link #reluctance(double)} instead.
   * <p>
   * Unit: scalar
   */
  public static double ratio(double value) {
    return DoubleUtils.requireInRange(DoubleUtils.roundTo3Decimals(value), 0.0, 1.0);
  }

  /**
   * A count is a number of items. Must be in range zero(0) to given {@code maxValue}.
   * <p>
   * Unit: scalar
   */
  public static int count(int value, int maxValue) {
    return IntUtils.requireInRange(value, 0, maxValue);
  }
}
