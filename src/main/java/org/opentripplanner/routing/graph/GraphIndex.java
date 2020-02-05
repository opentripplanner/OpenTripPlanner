package org.opentripplanner.routing.graph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.MinMaxPriorityQueue;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.execution.ExecutorServiceExecutionStrategy;
import org.joda.time.LocalDate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.geometry.CompactElevationProfile;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.index.IndexGraphQLSchema;
import org.opentripplanner.index.model.StopTimesInPattern;
import org.opentripplanner.index.model.TripTimeShort;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.GroupOfStations;
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
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.algorithm.astar.TraverseVisitor;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.spt.DominanceFunction;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * This class contains all the transient indexes of graph elements -- those that are not
 * serialized with the graph. Caching these maps is essentially an optimization, but a big one.
 * The index is bootstrapped from the graph's list of edges.
 */
public class GraphIndex {

    private static final Logger LOG = LoggerFactory.getLogger(GraphIndex.class);

    // TODO: consistently key on model object or id string
    public final Map<String, Map<String, Agency>> agenciesForFeedId = Maps.newHashMap();
    public final Map<FeedScopedId, Operator> operatorForId = Maps.newHashMap();
    public final Map<String, FeedInfo> feedInfoForId = Maps.newHashMap();
    public final Map<FeedScopedId, Stop> stopForId = Maps.newHashMap();
    public final Map<FeedScopedId, Trip> tripForId = Maps.newHashMap();
    public final Map<FeedScopedId, Route> routeForId = Maps.newHashMap();
    public final Map<Stop, TransitStopVertex> stopVertexForStop = Maps.newHashMap();
    public final Map<Trip, TripPattern> patternForTrip = Maps.newHashMap();
    public final Multimap<String, TripPattern> patternsForFeedId = ArrayListMultimap.create();
    public final Multimap<Route, TripPattern> patternsForRoute = ArrayListMultimap.create();
    public final Multimap<Stop, TripPattern> patternsForStop = ArrayListMultimap.create();
    public final Map<Station, MultiModalStation> multiModalStationForStations = Maps.newHashMap();
    final HashGridSpatialIndex<TransitStopVertex> stopSpatialIndex = new HashGridSpatialIndex<>();
    public final Map<ServiceDate, TIntSet> serviceCodesRunningForDate = new HashMap<>();

    /* Should eventually be replaced with new serviceId indexes. */
    private final CalendarService calendarService;
    private final Map<FeedScopedId,Integer> serviceCodes;

    /* This is a workaround, and should probably eventually be removed. */
    public Graph graph;

    /** Used for finding first/last trip of the day. This is the time at which service ends for the day. */
    public final int overnightBreak = 60 * 60 * 2; // FIXME not being set, this was done in transitIndex

    public GraphQL graphQL;

    public GraphIndex (Graph graph) {
        LOG.info("Indexing graph...");

        CompactElevationProfile.setDistanceBetweenSamplesM(graph.getDistanceBetweenElevationSamples());

        for (String feedId : graph.getFeedIds()) {
            for (Agency agency : graph.getAgencies(feedId)) {
                Map<String, Agency> agencyForId = agenciesForFeedId.getOrDefault(feedId, new HashMap<>());
                agencyForId.put(agency.getId(), agency);
                this.agenciesForFeedId.put(feedId, agencyForId);
            }
            this.feedInfoForId.put(feedId, graph.getFeedInfo(feedId));
        }

        for (Operator operator : graph.getOperators()) {
            this.operatorForId.put(operator.getId(), operator);
        }

        /* We will keep a separate set of all vertices in case some have the same label.
         * Maybe we should just guarantee unique labels. */
        for (Vertex vertex : graph.getVertices()) {
            if (vertex instanceof TransitStopVertex) {
                TransitStopVertex stopVertex = (TransitStopVertex) vertex;
                Stop stop = stopVertex.getStop();
                stopForId.put(stop.getId(), stop);
                stopVertexForStop.put(stop, stopVertex);
            }
        }
        for (TransitStopVertex stopVertex : stopVertexForStop.values()) {
            Envelope envelope = new Envelope(stopVertex.getCoordinate());
            stopSpatialIndex.insert(envelope, stopVertex);
        }
        for (TripPattern pattern : graph.tripPatternForId.values()) {
            patternsForFeedId.put(pattern.getFeedId(), pattern);
            patternsForRoute.put(pattern.route, pattern);
            for (Trip trip : pattern.getTrips()) {
                patternForTrip.put(trip, pattern);
                tripForId.put(trip.getId(), trip);
            }
            for (Stop stop: pattern.getStops()) {
                patternsForStop.put(stop, pattern);
            }
        }
        for (Route route : patternsForRoute.asMap().keySet()) {
            routeForId.put(route.getId(), route);
        }
        for (MultiModalStation multiModalStation : graph.multiModalStationById.values()) {
            for (Station childStation : multiModalStation.getChildStations()) {
                multiModalStationForStations.put(childStation, multiModalStation);
            }
        }

        // Copy these two service indexes from the graph until we have better ones.
        calendarService = graph.getCalendarService();
        serviceCodes = graph.serviceCodes;
        this.graph = graph;
        graphQL = new GraphQL(
            new IndexGraphQLSchema(this).indexSchema,
            new ExecutorServiceExecutionStrategy(Executors.newCachedThreadPool(
                new ThreadFactoryBuilder().setNameFormat("GraphQLExecutor-" + graph.routerId + "-%d").build()
            )));
        initalizeServiceCodesForDate(graph);

        LOG.info("Done indexing graph.");
    }

