package org.opentripplanner.gtfs.error;

/** Represents a problem parsing an integer field of GTFS feed. */
public class IntParseError extends GTFSError {

    public IntParseError(String file, long line, String field) {
        super(file, line, field);
    }

    @Override public String getMessage() {
        return String.format("Error parsing int from string. This does not look like an integer.");
    }

}
