package org.opentripplanner.routing.core;

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
    SHARE_TAXI, TROLLEY,
    TRANSIT, LEG_SWITCH,
    AIRPLANE;

    private static HashMap <Set<TraverseMode>, Set<TraverseMode>> setMap = 
            new HashMap <Set<TraverseMode>, Set<TraverseMode>>();

    public static Set<TraverseMode> internSet (Set<TraverseMode> modeSet) {
        if (modeSet == null)
            return null;
        Set<TraverseMode> ret = setMap.get(modeSet);
        if (ret == null) {
            EnumSet<TraverseMode> backingSet = EnumSet.noneOf(TraverseMode.class);
            backingSet.addAll(modeSet);
            Set<TraverseMode> unmodifiableSet = Collections.unmodifiableSet(backingSet);
            setMap.put(unmodifiableSet, unmodifiableSet);
            ret = unmodifiableSet;
        }
        return ret;
    }

    public boolean isTransit()
    {
        return this == TRAM || this == SUBWAY || this == RAIL || this == BUS || this == FERRY
                || this == CABLE_CAR || this == GONDOLA || this == FUNICULAR || this == TRANSIT
                || this == AIRPLANE || this == SHARE_TAXI || this == TROLLEY;
    }

    public boolean isOnStreetNonTransit() {
        return this == WALK || this == BICYCLE || this == CAR;
    }
    
    public boolean isDriving() {
        return this == CAR;
    }

}
