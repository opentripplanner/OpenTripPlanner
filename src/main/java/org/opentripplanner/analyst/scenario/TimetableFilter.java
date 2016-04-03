package org.opentripplanner.analyst.scenario;

import com.google.common.primitives.Ints;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.routing.edgetype.TripPattern;

import java.util.Collection;

/**
 * Abstract class for modifications to existing timetables/frequencies.
 */
public abstract class TimetableFilter extends Modification {
    /**
     * Agency ID to match. Each modification can apply to only a single agency, as having multiple agencies
     * means route and trip IDs may not be unique.
     */
    public String agencyId;

    /** Route IDs to match, or null for all */
    public Collection<String> routeId;

    /** Trip IDs to match, or null for all */
    public Collection<String> tripId;

    /** GTFS route types to match, see constants in com.conveyal.gtfs.model.Route */
    public int[] routeType;

    /** Could any trip on this trip pattern possibly match this filter? */
    protected boolean couldMatch (TripPattern pattern) {
        if (!pattern.route.getAgency().getId().equals(agencyId))
            return false;

        if (routeId != null && !routeId.contains(pattern.route.getId().getId()))
            return false;

        if (routeType != null && !Ints.contains(routeType, pattern.route.getType()))
            return false;

        return true;
    }

    /** Does this TripTimes match the match parameters defined here? */
    protected boolean matches(Trip trip) {
        Route route = trip.getRoute();

        if (!route.getAgency().getId().equals(agencyId))
            return false;

        if (routeId != null && !routeId.contains(route.getId().getId()))
            return false;

        if (tripId != null && !tripId.contains(trip.getId().getId()))
            return false;

        if (routeType != null && !Ints.contains(routeType, trip.getRoute().getType()))
            return false;

        return true;
    }
}
