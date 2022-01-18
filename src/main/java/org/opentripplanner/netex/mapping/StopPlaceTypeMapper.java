package org.opentripplanner.netex.mapping;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.TransitMode;
import org.rutebanken.netex.model.StopPlace;

/**
 * This is a best effort at mapping the NeTEx transport modes to the OTP route codes which are identical to the
 * <a href="https://developers.google.com/transit/gtfs/reference/extended-route-types">GTFS extended route types</a>
 */
class StopPlaceTypeMapper {

    public T2<TransitMode, String> map(StopPlace stopPlace) {
        T2<TransitMode, String> submode = getSubmodeAsString(stopPlace);
        if (submode != null) { return submode; };
        TransitMode mode = mapVehicleMode(stopPlace);
        return new T2<>(mode, null);
    }

    private TransitMode mapVehicleMode(StopPlace stopPlace) {
        if (stopPlace.getTransportMode() == null) {
            return null;
        }
        switch (stopPlace.getTransportMode()) {
            case AIR:
                return TransitMode.AIRPLANE;
            case BUS:
                return TransitMode.BUS;
            case TROLLEY_BUS:
                return TransitMode.TROLLEYBUS;
            case CABLEWAY:
                return TransitMode.CABLE_CAR;
            case COACH:
                return TransitMode.COACH;
            case FUNICULAR:
                return TransitMode.FUNICULAR;
            case METRO:
                return TransitMode.SUBWAY;
            case RAIL:
                return TransitMode.RAIL;
            case TRAM:
                return TransitMode.TRAM;
            case WATER:
            case FERRY:
                return TransitMode.FERRY;
            default:
                return null;
        }
    }

    private T2<TransitMode, String> getSubmodeAsString(StopPlace stopPlace) {
        if (stopPlace.getAirSubmode() != null) {
            return new T2<>(TransitMode.AIRPLANE, stopPlace.getAirSubmode().value());
        }
        if (stopPlace.getBusSubmode() != null) {
            return new T2<>(TransitMode.BUS, stopPlace.getBusSubmode().value());
        }
        if (stopPlace.getTelecabinSubmode() != null) {
            return new T2<>(TransitMode.GONDOLA, stopPlace.getTelecabinSubmode().value());
        }
        if (stopPlace.getCoachSubmode() != null) {
            return new T2<>(TransitMode.COACH, stopPlace.getCoachSubmode().value());
        }
        if (stopPlace.getFunicularSubmode() != null) {
            return new T2<>(TransitMode.FUNICULAR, stopPlace.getFunicularSubmode().value());
        }
        if (stopPlace.getMetroSubmode() != null) {
            return new T2<>(TransitMode.SUBWAY, stopPlace.getMetroSubmode().value());
        }
        if (stopPlace.getRailSubmode() != null) {
            return new T2<>(TransitMode.RAIL, stopPlace.getRailSubmode().value());
        }
        if (stopPlace.getTramSubmode() != null) {
            return new T2<>(TransitMode.TRAM, stopPlace.getTramSubmode().value());
        }
        if (stopPlace.getWaterSubmode() != null) {
            return new T2<>(TransitMode.FERRY, stopPlace.getWaterSubmode().value());
        }
        return null;
    }
}
