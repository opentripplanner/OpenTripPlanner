package org.opentripplanner.transit;

/**
 * Enums are inherently serializable.
 */
public enum TripFlag {

    BICYCLE(0),
    WHEELCHAIR(1);
    int flag;

    TripFlag(int bitNumber) {
        flag = 1 << bitNumber;
    }

}
