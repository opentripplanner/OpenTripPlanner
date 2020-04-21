package org.opentripplanner.routing.core;

import org.opentripplanner.model.TransitMode;

public enum TraverseMode {
    WALK, BICYCLE, CAR,
    TRAM, SUBWAY, RAIL, BUS, FERRY,
    CABLE_CAR, GONDOLA, FUNICULAR,
    TRANSIT, LEG_SWITCH,
    AIRPLANE;

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
