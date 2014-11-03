package com.conveyal.gtfs.error;

/** Indicates that a column marked as required is entirely missing from a GTFS feed. */
public class MissingColumnError extends GTFSError {

    public MissingColumnError(String file, String field) {
        super(file, 0, field);
    }

    @Override public String getMessage() {
        return String.format("Missing required column.");
    }

}
