package org.opentripplanner.raptor.api.model;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;

final class RaptorValueFormatter {

  private static final DecimalFormatSymbols DECIMAL_SYMBOLS = new DecimalFormatSymbols();

  static {
    DECIMAL_SYMBOLS.setDecimalSeparator('.');
    DECIMAL_SYMBOLS.setGroupingSeparator('_');
    DECIMAL_SYMBOLS.setMinusSign('-');
  }

  // In general DecimalFormat is not thread-safe, but we are not changing the state here,
  // so this is ok. The 'format' is not changing the state.
  private static final DecimalFormat FORMAT_CENTI = new DecimalFormat("#,##0.0#", DECIMAL_SYMBOLS);
  private static final DecimalFormat FORMAT_INT = new DecimalFormat("#,##0", DECIMAL_SYMBOLS);

  /** Format integers in centi units like 1234 => 12.34.  */
  static String format(int value) {
    return FORMAT_INT.format(value);
  }

  /** Format integers in centi units like 1234 => 12.34.  */
  static String formatCenti(int value) {
    if (value % 100 == 0) {
      value /= 100;
      return FORMAT_INT.format(value);
    }
    if (Math.abs(value) >= 1_000_000) {
      return FORMAT_INT.format(value / 100);
    }
    return FORMAT_CENTI.format(value / 100.0);
  }

  static int parse(String valueString) {
    try {
      return FORMAT_INT.parse(valueString).intValue();
    } catch (ParseException e) {
      throw new IllegalArgumentException("Not a valid Raptor value: " + valueString, e);
    }
  }

  static int parseCenti(String valueString) {
    try {
      return (int) Math.round(FORMAT_CENTI.parse(valueString).doubleValue() * 100.0);
    } catch (ParseException e) {
      throw new IllegalArgumentException("Not a valid Raptor value: " + valueString, e);
    }
  }
}
