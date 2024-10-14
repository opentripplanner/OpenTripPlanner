package org.opentripplanner.framework.lang;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class OtpNumberFormat {

  private static final String NULL_VALUE = "null";
  private static final DecimalFormatSymbols DECIMAL_SYMBOLS = DecimalFormatSymbols.getInstance(
    Locale.ROOT
  );

  private DecimalFormat integerFormat;
  private DecimalFormat decimalFormat;
  private DecimalFormat coordinateFormat;

  /**
   * Used to format integer cost types used in OTP in "transit seconds", not centi-seconds as used
   * by Raptor.
   */
  public static String formatCost(int cost) {
    return "$" + cost;
  }

  /** Used to format integer cost types like generalized-cost used by Raptor. */
  public static String formatCostCenti(int cost) {
    if (Math.abs(cost) >= 1_000_000 || cost % 100 == 0) {
      return "$" + cost / 100;
    }
    return String.format(Locale.ROOT, "$%.2f", cost / 100.0);
  }

  /** Used to format integer cost types with a given unit. */
  public static String formatCost(int cost, String unit) {
    return formatCostCenti(cost) + unit;
  }

  public String formatCoordinate(Number value) {
    if (coordinateFormat == null) {
      coordinateFormat = new DecimalFormat("#0.0####", DECIMAL_SYMBOLS);
    }
    // This need to be null-safe, because one of the coordinates in
    // #addCoordinate(String name, Number lat, Number lon) could be null.
    return value == null ? "null" : coordinateFormat.format(value);
  }

  public String formatNumber(Number value, String unit) {
    return formatNumber(value) + unit;
  }

  public String formatNumber(Number value) {
    if (value == null) {
      return NULL_VALUE;
    }

    if (value instanceof Integer || value instanceof Long || value instanceof BigInteger) {
      if (integerFormat == null) {
        integerFormat = new DecimalFormat("#,##0", DECIMAL_SYMBOLS);
      }
      return integerFormat.format(value);
    }

    if (decimalFormat == null) {
      decimalFormat = new DecimalFormat("#,##0.0##", DECIMAL_SYMBOLS);
    }
    return decimalFormat.format(value);
  }

  public static String formatZeroDecimal(double value) {
    return formatDecimals(value, new DecimalFormat("#,##0", DECIMAL_SYMBOLS));
  }

  public static String formatTwoDecimals(double value) {
    return formatDecimals(value, new DecimalFormat("#,##0.0#", DECIMAL_SYMBOLS));
  }

  public static String formatDecimals(double value, DecimalFormat decimalFormat) {
    if (value < -100_000_000 || value > 100_000_000) {
      return Double.toString(value);
    }
    return decimalFormat.format(value);
  }
}
