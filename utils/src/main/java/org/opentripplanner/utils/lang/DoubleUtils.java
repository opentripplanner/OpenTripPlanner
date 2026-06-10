package org.opentripplanner.utils.lang;

import static org.opentripplanner.utils.lang.OtpNumberFormat.formatTwoDecimals;

import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.annotation.Nullable;

public class DoubleUtils {

  /**
   * Round to a decimal number with 1 digits precision
   */
  public static double roundToZeroDecimals(double value) {
    return roundToNDecimals(value, 0);
  }

  /**
   * Round to a decimal number with 1 digits precision
   */
  public static double roundTo1Decimal(double value) {
    return roundToNDecimals(value, 1);
  }

  /**
   * Round to a decimal number with 2 digits precision
   */
  public static Double roundTo2Decimals(@Nullable Double value) {
    return value == null ? null : roundTo2Decimals(value.doubleValue());
  }

  /**
   * Round to a decimal number with 2 digits precision
   */
  public static double roundTo2Decimals(double value) {
    return roundToNDecimals(value, 2);
  }

  /**
   * Round to a decimal number with 3 digits precision
   */
  public static double roundTo3Decimals(double value) {
    return roundToNDecimals(value, 3);
  }

  /**
   * Round to a decimal number with 4 digits precision
   */
  public static double roundTo4Decimals(double value) {
    return roundToNDecimals(value, 4);
  }

  /**
   * Useful for coordinates, round of to ~ 1 cm.
   */
  public static Double roundTo7Decimals(@Nullable Double value) {
    return value == null ? null : roundTo7Decimals(value.doubleValue());
  }

  /**
   * Useful for coordinates, round of to ~ 1 cm.
   */
  public static double roundTo7Decimals(double value) {
    return roundToNDecimals(value, 7);
  }

  /**
   * Round {@code value} to the nearest multiple of {@code step}, with ties broken
   * towards the even neighbour ({@link RoundingMode#HALF_EVEN}, a.k.a. banker's rounding).
   * This is the IEEE 754 default and avoids systematic bias when many rounded values
   * are summed or averaged. BigDecimal is used so that the input is treated as an exact
   * decimal and ties are detected reliably (e.g. {@code 1.85 / 0.1} is exactly 18.5
   * rather than drifting through IEEE-754 representation of 0.1).
   */
  public static double roundToStep(double value, double step) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      return value;
    }
    return BigDecimal.valueOf(value)
      .divide(BigDecimal.valueOf(step), 0, RoundingMode.HALF_EVEN)
      .multiply(BigDecimal.valueOf(step))
      .doubleValue();
  }

  /**
   * Compare two doubles for equality - this is equivalent of
   * <pre>
   * Double.compare(a, b) == 0
   * </pre>
   */
  public static boolean doubleEquals(double a, double b) {
    return Double.compare(a, b) == 0;
  }

  /**
   * Throws an exception if the value is less than zero.
   */
  public static double requireNonNegative(double value) {
    if (value < 0) {
      throw new IllegalArgumentException("Value is required to be non-negative, but was: " + value);
    }
    return value;
  }

  public static double requireInRange(double value, double min, double max) {
    return requireInRange(value, min, max, "value");
  }

  public static double requireInRange(double value, double min, double max, String field) {
    if (value < min || value > max) {
      throw new IllegalArgumentException(
        "The %s is not in range[%s, %s]: %s".formatted(
          field,
          formatTwoDecimals(min),
          formatTwoDecimals(max),
          formatTwoDecimals(value)
        )
      );
    }
    return value;
  }

  /* private methods */

  private static double roundToNDecimals(double value, int decimals) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      return value;
    }
    return BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).doubleValue();
  }
}
