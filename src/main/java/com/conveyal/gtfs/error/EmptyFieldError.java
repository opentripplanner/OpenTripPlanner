package com.conveyal.gtfs.error;

/** Indicates that a field marked as required is not present in a GTFS feed on a particular line. */
public class EmptyFieldError extends GTFSError {

    public EmptyFieldError(String file, long line, String field) {
        super(file, line, field);
    }

    @Override public String getMessage() {
        return String.format("No value supplied for a required column.");
    }

}
