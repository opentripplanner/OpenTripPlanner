package org.opentripplanner.routing.core;

import org.opentripplanner.model.TransitMode;

import java.util.EnumSet;

public enum TraverseMode {
    WALK, BICYCLE, CAR,
    TRAM, SUBWAY, RAIL, BUS, FERRY,
    CABLE_CAR, GONDOLA, FUNICULAR,
    TRANSIT, AIRPLANE;

    private static final EnumSet<TraverseMode> TRANSIT_MODES = EnumSet.of(
        AIRPLANE, BUS, CABLE_CAR, FERRY, FUNICULAR, GONDOLA, RAIL, SUBWAY, TRAM, TRANSIT
    );

    private static final EnumSet<TraverseMode> STREET_MODES = EnumSet.of(WALK, BICYCLE, CAR);

    public boolean isTransit() {
        return TRANSIT_MODES.contains(this);
    }

    public boolean isOnStreetNonTransit() {
        return STREET_MODES.contains(this);
    }
    
    public boolean isDriving() {
        return this == CAR;
    }

    public boolean isWalking() {
        return this == WALK;
    }

    public static TraverseMode fromTransitMode(TransitMode transitMode) {
        switch (transitMode) {
            case RAIL:
                return TraverseMode.RAIL;
            case COACH:
            case BUS:
                return TraverseMode.BUS;
            case SUBWAY:
                return TraverseMode.SUBWAY;
            case TRAM:
                return TraverseMode.TRAM;
            case FERRY:
                return TraverseMode.FERRY;
            case AIRPLANE:
                return TraverseMode.AIRPLANE;
            case CABLE_CAR:
                return TraverseMode.CABLE_CAR;
            case GONDOLA:
                return TraverseMode.GONDOLA;
            case FUNICULAR:
                return TraverseMode.FUNICULAR;
            default:
                throw new IllegalArgumentException();
        }
    }
}
