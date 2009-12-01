package org.opentripplanner.routing.core;

import java.util.HashSet;

public enum TransportationMode {
    TRAM, SUBWAY, RAIL, BUS, FERRY, CABLE_CAR, GONDOLA, FUNICULAR, WALK, BICYCLE, BOARDING, ALIGHTING, TRANSFER;

    public static HashSet<TransportationMode> transitModes;
    static {
        transitModes = new HashSet<TransportationMode>();
        transitModes.add(TRAM);
        transitModes.add(SUBWAY);
        transitModes.add(RAIL);
        transitModes.add(BUS);
        transitModes.add(FERRY);
        transitModes.add(CABLE_CAR);
        transitModes.add(GONDOLA);
        transitModes.add(FUNICULAR);
    }
    
    public String toString() {
        return name().toLowerCase(); 
    }

    public boolean isTransitMode() {
        return transitModes.contains(this);
    }
}
