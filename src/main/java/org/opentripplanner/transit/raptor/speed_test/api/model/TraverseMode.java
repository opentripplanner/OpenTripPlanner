package org.opentripplanner.transit.raptor.speed_test.api.model;

import javax.xml.bind.annotation.XmlType;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Set;

@XmlType(name="TraverseMode")
public enum TraverseMode {
    WALK, BICYCLE, CAR,
    TRAM, SUBWAY, RAIL, BUS, FERRY,
    CABLE_CAR, GONDOLA, FUNICULAR,
    TRANSIT, LEG_SWITCH,
    AIRPLANE, CAR_PARK, CAR_PICKUP;

    private static HashMap<Set<org.opentripplanner.transit.raptor.speed_test.api.model.TraverseMode>, Set<org.opentripplanner.transit.raptor.speed_test.api.model.TraverseMode>> setMap =
            new HashMap<Set<org.opentripplanner.transit.raptor.speed_test.api.model.TraverseMode>, Set<org.opentripplanner.transit.raptor.speed_test.api.model.TraverseMode>>();

    public static Set<org.opentripplanner.transit.raptor.speed_test.api.model.TraverseMode> internSet (Set<org.opentripplanner.transit.raptor.speed_test.api.model.TraverseMode> modeSet) {
        if (modeSet == null)
            return null;
        Set<org.opentripplanner.transit.raptor.speed_test.api.model.TraverseMode> ret = setMap.get(modeSet);
        if (ret == null) {
            EnumSet<org.opentripplanner.transit.raptor.speed_test.api.model.TraverseMode> backingSet = EnumSet.noneOf(org.opentripplanner.transit.raptor.speed_test.api.model.TraverseMode.class);
            backingSet.addAll(modeSet);
            Set<org.opentripplanner.transit.raptor.speed_test.api.model.TraverseMode> unmodifiableSet = Collections.unmodifiableSet(backingSet);
            setMap.put(unmodifiableSet, unmodifiableSet);
            ret = unmodifiableSet;
        }
        return ret;
    }

    public boolean isTransit() {
        return this == TRAM || this == SUBWAY || this == RAIL || this == BUS || this == FERRY
                || this == CABLE_CAR || this == GONDOLA || this == FUNICULAR || this == TRANSIT
                || this == AIRPLANE;
    }

    public boolean isOnStreetNonTransit() {
        return this == WALK || this == BICYCLE || this == CAR;
    }
    
    public boolean isDriving() {
        return this == CAR;
    }

}
