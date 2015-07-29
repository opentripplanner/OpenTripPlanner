package org.opentripplanner.transit;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

/**
*
*/
public enum PickDropType {

    SCHEDULED(0),
    NONE(1),
    CALL_AGENCY(2),
    COORDINATE_WITH_DRIVER(3);

    // Will be initialized after constructor is called on all enum values.
    static TIntObjectMap<PickDropType> forGtfsCode = new TIntObjectHashMap<>();
    static {
        for (PickDropType pdt : PickDropType.values()) {
            forGtfsCode.put(pdt.gtfsCode, pdt);
        }
    }

    int gtfsCode;

    PickDropType(int gtfsCode) {
        this.gtfsCode = gtfsCode;
    }

    static PickDropType forGtfsCode (int gtfsCode) {
        return forGtfsCode.get(gtfsCode);
    }

}
