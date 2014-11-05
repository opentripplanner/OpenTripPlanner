package com.conveyal.gtfs.error;

/** Represents a problem parsing a URL field from a GTFS feed. */
public class URLParseError extends GTFSError {

    public URLParseError(String file, long line, String field) {
        super(file, line, field);
    }

    @Override public String getMessage() {
        return "Could not parse URL (format should be <scheme>://<authority><path>?<query>#<fragment>).";
    }

}
