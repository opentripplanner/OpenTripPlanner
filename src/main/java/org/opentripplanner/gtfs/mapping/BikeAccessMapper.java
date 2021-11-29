package org.opentripplanner.gtfs.mapping;

import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.model.BikeAccess;

/**
 * Model bike access for GTFS trips.
 *
 * The GTFS bike extensions is originally discussed at:
 * https://groups.google.com/d/msg/gtfs-changes/QqaGOuNmG7o/xyqORy-T4y0J
 *
 * It proposes "route_bikes_allowed" in routes.txt and "trip_bikes_allowed" in trips.txt with the
 * following semantics:
 *
 * 2: bikes allowed<br>
 * 1: no bikes allowed<br>
 * 0: no information (same as field omitted)<br>
 *
 * The values in trips.txt override the values in routes.txt.
 *
 * An alternative proposal is discussed in:
 * https://groups.google.com/d/msg/gtfs-changes/rEiSeKNc4cs/gTTnQ_yXtPgJ
 *
 * Here, the field "bikes_allowed" is used in both routes.txt and trip.txt with the following
 * semantics:
 *
 * 2: no bikes allowed<br>
 * 1: bikes allowed<br>
 * 0: no information (same as field omitted)<br>
 *
 * Here, the 0,1,2 semantics have been changed to match the convention used in the
 * "wheelchair_accessible" field in trips.txt.
 *
 * A number of feeds are still using the original proposal and a number of feeds have been updated
 * to use the new proposal. For now, we support both, using "bikes_allowed" if specified and then
 * "trip_bikes_allowed".
 */
class BikeAccessMapper {

    public static BikeAccess mapForTrip(Trip rhs) {
        //noinspection deprecation
        return mapValues(rhs.getBikesAllowed(), rhs.getTripBikesAllowed());
    }

    public static BikeAccess mapForRoute(Route rhs) {
        //noinspection deprecation
        return mapValues(rhs.getBikesAllowed(), rhs.getRouteBikesAllowed());
    }

    private static BikeAccess mapValues(int bikesAllowed, int legacyBikesAllowed) {
        if (bikesAllowed != 0) {
            switch (bikesAllowed) {
                case 1:
                    return BikeAccess.ALLOWED;
                case 2:
                    return BikeAccess.NOT_ALLOWED;
                default:
                    return BikeAccess.UNKNOWN;
            }
        }
        else if (legacyBikesAllowed != 0) {
            switch (legacyBikesAllowed) {
                case 1:
                    return BikeAccess.NOT_ALLOWED;
                case 2:
                    return BikeAccess.ALLOWED;
                default:
                    return BikeAccess.UNKNOWN;
            }
        }

        return BikeAccess.UNKNOWN;
    }
}
