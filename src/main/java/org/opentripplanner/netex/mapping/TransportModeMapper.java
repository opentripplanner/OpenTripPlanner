package org.opentripplanner.netex.mapping;

import org.rutebanken.netex.model.AirSubmodeEnumeration;
import org.rutebanken.netex.model.AllVehicleModesOfTransportEnumeration;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.CoachSubmodeEnumeration;
import org.rutebanken.netex.model.FunicularSubmodeEnumeration;
import org.rutebanken.netex.model.MetroSubmodeEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.rutebanken.netex.model.TelecabinSubmodeEnumeration;
import org.rutebanken.netex.model.TramSubmodeEnumeration;
import org.rutebanken.netex.model.TransportSubmodeStructure;
import org.rutebanken.netex.model.VehicleModeEnumeration;
import org.rutebanken.netex.model.WaterSubmodeEnumeration;

// TODO OTP2 - Add Unit tests
// TODO OTP2 - JavaDoc needed
class TransportModeMapper {

    private static final Integer DEFAULT_OTP_VALUE = 3;

    int getTransportMode(
            AllVehicleModesOfTransportEnumeration netexMode,
            TransportSubmodeStructure submode
    ) {
        if (submode == null) {
            return mapAllVehicleModesOfTransport(netexMode);
        } else {
            if (submode.getAirSubmode() != null) {
                return mapAirSubmode(submode.getAirSubmode());
            } else if (submode.getBusSubmode() != null) {
                return mapBusSubmode(submode.getBusSubmode());
            } else if (submode.getTelecabinSubmode() != null) {
                return mapTelecabinSubmode(submode.getTelecabinSubmode());
            } else if (submode.getCoachSubmode() != null) {
                return mapCoachSubmode(submode.getCoachSubmode());
            } else if (submode.getFunicularSubmode() != null) {
                return mapFunicularSubmode(submode.getFunicularSubmode());
            } else if (submode.getMetroSubmode() != null) {
                return mapMetroSubmode(submode.getMetroSubmode());
            } else if (submode.getRailSubmode() != null) {
                return mapRailSubmode(submode.getRailSubmode());
            } else if (submode.getTramSubmode() != null) {
                return mapTramSubmode(submode.getTramSubmode());
            } else if (submode.getWaterSubmode() != null) {
                return mapWaterSubmode(submode.getWaterSubmode());
            }
            else {
                return DEFAULT_OTP_VALUE;
            }
        }
    }

    private static int mapAllVehicleModesOfTransport(AllVehicleModesOfTransportEnumeration mode) {
        switch (mode) {
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
    }

    static int mapVehicleMode(VehicleModeEnumeration mode) {
        switch (mode) {
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
        case TRAM:
            return 900;
        case WATER:
            return 1000;
        default:
            return DEFAULT_OTP_VALUE;
        }
    }

    static int mapAirSubmode(AirSubmodeEnumeration mode) {
        switch (mode) {
            case DOMESTIC_FLIGHT:
                return 1102;
            case HELICOPTER_SERVICE:
                return 1110;
            case INTERNATIONAL_FLIGHT:
                return 1101;
            default:
                return 1000;
        }
    }

    static int mapBusSubmode(BusSubmodeEnumeration mode) {
        switch (mode) {
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
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    static int mapTelecabinSubmode(TelecabinSubmodeEnumeration mode) {
        switch (mode) {
            case TELECABIN:
                return 1301;
            default:
                return 1300;
        }
    }

    static int mapCoachSubmode(CoachSubmodeEnumeration mode) {
        switch (mode) {
            case INTERNATIONAL_COACH:
                return 201;
            case NATIONAL_COACH:
                return 202;
            case TOURIST_COACH:
                return 207;
            default:
                return 200;
        }
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    static int mapFunicularSubmode(FunicularSubmodeEnumeration mode) {
        switch (mode) {
            case FUNICULAR:
                return 1401;
            default:
                return 1400;
        }
    }

    static int mapMetroSubmode(MetroSubmodeEnumeration mode) {
        switch (mode) {
            case METRO:
                return 401;
            case URBAN_RAILWAY:
                return 403;
            default:
                return 401;
        }
    }

    static int mapRailSubmode(RailSubmodeEnumeration mode) {
        switch (mode) {
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
    }

    @SuppressWarnings("SwitchStatementWithTooFewBranches")
    static int mapTramSubmode(TramSubmodeEnumeration mode) {
        switch (mode) {
            case LOCAL_TRAM:
                return 902;
            default:
                return 900;
        }
    }

    static int mapWaterSubmode(WaterSubmodeEnumeration mode) {
        switch (mode) {
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
}