    private void initalizeServiceCodesForDate(Graph graph) {

        if (calendarService == null) { return; }

        // CalendarService has one main implementation (CalendarServiceImpl) which contains a
        // CalendarServiceData which can easily supply all of the dates. But it's impossible to
        // actually see those dates without modifying the interfaces and inheritance. So we have
        // to work around this abstraction and reconstruct the CalendarData.
        // Note the "multiCalendarServiceImpl" which has docs saying it expects one single
        // CalendarData. It seems to merge the calendar services from multiple GTFS feeds, but
        // its only documentation says it's a hack.
        // TODO OTP2 - This cleanup is added to the 'Final cleanup OTP2' issue #2757

        // Reconstruct set of all dates where service is defined, keeping track of which services
        // run on which days.
        Multimap<ServiceDate, FeedScopedId> serviceIdsForServiceDate = HashMultimap.create();

        for (FeedScopedId serviceId : calendarService.getServiceIds()) {
            Set<ServiceDate> serviceDatesForService = calendarService.getServiceDatesForServiceId(serviceId);
            for (ServiceDate serviceDate : serviceDatesForService) {
                serviceIdsForServiceDate.put(serviceDate, serviceId);
            }
        }
        for (ServiceDate serviceDate : serviceIdsForServiceDate.keySet()) {
            TIntSet serviceCodesRunning = new TIntHashSet();
            for (FeedScopedId serviceId : serviceIdsForServiceDate.get(serviceDate)) {
                serviceCodesRunning.add(graph.serviceCodes.get(serviceId));
            }
            serviceCodesRunningForDate.put(
                serviceDate,
                serviceCodesRunning
            );
        }
    }

    /* TODO: an almost similar function exists in ProfileRouter, combine these.
    *  Should these live in a separate class? */
    public List<StopAndDistance> findClosestStopsByWalking(double lat, double lon, int radius) {
        // Make a normal OTP routing request so we can traverse edges and use GenericAStar
        // TODO make a function that builds normal routing requests from profile requests
        RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
        rr.from = new GenericLocation(null, null, lat, lon);
        // FIXME requires destination to be set, not necessary for analyst
        rr.to = new GenericLocation(null, null, lat, lon);
        rr.oneToMany = true;
        rr.setRoutingContext(graph);
        rr.walkSpeed = 1;
        rr.dominanceFunction = new DominanceFunction.LeastWalk();
        // RR dateTime defaults to currentTime.
        // If elapsed time is not capped, searches are very slow.
        rr.worstTime = (rr.dateTime + radius);
        AStar astar = new AStar();
        rr.setNumItineraries(1);
        StopFinderTraverseVisitor visitor = new StopFinderTraverseVisitor();
        astar.setTraverseVisitor(visitor);
        astar.getShortestPathTree(rr, 1); // timeout in seconds
        // Destroy the routing context, to clean up the temporary edges & vertices
        rr.rctx.destroy();
        return visitor.stopsFound;
    }

    public static class StopAndDistance {
        public Stop stop;
        public int distance;

        public StopAndDistance(Stop stop, int distance){
            this.stop = stop;
            this.distance = distance;
        }
    }

