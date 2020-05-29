package org.opentripplanner.routing;

import lombok.experimental.Delegate;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeShort;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.algorithm.RoutingWorker;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.standalone.server.Router;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;

/**
 * This is the entry point of all API requests towards the OTP graph. A new instance of this class
 * should be created for each request. This ensures that the same TimetableSnapshot is used for the
 * duration of the request (which may involve several method calls).
 */
public class RoutingService {

    @Delegate(types = Graph.class)
    private final Graph graph;

    @Delegate(types = GraphIndex.class)
    private final GraphIndex graphIndex;

    /**
     * This should only be accessed through the getTimetableSnapshot method.
     */
    private TimetableSnapshot timetableSnapshot;

    public RoutingService(Graph graph) {
        this.graph = graph;
        this.graphIndex = graph.index;
    }

    // TODO We should probably not have the Router as a parameter here
    public RoutingResponse route(RoutingRequest request, Router router) {
        RoutingWorker worker = new RoutingWorker(router.raptorConfig, request);
        return worker.route(router);
    }

    public List<StopFinder.StopAndDistance> findClosestStopsByWalking(
            double lat, double lon, int radius
    ) {
        return StopFinder.findClosestStopsByWalking(graph, lat, lon, radius);
    }

    /**
     * Fetch upcoming vehicle departures from a stop. It goes though all patterns passing the stop
     * for the previous, current and next service date. It uses a priority queue to keep track of
     * the next departures. The queue is shared between all dates, as services from the previous
     * service date can visit the stop later than the current service date's services. This happens
     * eg. with sleeper trains.
     * <p>
     * TODO: Add frequency based trips
     *
     * @param stop               Stop object to perform the search for
     * @param startTime          Start time for the search. Seconds from UNIX epoch
     * @param timeRange          Searches forward for timeRange seconds from startTime
     * @param numberOfDepartures Number of departures to fetch per pattern
     * @param omitNonPickups     If true, do not include vehicles that will not pick up passengers.
     */
    public List<StopTimesInPattern> stopTimesForStop(
            Stop stop, long startTime, int timeRange, int numberOfDepartures, boolean omitNonPickups
    ) {
        return StopTimesHelper.stopTimesForStop(this,
                lazyGetTimeTableSnapShot(),
                stop,
                startTime,
                timeRange,
                numberOfDepartures,
                omitNonPickups
        );
    }

    /**
     * Get a list of all trips that pass through a stop during a single ServiceDate. Useful when
     * creating complete stop timetables for a single day.
     *
     * @param stop        Stop object to perform the search for
     * @param serviceDate Return all departures for the specified date
     */
    public List<StopTimesInPattern> getStopTimesForStop(
            Stop stop, ServiceDate serviceDate, boolean omitNonPickups
    ) {
        return StopTimesHelper.getStopTimesForStop(this, stop, serviceDate, omitNonPickups);
    }

    /**
     * Returns all the patterns for a specific stop. If includeRealtimeUpdates is set, new patterns
     * added by realtime updates are added to the collection.
     */
    public Collection<TripPattern> getPatternsForStop(Stop stop, boolean includeRealtimeUpdates) {
        return graph.index.getPatternsForStop(stop,
                includeRealtimeUpdates ? lazyGetTimeTableSnapShot() : null
        );
    }

    /**
     * Get the most up-to-date timetable for the given TripPattern, as of right now. There should
     * probably be a less awkward way to do this that just gets the latest entry from the resolver
     * without making a fake routing request.
     */
    public Timetable getTimetableForTripPattern(TripPattern tripPattern) {
        TimetableSnapshot timetableSnapshot = lazyGetTimeTableSnapShot();
        return timetableSnapshot != null ? timetableSnapshot.resolve(
                tripPattern,
                new ServiceDate(Calendar.getInstance().getTime())
        ) : tripPattern.scheduledTimetable;
    }

    public List<TripTimeShort> getTripTimesShort(Trip trip, ServiceDate serviceDate) {
        return TripTimesShortHelper.getTripTimesShort(this, trip, serviceDate);
    }

    /**
     * Lazy-initialization of TimetableSnapshot
     *
     * @return The same TimetableSnapshot is returned throughout the lifecycle of this object.
     */
    private TimetableSnapshot lazyGetTimeTableSnapShot() {
        if (this.timetableSnapshot == null) {
            timetableSnapshot = graph.getTimetableSnapshot();
        }
        return this.timetableSnapshot;
    }
}
