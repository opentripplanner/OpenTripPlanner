package org.opentripplanner.framework.model;

import static java.lang.Math.abs;

import java.util.Locale;
import org.opentripplanner.framework.lang.DoubleUtils;
import org.opentripplanner.framework.lang.IntUtils;

/**
 * This utility can be used to perform sanity checks on common number types. It will also normalize
 * the numbers. Supported types are:
 * <ul>
 *   <li>cost - A generalized-cost</li>
 *   <li>reluctance - Reluctance or factor.</li>
 *   <li>slack - Time/slack/duration. Unit: seconds (s)</li>
 *   <li>speed - Unit: meters per second (m/s)</li>
 *   <li>acceleration - Unit: meters per second squared (m/s^2)</li>
 *   <li>ratio - Unit: meters per second squared (m/s^2)</li>
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
   * Reluctance of factor from zero(0) to positive infinitive.
   * Number of decimals used are: 2 for values less than 2.0, 1 for values less than 10.0, and
   * zero for values above 10.0.
   * <p>
   * Unit: Human cost per second of actual time (scalar)
   */
  public static double reluctance(double value) {
    return normalizedFactor(value, 0.0, Double.MAX_VALUE);
  }

  /**
   * Normalized factor in given range between {@param minValue} and {@param maxValue}.
   * Number of decimals used are:
   * <ul>
   *   <li>2 decimals for absolute value less than 2. Example: -1.99 and 0.01</li>
   *   <li>1 decimal for absolute value less than 10. Example: 2.0 and 9.9</li>
   *   <li>zero decimals for absolute values above 10.  Example: -10 and 10</li>
   * </ul>
   * <p>
   * Unit: scalar
   */
  public static double normalizedFactor(double value, double minValue, double maxValue) {
    DoubleUtils.requireInRange(value, minValue, maxValue);
    if (abs(value) < 2.0) {
      return DoubleUtils.roundTo2Decimals(value);
    }
    if (abs(value) < 10.0) {
      return DoubleUtils.roundTo1Decimal(value);
    }
    return DoubleUtils.roundToZeroDecimals(value);
  }

  /**
   * Convert a factor to string using the normalized number of digits.
   */
  public static String factorToString(double value) {
    if (abs(value) < 2.0) {
      return String.format(Locale.ROOT, "%.2f", value);
    }
    if (abs(value) < 10.0) {
      return String.format(Locale.ROOT, "%.1f", value);
    }
    return String.format(Locale.ROOT, "%.0f", value);
  }

  /**
   * If given input value is {@code null}, then return {@code null}, if not
   * verify value, see {@link #normalizedFactor(double, double, double)}.
   */
  public static Double normalizedOptionalFactor(Double value, double minValue, double maxValue) {
    return (value == null) ? null : normalizedFactor(value, minValue, maxValue);
  }

  /**
   * Amount of time/slack/duration in seconds - A constant amount of time.
   */
  public static int duration(int seconds) {
    return IntUtils.requireNotNegative(seconds);
  }

  /**
   * Sanity check and normalize of the speed:
   * <ol>
   *   <li>Less than 0.0 -> throw Illegal argument exception.</li>
   *   <li>[0.0 .. 0.1) -> round up to 0.1 - Avoid divide by zero</li>
   *   <li>[0.1 .. 2) -> round with 2 decimals</li>
   *   <li>[2 .. 10) -> round with 1 decimal</li>
   *   <li>[10 .. 1 mach(340 m/s^2)) -> round with zero decimals</li>
   *   <li>Greater than 1 Mach(speed of sound) -> throw Illegal argument exception</li>
   * </ol>
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
    if (metersPerSecond < 2.0) {
      return DoubleUtils.roundTo2Decimals(metersPerSecond);
    }
    if (metersPerSecond < 10.0) {
      return DoubleUtils.roundTo1Decimal(metersPerSecond);
    }
    if (metersPerSecond > ONE_MACH) {
      throw new IllegalArgumentException(
        "Are you flying in supersonic speed: " + metersPerSecond + " m/s"
      );
    }
    return DoubleUtils.roundToZeroDecimals(metersPerSecond);
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