    static private class StopFinderTraverseVisitor implements TraverseVisitor {
        List<StopAndDistance> stopsFound = new ArrayList<>();
        @Override public void visitEdge(Edge edge, State state) { }
        @Override public void visitEnqueue(State state) { }
        // Accumulate stops into ret as the search runs.
        @Override public void visitVertex(State state) {
            Vertex vertex = state.getVertex();
            if (vertex instanceof TransitStopVertex) {
                stopsFound.add(new StopAndDistance(((TransitStopVertex) vertex).getStop(),
                    (int) state.getElapsedTimeSeconds()));
            }
        }
    }


    /** An OBA Service Date is a local date without timezone, only year month and day. */
    public BitSet servicesRunning (ServiceDate date) {
        BitSet services = new BitSet(calendarService.getServiceIds().size());
        for (FeedScopedId serviceId : calendarService.getServiceIdsOnDate(date)) {
            int n = serviceCodes.get(serviceId);
            if (n < 0) continue;
            services.set(n);
        }
        return services;
    }

    /**
     * Wraps the other servicesRunning whose parameter is an OBA ServiceDate.
     * Joda LocalDate is a similar class.
     */
    public BitSet servicesRunning (LocalDate date) {
        return servicesRunning(new ServiceDate(date.getYear(), date.getMonthOfYear(), date.getDayOfMonth()));
    }

    /** Dynamically generate the set of Routes passing though a Stop on demand. */
    public Set<Route> routesForStop(Stop stop) {
        Set<Route> routes = Sets.newHashSet();
        for (TripPattern p : patternsForStop.get(stop)) {
            routes.add(p.route);
        }
        return routes;
    }

    /**
     * Fetch upcoming vehicle departures from a stop.
     * Fetches two departures for each pattern during the next 24 hours as default
     */
    public Collection<StopTimesInPattern> stopTimesForStop(Stop stop, boolean omitNonPickups) {
        return stopTimesForStop(stop, System.currentTimeMillis()/1000, 24 * 60 * 60, 2, omitNonPickups);
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
                ServiceDay sd = new ServiceDay(graph, serviceDate, calendarService, pattern.route.getAgency().getId());
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

        Collection<TripPattern> patterns = getPatternsForStop(stop, true);
        for (TripPattern pattern : patterns) {
            StopTimesInPattern stopTimes = new StopTimesInPattern(pattern);
            Timetable tt;
            if (snapshot != null){
                tt = snapshot.resolve(pattern, serviceDate);
            } else {
                tt = pattern.scheduledTimetable;
            }
            ServiceDay sd = new ServiceDay(graph, serviceDate, calendarService, pattern.route.getAgency().getId());
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

    public Collection<TripPattern> getPatternsForStop(Stop stop, boolean includeRealtimeUpdates) {
        List<TripPattern> tripPatterns = new ArrayList<>(patternsForStop.get(stop));

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
        for (Map<String, Agency> agencyForId : agenciesForFeedId.values()) {
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
        for (Map<String, Agency> agencyForId : agenciesForFeedId.values()) {
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
        return operatorForId.values();
    }

    /**
     *
     * @param id Id of Stop, Station, MultiModalStation or GroupOfStations
     * @return The associated TransitStopVertex or all underlying TransitStopVertices
     */
    public Set<Vertex> getStopVerticesById(FeedScopedId id) {
        Collection<Stop> stops = getStopsForId(id);

        if (stops == null) {
            return null;
        }

        return stops.stream().map(stopVertexForStop::get).collect(Collectors.toSet());
    }

    private Collection<Stop> getStopsForId(FeedScopedId id) {

        // GroupOfStations
        GroupOfStations groupOfStations = graph.groupOfStationsById.get(id);
        if (groupOfStations != null) {
            return groupOfStations.getChildStops();
        }

        // Multimodal station
        MultiModalStation multiModalStation = graph.multiModalStationById.get(id);
        if (multiModalStation != null) {
            return multiModalStation.getChildStops();
        }

        // Station
        Station station = graph.stationById.get(id);
        if (station != null) {
            return station.getChildStops();
        }
        // Single stop
        Stop stop = graph.index.stopForId.get(id);
        if (stop != null) {
            return Collections.singleton(stop);
        }

        return null;
    }

    public Map<ServiceDate, TIntSet> getServiceCodesRunningForDate() {
        return this.serviceCodesRunningForDate;
    }
}
