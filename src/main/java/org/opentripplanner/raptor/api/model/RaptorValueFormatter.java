package org.opentripplanner.raptor.api.model;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class RaptorValueFormatter {

  private static final String UNIT_C1 = "C₁";
  private static final String UNIT_C2 = "C₂";
  private static final String UNIT_WAIT_TIME_COST = "wtC₁";
  private static final String UNIT_TRANSFER_PRIORITY = "Tₚ";
  private static final String UNIT_TRANSFERS = "Tₓ";

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

  public static String formatC1(int c1) {
    return UNIT_C1 + formatCenti(c1);
  }

  public static String formatC2(int c2) {
    return UNIT_C2 + c2;
  }

  public static String formatWaitTimeCost(int value) {
    return UNIT_WAIT_TIME_COST + formatCenti(value);
  }

  public static String formatNumOfTransfers(int value) {
    return UNIT_TRANSFERS + FORMAT_INT.format(value);
  }

  public static String formatTransferPriority(int value) {
    return UNIT_TRANSFER_PRIORITY + FORMAT_INT.format(value);
  }

  /** Format integers in centi units like 1234 => 12.34.  */
  private static String formatCenti(int value) {
    if (value % 100 == 0) {
      value /= 100;
      return FORMAT_INT.format(value);
    }
    if (Math.abs(value) >= 1_000_000) {
      return FORMAT_INT.format(value / 100);
    }
    return FORMAT_CENTI.format(value / 100.0);
  }
}
