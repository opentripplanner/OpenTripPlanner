package org.opentripplanner.gtfs;

import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;

/**
 * Model bike access for GTFS trips.
 * 
 * The GTFS bike extensions is originally discussed at:
 * https://groups.google.com/d/msg/gtfs-changes/QqaGOuNmG7o/xyqORy-T4y0J
 * 
 * It proposes "route_bikes_allowed" in routes.txt and "trip_bikes_allowed" in trips.txt with the
 * following semantics:
 * 
 * 2: bikes allowed<br/>
 * 1: no bikes allowed<br/>
 * 0: no information (same as field omitted)<br/>
 * 
 * The values in trips.txt override the values in routes.txt.
 * 
 * An alternative proposal is discussed in:
 * https://groups.google.com/d/msg/gtfs-changes/rEiSeKNc4cs/gTTnQ_yXtPgJ
 * 
 * Here, the field "bikes_allowed" is used in both routes.txt and trip.txt with the following
 * semantics:
 * 
 * 2: no bikes allowed<br/>
 * 1: bikes allowed<br/>
 * 0: no information (same as field omitted)<br/>
 * 
 * Here, the 0,1,2 semantics have been changed to match the convention used in the
 * "wheelchair_accessible" field in trips.txt.
 * 
 * A number of feeds are still using the original proposal and a number of feeds have been updated
 * to use the new proposal. For now, we support both, using "bikes_allowed" if specified and then
 * "trip_bikes_allowed".
 */
public enum BikeAccess {
    UNKNOWN, NOT_ALLOWED, ALLOWED;

    @SuppressWarnings("deprecation")
    public static BikeAccess fromTrip(Trip trip) {
        switch (trip.getBikesAllowed()) {
        case 1:
            return ALLOWED;
        case 2:
            return NOT_ALLOWED;
        }
        switch (trip.getTripBikesAllowed()) {
        case 1:
            return NOT_ALLOWED;
        case 2:
            return ALLOWED;
        }
        Route route = trip.getRoute();
        switch (route.getBikesAllowed()) {
        case 1:
            return ALLOWED;
        case 2:
            return NOT_ALLOWED;
        }
        switch (route.getRouteBikesAllowed()) {
        case 1:
            return NOT_ALLOWED;
        case 2:
            return ALLOWED;
        }
        return UNKNOWN;
    }

    @SuppressWarnings("deprecation")
    public static void setForTrip(Trip trip, BikeAccess access) {
        switch (access) {
        case ALLOWED:
            trip.setBikesAllowed(1);
            trip.setTripBikesAllowed(2);
            break;
        case NOT_ALLOWED:
            trip.setBikesAllowed(2);
            trip.setTripBikesAllowed(1);
            break;
        case UNKNOWN:
            trip.setBikesAllowed(0);
            trip.setTripBikesAllowed(0);
            break;
        }
    }

    /**
     * @Improvment This method could be removed, if the logic was not part of the GTFS import,
     * but rader applyed after the GTFS model is mapped into OTP.
     */
    @SuppressWarnings("deprecation")
    public static BikeAccess fromTrip(org.onebusaway.gtfs.model.Trip gtfsTrip) {
        switch (gtfsTrip.getBikesAllowed()) {
        case 1:
            return ALLOWED;
        case 2:
            return NOT_ALLOWED;
        }
        switch (gtfsTrip.getTripBikesAllowed()) {
        case 1:
            return NOT_ALLOWED;
        case 2:
            return ALLOWED;
        }
        org.onebusaway.gtfs.model.Route route = gtfsTrip.getRoute();
        switch (route.getBikesAllowed()) {
        case 1:
            return ALLOWED;
        case 2:
            return NOT_ALLOWED;
        }
        switch (route.getRouteBikesAllowed()) {
        case 1:
            return NOT_ALLOWED;
        case 2:
            return ALLOWED;
        }
        return UNKNOWN;
    }

    /**
     * @Improvment This method could be removed, if the logic was not part of the GTFS import,
     * but rader applyed after the GTFS model is mapped into OTP.
     */
    @SuppressWarnings("deprecation")
    public static void setForTrip(org.onebusaway.gtfs.model.Trip gtfsTrip, BikeAccess access) {
        switch (access) {
        case ALLOWED:
            gtfsTrip.setBikesAllowed(1);
            gtfsTrip.setTripBikesAllowed(2);
            break;
        case NOT_ALLOWED:
            gtfsTrip.setBikesAllowed(2);
            gtfsTrip.setTripBikesAllowed(1);
            break;
        case UNKNOWN:
            gtfsTrip.setBikesAllowed(0);
            gtfsTrip.setTripBikesAllowed(0);
            break;
        }
    }
}
