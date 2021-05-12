package org.opentripplanner.api.parameter;

import org.opentripplanner.model.TransitMode;

public enum ApiRequestMode {
    WALK, BICYCLE, CAR,
    TRAM, SUBWAY, RAIL, BUS, COACH, FERRY,
    CABLE_CAR, GONDOLA, FUNICULAR,
    TRANSIT, AIRPLANE, FLEX;

    public static ApiRequestMode fromTransitMode(TransitMode transitMode) {
        switch (transitMode) {
            case RAIL:
                return RAIL;
            case COACH:
                return COACH;
            case SUBWAY:
                return SUBWAY;
            case BUS:
                return BUS;
            case TRAM:
                return TRAM;
            case FERRY:
                return FERRY;
            case AIRPLANE:
                return AIRPLANE;
            case CABLE_CAR:
                return CABLE_CAR;
            case GONDOLA:
                return GONDOLA;
            case FUNICULAR:
                return FUNICULAR;
            default:
                throw new IllegalArgumentException("Can't convert to ApiRequestMode: " + transitMode);
        }
    }
}
