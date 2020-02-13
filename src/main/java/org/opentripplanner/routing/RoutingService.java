package org.opentripplanner.routing;

import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.ExecutorServiceExecutionStrategy;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.index.IndexGraphQLSchema;
import org.opentripplanner.index.model.StopTimesInPattern;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.AlertPatchService;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.util.HttpToGraphQLMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * This class contains all the transient indexes of graph elements -- those that are not
 * serialized with the graph. Caching these maps is essentially an optimization, but a big one.
 * The index is bootstrapped from the graph's list of edges.
 */
public class RoutingService {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingService.class);

    private Graph graph;

    // TODO Move this
    private GraphQL graphQL;

    public RoutingService(Graph graph) {
        this.graph = graph;
        graphQL = new GraphQL(
            new IndexGraphQLSchema(this).indexSchema,
            new ExecutorServiceExecutionStrategy(Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setNameFormat("GraphQLExecutor-" + graph.routerId + "-%d").build()
            )));
    }

    public List<FindClosestStopsByWalking.StopAndDistance> findClosestStopsByWalking(double lat, double lon, int radius) {
        return FindClosestStopsByWalking.findClosestStopsByWalking(graph, lat, lon, radius);
    }

    public Map<String, Map<String, Agency>> getAgenciesForFeedId() {
        return graph.index.getAgenciesForFeedId();
    }

    public Map<FeedScopedId, Operator> getOperatorForId() {
        return graph.index.getOperatorForId();
    }

    public Map<String, FeedInfo> getFeedInfoForId() {
        return graph.index.getFeedInfoForId();
    }

    public Map<FeedScopedId, Stop> getStopForId() {
        return graph.index.getStopForId();
    }

    public Map<FeedScopedId, Trip> getTripForId() {
        return graph.index.getTripForId();
    }

    public Map<FeedScopedId, Route> getRouteForId() {
        return graph.index.getRouteForId();
    }

    public Map<Stop, TransitStopVertex> getStopVertexForStop() {
        return graph.index.getStopVertexForStop();
    }

    public Map<Trip, TripPattern> getPatternForTrip() {
        return graph.index.getPatternForTrip();
    }

    public Multimap<Route, TripPattern> getPatternsForRoute() {
        return graph.index.getPatternsForRoute();
    }

    public Multimap<Stop, TripPattern> getPatternsForStop() {
        return graph.index.getPatternsForStop();
    }

    public Map<Station, MultiModalStation> getMultiModalStationForStations() {
        return graph.index.getMultiModalStationForStations();
    }

    /** Dynamically generate the set of Routes passing though a Stop on demand. */
    public Set<Route> routesForStop(Stop stop) {
        Set<Route> routes = Sets.newHashSet();
        for (TripPattern p : getPatternsForStop().get(stop)) {
            routes.add(p.route);
        }
        return routes;
    }

    /**
     * Fetch upcoming vehicle departures from a stop.
     * It goes though all patterns passing the stop for the previous, current and next service date.
     * It uses a priority queue to keep track of the next departures. The queue is shared between all dates, as services
     * from the previous service date can visit the stop later than the current service date's services. This happens
     * eg. with sleeper trains.
     *
     * TODO: Add frequency based trips
     * @param stop Stop object to perform the search for
     * @param startTime Start time for the search. Seconds from UNIX epoch
     * @param timeRange Searches forward for timeRange seconds from startTime
     * @param numberOfDepartures Number of departures to fetch per pattern
     * @param omitNonPickups If true, do not include vehicles that will not pick up passengers.
     * @return
     */
    public List<StopTimesInPattern> stopTimesForStop(Stop stop, long startTime, int timeRange, int numberOfDepartures, boolean omitNonPickups) {

        if (startTime == 0) {
            startTime = System.currentTimeMillis() / 1000;
        }
        List<StopTimesInPattern> ret = new ArrayList<>();
        TimetableSnapshot snapshot = graph.getTimetableSnapshot();
        Date date = new Date(startTime * 1000);
        ServiceDate[] serviceDates = {new ServiceDate(date).previous(), new ServiceDate(date), new ServiceDate(date).next()};

        Collection<TripPattern> patternsForStop = getPatternsForStop(stop, true);

        for (TripPattern pattern : patternsForStop) {

            // The bounded priority Q is used to keep a sorted short list of trip times. We can not
            // relay on the trip times to be in order because of real-time updates. This code can
            // probably be optimized, and the trip search in the Raptor search does almost the same
            // thing. This is no part of a routing request, but is a used frequently in some
            // operation like Entur for "departure boards" (apps, widgets, screens on platforms, and
            // hotel lobbies). Setting the numberOfDepartures and timeRange to a big number for a
            // transit hub could result in a DOS attack, but there are probably other more effective
            // ways to do it.
            //
            // The {@link MinMaxPriorityQueue} is marked beta, but we do not have a god alternative.
            MinMaxPriorityQueue<TripTimeShort> pq = MinMaxPriorityQueue
                    .orderedBy(Comparator.comparing((TripTimeShort tts) -> tts.serviceDay + tts.realtimeDeparture))
                    .maximumSize(numberOfDepartures)
                    .create();

            // Loop through all possible days
            for (ServiceDate serviceDate : serviceDates) {
                ServiceDay sd = new ServiceDay(graph, serviceDate, graph.getCalendarService(), pattern.route.getAgency().getId());
                Timetable tt;
                if (snapshot != null){
                    tt = snapshot.resolve(pattern, serviceDate);
                } else {
                    tt = pattern.scheduledTimetable;
                }

                if (!tt.temporallyViable(sd, startTime, timeRange, true)) continue;

                int secondsSinceMidnight = sd.secondsSinceMidnight(startTime);
                int sidx = 0;
                for (Stop currStop : pattern.stopPattern.stops) {
                    if (currStop == stop) {
                        if(omitNonPickups && pattern.stopPattern.pickups[sidx] == pattern.stopPattern.PICKDROP_NONE) continue;
                        for (TripTimes t : tt.tripTimes) {
                            if (!sd.serviceRunning(t.serviceCode)) continue;
                            if (t.getDepartureTime(sidx) != -1 &&
                                    t.getDepartureTime(sidx) >= secondsSinceMidnight) {
                                pq.add(new TripTimeShort(t, sidx, stop, sd));
                            }
                        }

                        // TODO: This needs to be adapted after #1647 is merged
                        for (FrequencyEntry freq : tt.frequencyEntries) {
                            if (!sd.serviceRunning(freq.tripTimes.serviceCode)) continue;
                            int departureTime = freq.nextDepartureTime(sidx, secondsSinceMidnight);
                            if (departureTime == -1) continue;
                            int lastDeparture = freq.endTime + freq.tripTimes.getArrivalTime(sidx) -
                                    freq.tripTimes.getDepartureTime(0);
                            int i = 0;
                            while (departureTime <= lastDeparture && i < numberOfDepartures) {
                                pq.add(
                                    new TripTimeShort(
                                        freq.materialize(sidx, departureTime, true),
                                        sidx,
                                        stop,
                                        sd
                                    )
                                );
                                departureTime += freq.headway;
                                i++;
                            }
                        }
                    }
                    sidx++;
                }
            }

            if (pq.size() != 0) {
                StopTimesInPattern stopTimes = new StopTimesInPattern(pattern);
                while (pq.size() != 0) {
                    stopTimes.times.add(0, pq.poll());
                }
                ret.add(stopTimes);
            }
        }
        return ret;
    }

    /**
     * Get a list of all trips that pass through a stop during a single ServiceDate. Useful when creating complete stop
     * timetables for a single day.
     *
     * @param stop Stop object to perform the search for
     * @param serviceDate Return all departures for the specified date
     * @return
     */
    public List<StopTimesInPattern> getStopTimesForStop(Stop stop, ServiceDate serviceDate, boolean omitNonPickups) {
        List<StopTimesInPattern> ret = new ArrayList<>();
        TimetableSnapshot snapshot = graph.getTimetableSnapshot();

        Collection<TripPattern> patternsForStop = getPatternsForStop(stop, true);
        for (TripPattern pattern : patternsForStop) {
            StopTimesInPattern stopTimes = new StopTimesInPattern(pattern);
            Timetable tt;
            if (snapshot != null){
                tt = snapshot.resolve(pattern, serviceDate);
            } else {
                tt = pattern.scheduledTimetable;
            }
            ServiceDay sd = new ServiceDay(graph, serviceDate, graph.getCalendarService(), pattern.route.getAgency().getId());
            int sidx = 0;
            for (Stop currStop : pattern.stopPattern.stops) {
                if (currStop == stop) {
                    if(omitNonPickups && pattern.stopPattern.pickups[sidx] == pattern.stopPattern.PICKDROP_NONE) continue;
                    for (TripTimes t : tt.tripTimes) {
                        if (!sd.serviceRunning(t.serviceCode)) continue;
                        stopTimes.times.add(new TripTimeShort(t, sidx, stop, sd));
                    }
                }
                sidx++;
            }
            ret.add(stopTimes);
        }
        return ret;
    }

    /**
     * Returns all the patterns for a specific stop. If includeRealtimeUpdates is set, new patterns
     * added by realtime updates are added to the collection.
     */
    public Collection<TripPattern> getPatternsForStop(Stop stop, boolean includeRealtimeUpdates) {
        List<TripPattern> tripPatterns = new ArrayList<>(getPatternsForStop().get(stop));

        TimetableSnapshot timetableSnapshot = graph.getTimetableSnapshot();

        if (includeRealtimeUpdates && timetableSnapshot != null) {
            tripPatterns.addAll(timetableSnapshot.getPatternsForStop(stop));
        }

        return tripPatterns;
    }

    /**
     * Get the most up-to-date timetable for the given TripPattern, as of right now.
     * There should probably be a less awkward way to do this that just gets the latest entry from the resolver without
     * making a fake routing request.
     */
    public Timetable currentUpdatedTimetableForTripPattern (TripPattern tripPattern) {
        // The timetableSnapshot will be null if there's no real-time data being applied.
        if (graph.getTimetableSnapshot() == null) return tripPattern.scheduledTimetable;
        // Get the updated times for right now, which is the only reasonable default since no date is supplied.
        Calendar calendar = Calendar.getInstance();
        ServiceDate serviceDate = new ServiceDate(calendar.getTime());
        return graph.getTimetableSnapshot().resolve(tripPattern, serviceDate);
    }

    public Response getGraphQLResponse(String query, Map<String, Object> variables, String operationName) {
        ExecutionResult executionResult = graphQL.execute(query, operationName, null, variables);

        return HttpToGraphQLMapper.mapExecutionResultToHttpResponse(executionResult);
    }

    /**
     * Fetch an agency by its string ID, ignoring the fact that this ID should be scoped by a feedId.
     * This is a stopgap (i.e. hack) method for fetching agencies where no feed scope is available.
     * I am creating this method only to allow merging pull request #2032 which adds GraphQL.
     * Note that if the same agency ID is defined in several feeds, this will return one of them
     * at random. That is obviously not the right behavior. The problem is that agencies are
     * not currently keyed on an FeedScopedId object, but on separate feedId and id Strings.
     * A real fix will involve replacing or heavily modifying the OBA GTFS loader, which is now
     * possible since we have forked it.
     */
    public Agency getAgencyWithoutFeedId(String agencyId) {
        // Iterate over the agency map for each feed.
        for (Map<String, Agency> agencyForId : getAgenciesForFeedId().values()) {
            Agency agency = agencyForId.get(agencyId);
            if (agency != null) {
                return agency;
            }
        }
        return null;
    }

    /**
     * Construct a set of all Agencies in this graph, spanning across all feed IDs.
     * I am creating this method only to allow merging pull request #2032 which adds GraphQL.
     * This should probably be done some other way, see javadoc on getAgencyWithoutFeedId.
     */
    public Set<Agency> getAllAgencies() {
        Set<Agency> allAgencies = new HashSet<>();
        for (Map<String, Agency> agencyForId : getAgenciesForFeedId().values()) {
            allAgencies.addAll(agencyForId.values());
        }
        return allAgencies;
    }

    public Collection<Notice> getNoticesByEntity(TransitEntity<?> entity) {
        // Delegate to graph
        Collection<Notice> res = graph.getNoticesByElement().get(entity);
        return res == null ? Collections.emptyList() : res;
    }

    /**
     * Get a list of all operators spanning across all feeds.
     */
    public Collection<Operator> getAllOperators() {
        return getOperatorForId().values();
    }

    public BitSet getServicesRunningForDate(ServiceDate date) {
        return graph.servicesRunning(date);
    }

    public TripPattern getTripPatternForId(String id) {
        return graph.tripPatternForId.get(id);
    }

    public Collection<TripPattern> getTripPatterns() {
        return graph.tripPatternForId.values();
    }

    public Collection<Notice> getNoticesByElement(TransitEntity<?> transitEntity) {
        return graph.getNoticesByElement().get(transitEntity);
    }

    public Collection<Notice> getNotices() {
        return graph.getNoticesByElement().values();
    }

    public List<TripTimeShort> getStopTimesForTripAndDate(Trip trip, ServiceDate serviceDate) {
        TimetableSnapshot tts = graph.getTimetableSnapshot();
        if (tts == null) {
            return null;
        } else {
            TripPattern pattern = getPatternForTrip().get(trip);
            return TripTimeShort.fromTripTimes(tts.resolve(pattern, serviceDate), trip);
        }
    }

    public Collection<Stop> getStopsByBoundingBox(Envelope envelope) {
        return graph.streetIndex
            .getTransitStopForEnvelope(envelope)
            .stream()
            .map(TransitStopVertex::getStop)
            .collect(Collectors.toList());
    }

    public Station getStationById(FeedScopedId id) {
        return graph.stationById.get(id);
    }

    public Collection<Station> getStations() {
        return graph.stationById.values();
    }

    public CalendarService getCalendarService() {
        return graph.getCalendarService();
    }

    public TimeZone getTimeZone() {
        return graph.getTimeZone();
    }

    public BikeRentalStationService getBikerentalStationService() {
        return graph.getService(BikeRentalStationService.class);
    }

    public AlertPatchService getSiriAlertPatchService() {
        return graph.getSiriAlertPatchService();
    }

    public MultiModalStation getMultiModalStationById(FeedScopedId feedScopedId) {
        return graph.multiModalStationById.get(feedScopedId);
    }

    public List<TripTimeShort> getTripTimesShort(Trip trip, ServiceDate serviceDate) {
        final ServiceDay serviceDay = new ServiceDay(graph, serviceDate,
            getCalendarService(), trip.getRoute().getAgency().getId());
        TimetableSnapshot timetableSnapshot = graph.getTimetableSnapshot();
        Timetable timetable = null;
        if (timetableSnapshot != null) {
            // Check if realtime-data is available for trip

            TripPattern pattern = timetableSnapshot.getLastAddedTripPattern(
                trip.getId(), serviceDate);
            if (pattern == null) {
                pattern = getPatternForTrip().get(trip);
            }
            timetable = timetableSnapshot.resolve(pattern, serviceDate);
        }
        if (timetable == null) {
            timetable = getPatternForTrip().get(trip).scheduledTimetable;
        }

        // This check is made here to avoid changing TripTimeShort.fromTripTimes
        TripTimes times = timetable.getTripTimes(timetable.getTripIndex(trip.getId()));
        if (!serviceDay.serviceRunning(times.serviceCode)) {
            return new ArrayList<>();
        }
        else {
            return TripTimeShort.fromTripTimes(timetable, trip, serviceDay);
        }
    }

    public GraphQL getGraphQL() {
        return this.graphQL;
    }
}
