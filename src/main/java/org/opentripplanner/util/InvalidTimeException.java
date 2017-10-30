package org.opentripplanner.util;

/**
 * Indicates the "time" is not on the expected format.
 */
public class InvalidTimeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    InvalidTimeException(String timeString, String expectedFormat) {
        super("invalid time: '" + timeString + "', expected format: '" + expectedFormat + "'");
    }
}
