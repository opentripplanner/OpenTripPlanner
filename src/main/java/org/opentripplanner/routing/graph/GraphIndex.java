package org.opentripplanner.routing.graph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.geometry.CompactElevationProfile;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GraphIndex {

    private static final Logger LOG = LoggerFactory.getLogger(GraphIndex.class);

    // TODO: consistently key on model object or id string
    private final Map<FeedScopedId, Agency> agencyForId = Maps.newHashMap();
    private final Map<FeedScopedId, Operator> operatorForId = Maps.newHashMap();
    private final Map<String, FeedInfo> feedInfoForId = Maps.newHashMap();
    private final Map<FeedScopedId, Stop> stopForId = Maps.newHashMap();
    private final Map<FeedScopedId, Trip> tripForId = Maps.newHashMap();
    private final Map<FeedScopedId, Route> routeForId = Maps.newHashMap();
    private final Map<Stop, TransitStopVertex> stopVertexForStop = Maps.newHashMap();
    private final Map<Trip, TripPattern> patternForTrip = Maps.newHashMap();
    private final Multimap<String, TripPattern> patternsForFeedId = ArrayListMultimap.create();
    private final Multimap<Route, TripPattern> patternsForRoute = ArrayListMultimap.create();
    private final Multimap<Stop, TripPattern> patternsForStopId = ArrayListMultimap.create();
    private final Map<Station, MultiModalStation> multiModalStationForStations = Maps.newHashMap();
    private final HashGridSpatialIndex<TransitStopVertex> stopSpatialIndex = new HashGridSpatialIndex<>();
    private final Map<ServiceDate, TIntSet> serviceCodesRunningForDate = new HashMap<>();

    public GraphIndex(Graph graph) {
        LOG.info("GraphIndex init...");
        CompactElevationProfile.setDistanceBetweenSamplesM(graph.getDistanceBetweenElevationSamples());

        for (Agency agency : graph.getAgencies()) {
            this.agencyForId.put(agency.getId(), agency);
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
            for (Stop stop : pattern.getStops()) {
                patternsForStopId.put(stop, pattern);
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

        initalizeServiceCodesForDate(graph);

        LOG.info("GraphIndex init complete.");
    }

    private void initalizeServiceCodesForDate(Graph graph) {

        CalendarService calendarService = graph.getCalendarService();

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
            Set<ServiceDate> serviceDatesForService = calendarService.getServiceDatesForServiceId(
                    serviceId);
            for (ServiceDate serviceDate : serviceDatesForService) {
                serviceIdsForServiceDate.put(serviceDate, serviceId);
            }
        }
        for (ServiceDate serviceDate : serviceIdsForServiceDate.keySet()) {
            TIntSet serviceCodesRunning = new TIntHashSet();
            for (FeedScopedId serviceId : serviceIdsForServiceDate.get(serviceDate)) {
                serviceCodesRunning.add(graph.getServiceCodes().get(serviceId));
            }
            serviceCodesRunningForDate.put(serviceDate, serviceCodesRunning);
        }
    }

    public Agency getAgencyForId(FeedScopedId id) {
        return agencyForId.get(id);
    }

    public Stop getStopForId(FeedScopedId id) {
        return stopForId.get(id);
    }

    public Route getRouteForId(FeedScopedId id) {
        return routeForId.get(id);
    }

    /**
     * TODO OTP2 - This is NOT THREAD-SAFE and is used in the real-time updaters, we need to fix
     *           - this when doing the issue #3030.
     */
    public void addRoutes(Route route) {
        routeForId.put(route.getId(), route);
    }

    /** Dynamically generate the set of Routes passing though a Stop on demand. */
    public Set<Route> getRoutesForStop(Stop stop) {
        Set<Route> routes = Sets.newHashSet();
        for (TripPattern p : getPatternsForStop(stop)) {
            routes.add(p.route);
        }
        return routes;
    }

    public Collection<TripPattern> getPatternsForStop(Stop stop) {
        return patternsForStopId.get(stop);
    }

    /**
     * Returns all the patterns for a specific stop. If includeRealtimeUpdates is set, new patterns
     * added by realtime updates are added to the collection. A set is used here because trip
     * patterns that were updated by realtime data is both part of the GraphIndex and the
     * TimetableSnapshot.
     */
    public Collection<TripPattern> getPatternsForStop(
            Stop stop,
            TimetableSnapshot timetableSnapshot
    ) {
        Set<TripPattern> tripPatterns = new HashSet<>(getPatternsForStop(stop));

        if (timetableSnapshot != null) {
            tripPatterns.addAll(timetableSnapshot.getPatternsForStop(stop));
        }

        return tripPatterns;
    }

    /**
     * Get a list of all operators spanning across all feeds.
     */
    public Collection<Operator> getAllOperators() {
        return getOperatorForId().values();
    }

    public Map<FeedScopedId, Operator> getOperatorForId() {
        return operatorForId;
    }

    public Map<String, FeedInfo> getFeedInfoForId() {
        return feedInfoForId;
    }

    public Collection<Stop> getAllStops() {
        return stopForId.values();
    }

    public Map<FeedScopedId, Trip> getTripForId() {
        return tripForId;
    }

    public Collection<Route> getAllRoutes() {
        return routeForId.values();
    }

    public Map<Stop, TransitStopVertex> getStopVertexForStop() {
        return stopVertexForStop;
    }

    public Map<Trip, TripPattern> getPatternForTrip() {
        return patternForTrip;
    }

    public Multimap<String, TripPattern> getPatternsForFeedId() {
        return patternsForFeedId;
    }

    public Multimap<Route, TripPattern> getPatternsForRoute() {
        return patternsForRoute;
    }

    public Map<Station, MultiModalStation> getMultiModalStationForStations() {
        return multiModalStationForStations;
    }

    public HashGridSpatialIndex<TransitStopVertex> getStopSpatialIndex() {
        return stopSpatialIndex;
    }

    public Map<ServiceDate, TIntSet> getServiceCodesRunningForDate() {
        return serviceCodesRunningForDate;
    }
}
