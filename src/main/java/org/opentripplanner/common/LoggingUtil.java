package org.opentripplanner.common;

import static java.util.Locale.ENGLISH;

/**
 * Utility functions for logging output.
 */
public class LoggingUtil {

    public static String fileSizeToString(long n) {
        if (n >= 1_000_000_000) { return String.format(ENGLISH, "%.1f GB", n/1_000_000_000.0); }
        if (n >= 1_000_000) { return String.format(ENGLISH, "%.1f MB", n/1_000_000.0); }
        if (n >= 1_000) { return String.format(ENGLISH, "%d kb", n/1_000); }
        if (n == 1) { return String.format(ENGLISH, "%d byte", n); }
        else return String.format(ENGLISH, "%d bytes", n);
    }

}
