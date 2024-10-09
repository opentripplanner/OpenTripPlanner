package org.opentripplanner.framework.lang;

import static org.opentripplanner.framework.lang.OtpNumberFormat.formatTwoDecimals;

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
   * Compare two doubles for equality - this is equivalent of
   * <pre>
   * Double.compare(a, b) == 0
   * </pre>
   */
  public static boolean doubleEquals(double a, double b) {
    return Double.compare(a, b) == 0;
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
