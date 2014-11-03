package com.conveyal.gtfs.error;

/** Represents any GTFS loading problem that does not have its own class, with a free-text message. */
public class GeneralError extends GTFSError {

    private String message;

    public GeneralError(String file, long line, String field, String message) {
        super(file, line, field);
    }

    @Override public String getMessage() {
        return message;
    }

}
