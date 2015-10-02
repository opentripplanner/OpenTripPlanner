package org.opentripplanner.transit;

/**
*
*/
public enum PickDropType {

    SCHEDULED(0),
    NONE(1),
    CALL_AGENCY(2),
    COORDINATE_WITH_DRIVER(3);

    // Will be initialized after constructor is called on all enum values.
    private static PickDropType[] forGtfsCode;
    static {
        forGtfsCode = new PickDropType[4];
        for (PickDropType pdt : PickDropType.values()) {
            forGtfsCode[pdt.gtfsCode] = pdt;
        }
    }

    int gtfsCode;

    PickDropType(int gtfsCode) {
        this.gtfsCode = gtfsCode;
    }

    static PickDropType forGtfsCode (int gtfsCode) {
        return forGtfsCode[gtfsCode];
    }

}
