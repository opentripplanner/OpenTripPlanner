package org.opentripplanner.gtfs;

import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.TransitMode;


public class GtfsLibrary {

    private static final char ID_SEPARATOR = ':'; // note this is different than what OBA GTFS uses to match our 1.0 API

    /* Using in index since we can't modify OBA libs and the colon in the expected separator in the 1.0 API. */
    public static FeedScopedId convertIdFromString(String value) {
        int index = value.indexOf(ID_SEPARATOR);
        if (index == -1) {
            throw new IllegalArgumentException("invalid agency-and-id: " + value);
        } else {
            return new FeedScopedId(value.substring(0, index), value.substring(index + 1));
        }
    }

    public static String convertIdToString(FeedScopedId aid) {
        return aid.getFeedId() + ID_SEPARATOR + aid.getId();
    }

    /** @return the route's short name, or the long name if the short name is null. */
    public static String getRouteName(Route route) {
        if (route.getShortName() != null)
            return route.getShortName();
        return route.getLongName();
    }

    public static TransitMode getTransitMode(Route route) {
        int routeType = route.getType();
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
            return TransitMode.RAIL;
        } else if (routeType >= 500 && routeType < 700) { //Metro Service and Underground Service
            return TransitMode.SUBWAY;
        } else if (routeType >= 700 && routeType < 900) { //Bus Service and Trolleybus service
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
            throw new IllegalArgumentException("Taxi service not supported" + routeType);
        } else if (routeType >= 1600 && routeType < 1700) { //Self drive
            throw new IllegalArgumentException("Self drive not supported" + routeType);
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
        default:
            throw new IllegalArgumentException("unknown gtfs route type " + routeType);
        }
    }
}
