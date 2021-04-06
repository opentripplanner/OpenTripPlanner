package org.opentripplanner.common;

/**
 * Utility functions for logging output.
 */
public class LoggingUtil {

    public static String fileSizeToString(long n) {
        if (n >= 1_000_000_000) { return String.format("%.1f GB", n/1_000_000_000.0); }
        if (n >= 1_000_000) { return String.format("%.1f MB", n/1_000_000.0); }
        if (n >= 1_000) { return String.format("%d kb", n/1_000); }
        if (n == 1) { return String.format("%d byte", n); }
        else return String.format("%d bytes", n);
    }

}
