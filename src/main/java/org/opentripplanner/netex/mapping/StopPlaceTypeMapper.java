package org.opentripplanner.netex.mapping;

import org.rutebanken.netex.model.StopPlace;

import static org.opentripplanner.netex.mapping.TransportModeMapper.mapAirSubmode;
import static org.opentripplanner.netex.mapping.TransportModeMapper.mapFunicularSubmode;
import static org.opentripplanner.netex.mapping.TransportModeMapper.mapMetroSubmode;
import static org.opentripplanner.netex.mapping.TransportModeMapper.mapRailSubmode;
import static org.opentripplanner.netex.mapping.TransportModeMapper.mapTelecabinSubmode;
import static org.opentripplanner.netex.mapping.TransportModeMapper.mapTramSubmode;
import static org.opentripplanner.netex.mapping.TransportModeMapper.mapVehicleMode;
import static org.opentripplanner.netex.mapping.TransportModeMapper.mapWaterSubmode;

/**
 * This is a best effort at mapping the NeTEx transport modes to the OTP route codes which are identical to the
 * <a href="https://developers.google.com/transit/gtfs/reference/extended-route-types">GTFS extended route types</a>
 */
class StopPlaceTypeMapper {

    private static final Integer DEFAULT_OTP_VALUE = 3;

    int getTransportMode(StopPlace stopPlace) {
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
            return mapRailSubmode(stopPlace.getRailSubmode());
        }
        if (stopPlace.getTramSubmode() != null) {
            return mapTramSubmode(stopPlace.getTramSubmode());
        }
        if (stopPlace.getWaterSubmode() != null) {
            return mapWaterSubmode(stopPlace.getWaterSubmode());
        }
        if (stopPlace.getTransportMode() != null) {
            return mapVehicleMode(stopPlace.getTransportMode());
        }
        return DEFAULT_OTP_VALUE;
    }
}
