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

// TODO OTP2 - Add Unit tests
// TODO OTP2 - This code needs cleanup
// TODO OTP2 - JavaDoc needed
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
}
