package org.opentripplanner.util.lang;

import java.math.BigDecimal;
import java.math.RoundingMode;
import javax.annotation.Nullable;

public class DoubleUtils {

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

  public static double roundToNDecimals(double value, int places) {
    if (Double.isNaN(value) || Double.isInfinite(value)) {
      return value;
    }
    return BigDecimal.valueOf(value).setScale(places, RoundingMode.HALF_UP).doubleValue();
  }
}
