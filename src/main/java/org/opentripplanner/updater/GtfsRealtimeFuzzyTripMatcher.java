package org.opentripplanner.updater;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.util.time.TimeUtils;

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

    private RoutingService routingService;
    private BitSet servicesRunningForDate;
    private ServiceDate date;

    public GtfsRealtimeFuzzyTripMatcher(RoutingService routingService) {
        this.routingService = routingService;
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

        FeedScopedId routeId = new FeedScopedId(feedId, trip.getRouteId());
        int time = TimeUtils.time(trip.getStartTime());
        ServiceDate date;
        try {
            date = ServiceDate.parseString(trip.getStartDate());
        } catch (ParseException e) {
            return trip;
        }
        Route route = routingService.getRouteForId(routeId);
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

    public synchronized Trip getTrip (Route route, int direction, int startTime, ServiceDate date) {
        if (!date.equals(this.date)) {
            this.date = date;
            // TODO: This is slow, we should either precalculate or cache these for all dates in graph
            this.servicesRunningForDate = routingService.getServicesRunningForDate(date);
        }
        for (TripPattern pattern : routingService.getPatternsForRoute().get(route)) {
            if (pattern.getDirection().gtfsCode != direction) continue;
            for (TripTimes times : pattern.getScheduledTimetable().getTripTimes()) {
                if (times.getScheduledDepartureTime(0) == startTime &&
                        servicesRunningForDate.get(times.getServiceCode())) {
                    return times.getTrip();
                }
            }
        }
        return null;
    }
}
