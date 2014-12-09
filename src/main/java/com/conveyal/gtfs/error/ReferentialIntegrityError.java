package com.conveyal.gtfs.error;

/** Indicates that an entity referenced another entity that does not exist. */
public class ReferentialIntegrityError extends GTFSError {

    // TODO maybe also store the entity ID of the entity which contained the bad reference, in addition to the row number
    String badReference = null;

    public ReferentialIntegrityError(String tableName, long row, String field, String badReference) {
        super(tableName, row, field);
        this.badReference = badReference;
    }

    @Override public String getMessage() {
        return String.format("Entity references non-existant entity '%s'.", badReference);
    }

}
