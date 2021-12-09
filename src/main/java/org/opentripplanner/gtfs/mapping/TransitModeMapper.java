package org.opentripplanner.gtfs.mapping;

import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.TransitMode;

public class TransitModeMapper {

    /**
     * Return an OTP TransitMode matching a routeType. If no good match is found, it returns null.
     *
     * @param routeType  Route type to be mapped into a mode
     */
    public static TransitMode mapMode(int routeType) {
        // Should really be reference to org.onebusaway.gtfs.model.Stop.MISSING_VALUE, but it is private.
        if (routeType == -999) { return null; }

        /* TPEG Extension  https://groups.google.com/d/msg/gtfs-changes/keT5rTPS7Y0/71uMz2l6ke0J */
        if (routeType >= 100 && routeType < 200) { // Railway Service
            return TransitMode.RAIL;
        } else if (routeType >= 200 && routeType < 300) { //Coach Service
            return TransitMode.BUS;
        } else if (routeType >= 300
                && routeType < 500) { //Suburban Railway Service and Urban Railway service
            if (routeType >= 401 && routeType <= 402) {
                return TransitMode.SUBWAY;
            }
            if (routeType == 405) {
                return TransitMode.MONORAIL;
            }
            return TransitMode.RAIL;
        } else if (routeType >= 500 && routeType < 700) { //Metro Service and Underground Service
            return TransitMode.SUBWAY;
        } else if (routeType >= 700 && routeType < 900) { //Bus Service and Trolleybus service
            if (routeType == 800) {
                return TransitMode.TROLLEYBUS;
            }
            return TransitMode.BUS;
        } else if (routeType >= 900 && routeType < 1000) { //Tram service
            return TransitMode.TRAM;
        } else if (routeType >= 1000 && routeType < 1100) { //Water Transport Service
            return TransitMode.FERRY;
        } else if (routeType >= 1100 && routeType < 1200) { //Air Service
            return TransitMode.AIRPLANE;
        } else if (routeType >= 1200 && routeType < 1300) { //Ferry Service
            return TransitMode.FERRY;
        } else if (routeType >= 1300 && routeType < 1400) { //Telecabin Service
            return TransitMode.GONDOLA;
        } else if (routeType >= 1400 && routeType < 1500) { //Funicalar Service
            return TransitMode.FUNICULAR;
        } else if (routeType >= 1500 && routeType < 1600) { //Taxi Service
            return null;
        } else if (routeType >= 1600 && routeType < 1700) { //Self drive
            return TransitMode.BUS;
        } else if (routeType >= 1700 && routeType < 1800) { //Miscellaneous Service
            return null;
        }
        /* Original GTFS route types. Should these be checked before TPEG types? */
        switch (routeType) {
            case 0:
                return TransitMode.TRAM;
            case 1:
                return TransitMode.SUBWAY;
            case 2:
                return TransitMode.RAIL;
            case 3:
                return TransitMode.BUS;
            case 4:
                return TransitMode.FERRY;
            case 5:
                return TransitMode.CABLE_CAR;
            case 6:
                return TransitMode.GONDOLA;
            case 7:
                return TransitMode.FUNICULAR;
            case 11:
                return TransitMode.TROLLEYBUS;
            case 12:
                return TransitMode.MONORAIL;
            default:
                throw new IllegalArgumentException("unknown gtfs route type " + routeType);
        }
    }
}
