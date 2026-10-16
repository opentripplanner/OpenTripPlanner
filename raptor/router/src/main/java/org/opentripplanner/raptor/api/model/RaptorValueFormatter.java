package org.opentripplanner.raptor.api.model;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;

/**
 * Formats and parses Raptor values, like cost and number of transfers, using a fixed format that
 * does not depend on the default locale.
 * <p>
 * <b>Thread-safety:</b> This class is thread-safe. {@link DecimalFormat} is NOT thread-safe, not
 * even for {@code format(..)}, because it lazily updates internal state while formatting. Sharing
 * one instance between threads can fail with a {@link NullPointerException} or produce corrupt
 * output. Therefore, each thread uses its own {@link DecimalFormat} instances. Never replace them
 * with shared static instances.
 */
final class RaptorValueFormatter {

  private static final DecimalFormatSymbols DECIMAL_SYMBOLS = new DecimalFormatSymbols();

  static {
    DECIMAL_SYMBOLS.setDecimalSeparator('.');
    DECIMAL_SYMBOLS.setGroupingSeparator('_');
    DECIMAL_SYMBOLS.setMinusSign('-');
  }

  // ThreadLocal is ok here, because formatting is only used for logging, debugging and toString.
  // It is not on a hot path.
  private static final ThreadLocal<DecimalFormat> FORMAT_CENTI = ThreadLocal.withInitial(() ->
    new DecimalFormat("#,##0.0#", DECIMAL_SYMBOLS)
  );
  private static final ThreadLocal<DecimalFormat> FORMAT_INT = ThreadLocal.withInitial(() ->
    new DecimalFormat("#,##0", DECIMAL_SYMBOLS)
  );

  /** Format integers in centi units like 1234 => 12.34.  */
  static String format(int value) {
    return FORMAT_INT.get().format(value);
  }

  /** Format integers in centi units like 1234 => 12.34.  */
  static String formatCenti(int value) {
    if (value % 100 == 0) {
      value /= 100;
      return FORMAT_INT.get().format(value);
    }
    if (Math.abs(value) >= 1_000_000) {
      return FORMAT_INT.get().format(value / 100);
    }
    return FORMAT_CENTI.get().format(value / 100.0);
  }

  static int parse(String valueString) {
    try {
      return FORMAT_INT.get().parse(valueString).intValue();
    } catch (ParseException e) {
      throw new IllegalArgumentException("Not a valid Raptor value: " + valueString, e);
    }
  }

  static int parseCenti(String valueString) {
    try {
      return (int) Math.round(FORMAT_CENTI.get().parse(valueString).doubleValue() * 100.0);
    } catch (ParseException e) {
      throw new IllegalArgumentException("Not a valid Raptor value: " + valueString, e);
    }
  }
}
