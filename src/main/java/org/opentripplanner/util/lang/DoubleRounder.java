package org.opentripplanner.util.lang;

import javax.annotation.Nullable;

public class DoubleRounder {

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
    return roundToNDecimals(value, 10_000_000.0);
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
    return roundToNDecimals(value, 100.0);
  }

  public static double roundToNDecimals(double value, double factor) {
    return Math.round(value * factor) / factor;
  }
}
