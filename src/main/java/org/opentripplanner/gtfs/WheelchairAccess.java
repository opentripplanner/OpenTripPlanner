package org.opentripplanner.gtfs;

/**
 * Describes the level of wheelchair access for a given place.
 *
 * Used to decide if wheelchair users can use a certain stop or trip.
 */
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
