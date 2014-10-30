package org.opentripplanner.gtfs.error;

/** Represents a problem parsing a double-precision floating point field of GTFS feed. */
public class DoubleParseError extends GTFSError {

    public DoubleParseError(String file, long line, String field) {
        super(file, line, field);
    }

    @Override public String getMessage() {
        return "";
    }

}
