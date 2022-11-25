package org.opentripplanner.framework.text;

import java.util.Locale;

/**
 * Utility functions for logging output.
 */
public class FileSizeToTextConverter {

  public static String fileSizeToString(long n) {
    if (n >= 1_000_000_000) {
      return String.format(Locale.ROOT, "%.1f GB", n / 1_000_000_000.0);
    }
    if (n >= 1_000_000) {
      return String.format(Locale.ROOT, "%.1f MB", n / 1_000_000.0);
    }
    if (n >= 1_000) {
      return String.format(Locale.ROOT, "%d kb", n / 1_000);
    }
    if (n == 1) {
      return String.format(Locale.ROOT, "%d byte", n);
    } else {
      return String.format(Locale.ROOT, "%d bytes", n);
    }
  }
}
