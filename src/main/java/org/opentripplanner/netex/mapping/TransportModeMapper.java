package org.opentripplanner.netex.mapping;

import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;

public class TransportModeMapper {

    private static final Integer DEFAULT_OTP_VALUE = 3;

    public int getTransportMode(AllVehicleModesOfTransportEnumeration netexTransportMode, TransportSubmodeStructure transportSubmodeStructure) {
        if (transportSubmodeStructure == null)
            switch (netexTransportMode) {
                case AIR:
                    return 1100;
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
                case TAXI:
                    return 1500;
                case TRAM:
                    return 900;
                case WATER:
                    return 1000;
                default:
                    return DEFAULT_OTP_VALUE;
            }
        else {
            if (transportSubmodeStructure.getAirSubmode() != null) {
                switch (transportSubmodeStructure.getAirSubmode()) {
                    case DOMESTIC_FLIGHT:
                        return 1102;
                    case HELICOPTER_SERVICE:
                        return 1110;
                    case INTERNATIONAL_FLIGHT:
                        return 1101;
                    default:
                        return 1000;
                }
            } else if (transportSubmodeStructure.getBusSubmode() != null) {
                switch (transportSubmodeStructure.getBusSubmode()) {
                    case AIRPORT_LINK_BUS:
                        return 700; // ?
                    case EXPRESS_BUS:
                        return 702;
                    case LOCAL_BUS:
                        return 704;
                    case NIGHT_BUS:
                        return 705;
                    case RAIL_REPLACEMENT_BUS:
                        return 714;
                    case REGIONAL_BUS:
                        return 701;
                    case SCHOOL_BUS:
                        return 712;
                    case SHUTTLE_BUS:
                        return 711;
                    case SIGHTSEEING_BUS:
                        return 710;
                    default:
                        return 700;
                }
            } else if (transportSubmodeStructure.getTelecabinSubmode() != null) {
                switch (transportSubmodeStructure.getTelecabinSubmode()) {
                    case TELECABIN:
                        return 1301;
                    default:
                        return 1300;
                }
            } else if (transportSubmodeStructure.getCoachSubmode() != null) {
                switch (transportSubmodeStructure.getCoachSubmode()) {
                    case INTERNATIONAL_COACH:
                        return 201;
                    case NATIONAL_COACH:
                        return 202;
                    case TOURIST_COACH:
                        return 207;
                    default:
                        return 200;
                }
            } else if (transportSubmodeStructure.getFunicularSubmode() != null) {
                switch (transportSubmodeStructure.getFunicularSubmode()) {
                    case FUNICULAR:
                        return 1401;
                    default:
                        return 1400;
                }
            } else if (transportSubmodeStructure.getMetroSubmode() != null) {
                switch (transportSubmodeStructure.getMetroSubmode()) {
                    case METRO:
                        return 401;
                    case URBAN_RAILWAY:
                        return 403;
                    default:
                        return 401;
                }
            } else if (transportSubmodeStructure.getRailSubmode() != null) {
                switch (transportSubmodeStructure.getRailSubmode()) {
                    case AIRPORT_LINK_RAIL:
                        return 100; // ?
                    case INTERNATIONAL:
                        return 100; // ?
                    case INTERREGIONAL_RAIL:
                        return 103;
                    case LOCAL:
                        return 100; // ?
                    case LONG_DISTANCE:
                        return 102;
                    case NIGHT_RAIL:
                        return 100;
                    case REGIONAL_RAIL:
                        return 103;
                    case TOURIST_RAILWAY:
                        return 107;
                    default:
                        return 100;
                }
            } else if (transportSubmodeStructure.getTramSubmode() != null) {
                switch (transportSubmodeStructure.getTramSubmode()) {
                    case LOCAL_TRAM:
                        return 902;
                    default:
                        return 900;
                }
            } else if (transportSubmodeStructure.getWaterSubmode() != null) {
                switch (transportSubmodeStructure.getWaterSubmode()) {
                    case HIGH_SPEED_PASSENGER_SERVICE:
                        return 1014;
                    case HIGH_SPEED_VEHICLE_SERVICE:
                        return 1013;
                    case INTERNATIONAL_CAR_FERRY:
                        return 1001;
                    case INTERNATIONAL_PASSENGER_FERRY:
                        return 1005;
                    case LOCAL_CAR_FERRY:
                        return 1004;
                    case LOCAL_PASSENGER_FERRY:
                        return 1008;
                    case NATIONAL_CAR_FERRY:
                        return 1002;
                    case SIGHTSEEING_SERVICE:
                        return 1015;
                    default:
                        return 1000;
                }
            }
            else {
                return DEFAULT_OTP_VALUE;
            }
        }
    }
}
