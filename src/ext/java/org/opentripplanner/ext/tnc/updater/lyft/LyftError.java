package org.opentripplanner.ext.tnc.updater.lyft;

public class LyftError {
    public String error;
    public String error_description;

    @Override
    public String toString() {
        return "LyftError{" +
            "error='" + (error != null ? error : "null") + '\'' +
            ", error_description='" + (error_description != null ? error_description : "null") + '\'' +
            '}';
    }
}
