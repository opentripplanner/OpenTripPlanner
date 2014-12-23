package com.conveyal.gtfs.error;

/**
 * Represents an error encountered
 */
public abstract class GTFSError {

    final String file; // TODO GTFSTable enum? Or simply use class objects.
    final long   line;
    final String field;

    public GTFSError(String file, long line, String field) {
        this.file  = file;
        this.line  = line;
        this.field = field;
    }

    public String getMessage() {
        return "no message";
    }

    public String getMessageWithContext() {
        StringBuilder sb = new StringBuilder();
        sb.append(file);
        sb.append(' ');
        if (line >= 0) {
            sb.append("line ");
            sb.append(line);
        } else {
            sb.append("(no line)");
        }
        if (field != null) {
            sb.append(", field '");
            sb.append(field);
            sb.append('\'');
        }
        sb.append(": ");
        sb.append(getMessage());
        return sb.toString();
    }

    @Override
    public String toString() {
        return "GTFSError: " + getMessageWithContext();
    }

}
