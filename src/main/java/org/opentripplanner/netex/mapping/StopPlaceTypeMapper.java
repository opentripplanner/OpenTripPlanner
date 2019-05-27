package org.opentripplanner.netex.mapping;

import org.rutebanken.netex.model.StopPlace;
import org.rutebanken.netex.model.VehicleModeEnumeration;

import static org.opentripplanner.netex.mapping.TransportModeMapper.mapAirSubmode;
import static org.opentripplanner.netex.mapping.TransportModeMapper.mapFunicularSubmode;
import static org.opentripplanner.netex.mapping.TransportModeMapper.mapMetroSubmode;
import static org.opentripplanner.netex.mapping.TransportModeMapper.mapRailSubmode;
import static org.opentripplanner.netex.mapping.TransportModeMapper.mapTelecabinSubmode;
import static org.opentripplanner.netex.mapping.TransportModeMapper.mapTramSubmode;
import static org.opentripplanner.netex.mapping.TransportModeMapper.mapWaterSubmode;
import static org.rutebanken.netex.model.RailSubmodeEnumeration.NIGHT_RAIL;

class StopPlaceTypeMapper {

    private static final Integer DEFAULT_OTP_VALUE = 3;

    int getTransportMode(StopPlace stopPlace) {
        if (stopPlace.getTransportMode() != null) {
            return mapVehicleMode(stopPlace.getTransportMode());
        }
        if (stopPlace.getAirSubmode() != null) {
            return mapAirSubmode (stopPlace.getAirSubmode());
        }
        if (stopPlace.getBusSubmode() != null) {
            return TransportModeMapper.mapBusSubmode(stopPlace.getBusSubmode());
        }
        if (stopPlace.getTelecabinSubmode() != null) {
            return mapTelecabinSubmode(stopPlace.getTelecabinSubmode());
        }
        if (stopPlace.getCoachSubmode() != null) {
            return TransportModeMapper.mapCoachSubmode(stopPlace.getCoachSubmode());
        }
        if (stopPlace.getFunicularSubmode() != null) {
            return mapFunicularSubmode (stopPlace.getFunicularSubmode());
        }
        if (stopPlace.getMetroSubmode() != null) {
            return mapMetroSubmode(stopPlace.getMetroSubmode());
        }
        if (stopPlace.getRailSubmode() != null) {
            // TODO OTP2 - Is this realy intended, the #mapRailSubmode returns 100 for night?
            if (stopPlace.getRailSubmode() == NIGHT_RAIL) {
                return 0;
            }
            return mapRailSubmode(stopPlace.getRailSubmode());
        }
        if (stopPlace.getTramSubmode() != null) {
            return mapTramSubmode(stopPlace.getTramSubmode());
        }
        if (stopPlace.getWaterSubmode() != null) {
            return mapWaterSubmode(stopPlace.getWaterSubmode());
        }
        return DEFAULT_OTP_VALUE;
    }

    private static int mapVehicleMode(VehicleModeEnumeration mode) {
        switch (mode) {
            case AIR:
                return 1000;
            case BUS:
                return 700;
            case CABLEWAY:
                return 1700;
            case COACH:
                return 200;
            case FUNICULAR:
                return 1400;
            case METRO:
                return 400;
            case RAIL:
                return 100;
            case TRAM:
                return 900;
            case WATER:
                return 1000;
            default:
                return DEFAULT_OTP_VALUE;
        }
    }
}
