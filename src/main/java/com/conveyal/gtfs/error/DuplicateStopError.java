package com.conveyal.gtfs.error;

/** Indicates that a stop exists more than once in the feed. */
public class DuplicateStopError extends GTFSError {

    private String message;

    public DuplicateStopError(String file, long line, String field, String message) {
        super(file, line, field);
    }

    @Override public String getMessage() {
        return message;
    }

}
