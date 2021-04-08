package org.opentripplanner.model.base;

import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

class NumberFormat {

  private static final String NULL_VALUE = "null";
  private static final DecimalFormatSymbols DECIMAL_SYMBOLS = DecimalFormatSymbols.getInstance(
      Locale.US);

  private DecimalFormat integerFormat;
  private DecimalFormat decimalFormat;
  private DecimalFormat coordinateFormat;


  String formatCoordinate(Number value) {
    if (coordinateFormat == null) {
      coordinateFormat = new DecimalFormat("#0.0####", DECIMAL_SYMBOLS);
    }
    // This need to be null-safe, because one of the coordinates in
    // #addCoordinate(String name, Number lat, Number lon) could be null.
    return value == null ? "null" : coordinateFormat.format(value);
  }

  String formatNumber(Number value, String unit) {
    return formatNumber(value) + unit;
  }


  String formatNumber(Number value) {
    if (value == null) { return NULL_VALUE; }

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
}
