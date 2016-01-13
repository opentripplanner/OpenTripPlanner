package org.opentripplanner.updater;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.serialization.mappings.StopTimeFieldMappingFactory;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.text.ParseException;
import java.util.BitSet;

/**
 * This class is used for matching TripDescriptors without trip_ids to scheduled GTFS data and to
 * feed back that information into a new TripDescriptor with proper trip_id.
 *
 * The class should only be used if we know that the feed producer is unable to produce trip_ids
 * in the GTFS-RT feed.
 */
public class GtfsRealtimeFuzzyTripMatcher {

    private GraphIndex index;

    public GtfsRealtimeFuzzyTripMatcher(GraphIndex index) {
        this.index = index;
    }

    public TripDescriptor match(String feedId, TripDescriptor trip) {
        if (trip.hasTripId()) {
            // trip_id already exists
            return trip;
        }

        if (!trip.hasRouteId() || !trip.hasDirectionId() ||
                !trip.hasStartTime() || !trip.hasStartDate()) {
            // Could not determine trip_id, returning original TripDescriptor
            return trip;
        }

        AgencyAndId routeId = new AgencyAndId(feedId, trip.getRouteId());
        int time = StopTimeFieldMappingFactory.getStringAsSeconds(trip.getStartTime());
        ServiceDate date;
        try {
            date = ServiceDate.parseString(trip.getStartDate());
        } catch (ParseException e) {
            return trip;
        }
        Route route = index.routeForId.get(routeId);
        if (route == null) {
            return trip;
        }
        int direction = trip.getDirectionId();

        Trip matchedTrip = getTrip(route, direction, time, date);

        if (matchedTrip == null) {
            // Check if the trip is carried over from previous day
            date = date.previous();
            time += 24*60*60;
            matchedTrip = getTrip(route, direction, time, date);
        }

        if (matchedTrip == null) {
            return trip;
        }

        // If everything succeeds, build a new TripDescriptor with the matched trip_id
        return trip.toBuilder().setTripId(matchedTrip.getId().getId()).build();

    }

    public Trip getTrip (Route route, int direction,
                          int startTime, ServiceDate date) {
        BitSet services = index.servicesRunning(date);
        for (TripPattern pattern : index.patternsForRoute.get(route)) {
            if (pattern.directionId != direction) continue;
            for (TripTimes times : pattern.scheduledTimetable.tripTimes) {
                if (times.getScheduledDepartureTime(0) == startTime &&
                        services.get(times.serviceCode)) {
                    return times.trip;
                }
            }
        }
        return null;
    }
}
