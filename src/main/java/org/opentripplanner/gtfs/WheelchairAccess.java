package org.opentripplanner.gtfs;

public enum WheelchairAccess {
    UNKNOWN,ALLOWED,NOT_ALLOWED;

    public static WheelchairAccess fromGtfsValue(int v) {
        switch (v) {
            case 1:
                return ALLOWED;
            case 2:
                return NOT_ALLOWED;
            default:
                return UNKNOWN;
        }
    }
}
