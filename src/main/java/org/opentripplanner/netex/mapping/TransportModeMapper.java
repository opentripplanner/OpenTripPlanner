package org.opentripplanner.netex.mapping;

import org.opentripplanner.common.model.T2;
import org.opentripplanner.model.TransitMode;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;

/**
 * This is a best effort at mapping the NeTEx transport modes to the OTP route codes which are identical to the
 * <a href="https://developers.google.com/transit/gtfs/reference/extended-route-types">GTFS extended route types</a>
 */
class TransportModeMapper {

    public T2<TransitMode, String> map(
        AllVehicleModesOfTransportEnumeration netexMode,
        TransportSubmodeStructure submode
    ) throws UnsupportedModeException {
        if (submode != null) {
            return getSubmodeAsString(submode);
        }
        return new T2<>(mapAllVehicleModesOfTransport(netexMode), null);
    }

    private TransitMode mapAllVehicleModesOfTransport(AllVehicleModesOfTransportEnumeration mode)
            throws UnsupportedModeException {
        if(mode == null) {
            throw new UnsupportedModeException(null);
        }
        return switch (mode) {
            case AIR -> TransitMode.AIRPLANE;
            case BUS, TAXI -> TransitMode.BUS;
            case CABLEWAY -> TransitMode.CABLE_CAR;
            case COACH -> TransitMode.COACH;
            case FUNICULAR -> TransitMode.FUNICULAR;
            case METRO -> TransitMode.SUBWAY;
            case RAIL -> TransitMode.RAIL;
            case TRAM -> TransitMode.TRAM;
            case WATER -> TransitMode.FERRY;
            default -> throw new UnsupportedModeException(mode);
        };
    }

    private T2<TransitMode, String> getSubmodeAsString(TransportSubmodeStructure submode) {
        if (submode.getAirSubmode() != null) {
            return new T2<>(TransitMode.AIRPLANE, submode.getAirSubmode().value());
        } else if (submode.getBusSubmode() != null) {
            return new T2<>(TransitMode.BUS, submode.getBusSubmode().value());
        } else if (submode.getTelecabinSubmode() != null) {
            return new T2<>(TransitMode.GONDOLA, submode.getTelecabinSubmode().value());
        } else if (submode.getCoachSubmode() != null) {
            return new T2<>(TransitMode.COACH, submode.getCoachSubmode().value());
        } else if (submode.getFunicularSubmode() != null) {
            return new T2<>(TransitMode.FUNICULAR, submode.getFunicularSubmode().value());
        } else if (submode.getMetroSubmode() != null) {
            return new T2<>(TransitMode.SUBWAY, submode.getMetroSubmode().value());
        } else if (submode.getRailSubmode() != null) {
            return new T2<>(TransitMode.RAIL, submode.getRailSubmode().value());
        } else if (submode.getTramSubmode() != null) {
            return new T2<>(TransitMode.TRAM, submode.getTramSubmode().value());
        } else if (submode.getWaterSubmode() != null) {
            return new T2<>(TransitMode.FERRY, submode.getWaterSubmode().value());
        }
        throw new IllegalArgumentException();
    }

    static class UnsupportedModeException extends Exception {
        final AllVehicleModesOfTransportEnumeration mode;

        public UnsupportedModeException(AllVehicleModesOfTransportEnumeration mode) {
            this.mode = mode;
        }
    }
}
