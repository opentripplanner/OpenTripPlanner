package org.opentripplanner.routing;

import com.google.common.collect.Multimap;
import gnu.trove.set.TIntSet;
import java.io.Serializable;
import java.time.Instant;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Function;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.common.geometry.HashGridSpatialIndex;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.flex.FlexIndex;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource.DrivingDirection;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.GraphBundle;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.PathTransfer;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTimesInPattern;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TimetableSnapshotProvider;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.RoutingWorker;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TransitLayer;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.api.response.RoutingResponse;
import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCostModel;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.graphfinder.GraphFinder;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.routing.graphfinder.PlaceAtDistance;
import org.opentripplanner.routing.graphfinder.PlaceType;
import org.opentripplanner.routing.impl.StreetVertexIndex;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.routing.stoptimes.ArrivalDeparture;
import org.opentripplanner.routing.stoptimes.StopTimesHelper;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.util.WorldEnvelope;

/**
 * This is the entry point of all API requests towards the OTP graph. A new instance of this class
 * should be created for each request. This ensures that the same TimetableSnapshot is used for the
 * duration of the request (which may involve several method calls).
 */
public class RoutingService {

    private final Graph graph;

    private final GraphIndex graphIndex;

    private final GraphFinder graphFinder;

    /**
     * This should only be accessed through the getTimetableSnapshot method.
     */
    private TimetableSnapshot timetableSnapshot;

    public RoutingService(Graph graph) {
        this.graph = graph;
        this.graphIndex = graph.index;
        this.graphFinder = GraphFinder.getInstance(graph);
    }

    // TODO We should probably not have the Router as a parameter here
    public RoutingResponse route(RoutingRequest request, Router router) {
        try {
            var zoneId = graph.getTimeZone().toZoneId();
            RoutingWorker worker = new RoutingWorker(router, request, zoneId);
            return worker.route();
        } finally {
            if (request != null) {
                request.cleanup();
            }
        }
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
     * @param stop                  Stop object to perform the search for
     * @param startTime             Start time for the search. Seconds from UNIX epoch
     * @param timeRange             Searches forward for timeRange seconds from startTime
     * @param numberOfDepartures    Number of departures to fetch per pattern
     * @param arrivalDeparture      Filter by arrivals, departures, or both
     * @param includeCancelledTrips If true, cancelled trips will also be included in result.
     */
    public List<StopTimesInPattern> stopTimesForStop(
            StopLocation stop, long startTime, int timeRange, int numberOfDepartures, ArrivalDeparture arrivalDeparture, boolean includeCancelledTrips
    ) {
        return StopTimesHelper.stopTimesForStop(
                this,
                lazyGetTimeTableSnapShot(),
                stop,
                startTime,
                timeRange,
                numberOfDepartures,
                arrivalDeparture,
                includeCancelledTrips
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
            StopLocation stop, ServiceDate serviceDate, ArrivalDeparture arrivalDeparture
    ) {
        return StopTimesHelper.stopTimesForStop(this, stop, serviceDate, arrivalDeparture);
    }


    /**
     * Fetch upcoming vehicle departures from a stop for a specific pattern, passing the stop
     * for the previous, current and next service date. It uses a priority queue to keep track of
     * the next departures. The queue is shared between all dates, as services from the previous
     * service date can visit the stop later than the current service date's services.
     * <p>
     * TODO: Add frequency based trips
     *
     * @param stop               Stop object to perform the search for
     * @param pattern            Pattern object to perform the search for
     * @param startTime          Start time for the search. Seconds from UNIX epoch
     * @param timeRange          Searches forward for timeRange seconds from startTime
     * @param numberOfDepartures Number of departures to fetch per pattern
     * @param arrivalDeparture   Filter by arrivals, departures, or both
     */
    public List<TripTimeOnDate> stopTimesForPatternAtStop(
            StopLocation stop, TripPattern pattern, long startTime, int timeRange, int numberOfDepartures, ArrivalDeparture arrivalDeparture
    ) {
        return StopTimesHelper.stopTimesForPatternAtStop(
                this,
                lazyGetTimeTableSnapShot(),
                stop,
                pattern,
                startTime,
                timeRange,
                numberOfDepartures,
                arrivalDeparture
        );
    }

    /**
     * Returns all the patterns for a specific stop. If includeRealtimeUpdates is set, new patterns
     * added by realtime updates are added to the collection.
     */
    public Collection<TripPattern> getPatternsForStop(StopLocation stop, boolean includeRealtimeUpdates) {
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
        ) : tripPattern.getScheduledTimetable();
    }

    public List<TripTimeOnDate> getTripTimesShort(Trip trip, ServiceDate serviceDate) {
        return TripTimesShortHelper.getTripTimesShort(this, trip, serviceDate);
    }

    /** {@link Graph#getTimetableSnapshot()} */
    public TimetableSnapshot getTimetableSnapshot() {return this.graph.getTimetableSnapshot();}

    /** {@link Graph#getOrSetupTimetableSnapshotProvider(Function)} */
    public <T extends TimetableSnapshotProvider> T getOrSetupTimetableSnapshotProvider(Function<Graph, T> creator) {
        return this.graph.getOrSetupTimetableSnapshotProvider(creator);
    }

    /** {@link Graph#addVertex(Vertex)} */
    public void addVertex(Vertex v) {this.graph.addVertex(v);}

    /** {@link Graph#removeEdge(Edge)} */
    public void removeEdge(Edge e) {this.graph.removeEdge(e);}

    /** {@link Graph#getVertex(String)} */
    public Vertex getVertex(String label) {return this.graph.getVertex(label);}

    /** {@link Graph#getVertices()} */
    public Collection<Vertex> getVertices() {return this.graph.getVertices();}

    /** {@link Graph#getVerticesOfType(Class)} */
    public <T extends Vertex> List<T> getVerticesOfType(Class<T> cls) {
        return this.graph.getVerticesOfType(cls);
    }

    /** {@link Graph#getEdges()} */
    public Collection<Edge> getEdges() {return this.graph.getEdges();}

    /** {@link Graph#getEdgesOfType(Class)} */
    public <T extends Edge> List<T> getEdgesOfType(Class<T> cls) {
        return this.graph.getEdgesOfType(cls);
    }

    /** {@link Graph#getStreetEdges()} */
    public Collection<StreetEdge> getStreetEdges() {return this.graph.getStreetEdges();}

    /** {@link Graph#getTransitLayer()} */
    public TransitLayer getTransitLayer() {return this.graph.getTransitLayer();}

    /** {@link Graph#setTransitLayer(TransitLayer)} */
    public void setTransitLayer(TransitLayer transitLayer) {
        this.graph.setTransitLayer(transitLayer);
    }

    /** {@link Graph#getRealtimeTransitLayer()} */
    public TransitLayer getRealtimeTransitLayer() {return this.graph.getRealtimeTransitLayer();}

    /** {@link Graph#hasRealtimeTransitLayer()} */
    public boolean hasRealtimeTransitLayer() {return this.graph.hasRealtimeTransitLayer();}

    /** {@link Graph#setRealtimeTransitLayer(TransitLayer)} */
    public void setRealtimeTransitLayer(TransitLayer realtimeTransitLayer) {
        this.graph.setRealtimeTransitLayer(realtimeTransitLayer);
    }

    /** {@link Graph#containsVertex(Vertex)} */
    public boolean containsVertex(Vertex v) {return this.graph.containsVertex(v);}

    /** {@link Graph#putService(Class, Serializable)} */
    public <T extends Serializable> T putService(
            Class<T> serviceType,
            T service
    ) {return this.graph.putService(serviceType, service);}

    /** {@link Graph#hasService(Class)} */
    public boolean hasService(Class<? extends Serializable> serviceType) {
        return this.graph.hasService(serviceType);
    }

    /** {@link Graph#getService(Class)} */
    public <T extends Serializable> T getService(Class<T> serviceType) {
        return this.graph.getService(serviceType);
    }

    /** {@link Graph#getService(Class, boolean)} */
    public <T extends Serializable> T getService(
            Class<T> serviceType,
            boolean autoCreate
    ) {return this.graph.getService(serviceType, autoCreate);}

    /** {@link Graph#remove(Vertex)} */
    public void remove(Vertex vertex) {this.graph.remove(vertex);}

    /** {@link Graph#removeIfUnconnected(Vertex)} */
    public void removeIfUnconnected(Vertex v) {this.graph.removeIfUnconnected(v);}

    /** {@link Graph#getExtent()} */
    public Envelope getExtent() {return this.graph.getExtent();}

    /** {@link Graph#getTransferService()} */
    public TransferService getTransferService() {return this.graph.getTransferService();}

    /** {@link Graph#updateTransitFeedValidity(CalendarServiceData, DataImportIssueStore)} */
    public void updateTransitFeedValidity(
            CalendarServiceData data,
            DataImportIssueStore issueStore
    ) {this.graph.updateTransitFeedValidity(data, issueStore);}

    /** {@link Graph#transitFeedCovers(Instant)} */
    public boolean transitFeedCovers(Instant time) {return this.graph.transitFeedCovers(time);}

    /** {@link Graph#getBundle()} */
    public GraphBundle getBundle() {return this.graph.getBundle();}

    /** {@link Graph#setBundle(GraphBundle)} */
    public void setBundle(GraphBundle bundle) {this.graph.setBundle(bundle);}

    /** {@link Graph#countVertices()} */
    public int countVertices() {return this.graph.countVertices();}

    /** {@link Graph#countEdges()} */
    public int countEdges() {return this.graph.countEdges();}

    /** {@link Graph#addTransitMode(TransitMode)} */
    public void addTransitMode(TransitMode mode) {this.graph.addTransitMode(mode);}

    /** {@link Graph#getTransitModes()} */
    public HashSet<TransitMode> getTransitModes() {return this.graph.getTransitModes();}

    /** {@link Graph#index()} */
    //public void index() {this.graph.index();}

    /** {@link Graph#getCalendarService()} */
    public CalendarService getCalendarService() {return this.graph.getCalendarService();}

    /** {@link Graph#getCalendarDataService()} */
    public CalendarServiceData getCalendarDataService() {return this.graph.getCalendarDataService();}

    /** {@link Graph#clearCachedCalenderService()} */
    public void clearCachedCalenderService() {this.graph.clearCachedCalenderService();}

    /** {@link Graph#getStreetIndex()} */
    public StreetVertexIndex getStreetIndex() {return this.graph.getStreetIndex();}

    /** {@link Graph#getLinker()} */
    public VertexLinker getLinker() {return this.graph.getLinker();}

    /** {@link Graph#getOrCreateServiceIdForDate(ServiceDate)} */
    public FeedScopedId getOrCreateServiceIdForDate(ServiceDate serviceDate) {
        return this.graph.getOrCreateServiceIdForDate(serviceDate);
    }

    /** {@link Graph#removeEdgelessVertices()} */
    public int removeEdgelessVertices() {return this.graph.removeEdgelessVertices();}

    /** {@link Graph#getFeedIds()} */
    public Collection<String> getFeedIds() {return this.graph.getFeedIds();}

    /** {@link Graph#getAgencies()} */
    public Collection<Agency> getAgencies() {return this.graph.getAgencies();}

    /** {@link Graph#getFeedInfo(String)} ()} */
    public FeedInfo getFeedInfo(String feedId) {return this.graph.getFeedInfo(feedId);}

    /** {@link Graph#addAgency(String, Agency)} */
    public void addAgency(String feedId, Agency agency) {this.graph.addAgency(feedId, agency);}

    /** {@link Graph#addFeedInfo(FeedInfo)} */
    public void addFeedInfo(FeedInfo info) {this.graph.addFeedInfo(info);}

    /** {@link Graph#getTimeZone()} */
    public TimeZone getTimeZone() {return this.graph.getTimeZone();}

    /** {@link Graph#getOperators()} */
    public Collection<Operator> getOperators() {return this.graph.getOperators();}

    /** {@link Graph#clearTimeZone()} */
    public void clearTimeZone() {this.graph.clearTimeZone();}

    /** {@link Graph#calculateEnvelope()} */
    public void calculateEnvelope() {this.graph.calculateEnvelope();}

    /** {@link Graph#calculateConvexHull()} */
    public void calculateConvexHull() {this.graph.calculateConvexHull();}

    /** {@link Graph#getConvexHull()} */
    public Geometry getConvexHull() {return this.graph.getConvexHull();}

    /** {@link Graph#expandToInclude(double, double)} ()} */
    public void expandToInclude(double x, double y) {this.graph.expandToInclude(x, y);}

    /** {@link Graph#getEnvelope()} */
    public WorldEnvelope getEnvelope() {return this.graph.getEnvelope();}

    /** {@link Graph#calculateTransitCenter()} */
    public void calculateTransitCenter() {this.graph.calculateTransitCenter();}

    /** {@link Graph#getCenter()} */
    public Optional<Coordinate> getCenter() {return this.graph.getCenter();}

    /** {@link Graph#getTransitServiceStarts()} */
    public long getTransitServiceStarts() {return this.graph.getTransitServiceStarts();}

    /** {@link Graph#getTransitServiceEnds()} */
    public long getTransitServiceEnds() {return this.graph.getTransitServiceEnds();}

    /** {@link Graph#getNoticesByElement()} */
    public Multimap<TransitEntity, Notice> getNoticesByElement() {return this.graph.getNoticesByElement();}

    /** {@link Graph#addNoticeAssignments(Multimap)} */
    public void addNoticeAssignments(Multimap<TransitEntity, Notice> noticesByElement) {
        this.graph.addNoticeAssignments(noticesByElement);
    }

    /** {@link Graph#getDistanceBetweenElevationSamples()} */
    public double getDistanceBetweenElevationSamples() {return this.graph.getDistanceBetweenElevationSamples();}

    /** {@link Graph#setDistanceBetweenElevationSamples(double)} */
    public void setDistanceBetweenElevationSamples(double distanceBetweenElevationSamples) {
        this.graph.setDistanceBetweenElevationSamples(distanceBetweenElevationSamples);
    }

    /** {@link Graph#getTransitAlertService()} */
    public TransitAlertService getTransitAlertService() {return this.graph.getTransitAlertService();}

    /** {@link Graph#getStopVerticesById(FeedScopedId)} */
    public Set<Vertex> getStopVerticesById(FeedScopedId id) {
        return this.graph.getStopVerticesById(id);
    }

    /** {@link Graph#getServicesRunningForDate(ServiceDate)} */
    public BitSet getServicesRunningForDate(ServiceDate date) {
        return this.graph.getServicesRunningForDate(date);
    }

    /** {@link Graph#getVehicleRentalStationService()} */
    public VehicleRentalStationService getVehicleRentalStationService() {return this.graph.getVehicleRentalStationService();}

    /** {@link Graph#getVehicleParkingService()} */
    public VehicleParkingService getVehicleParkingService() {return this.graph.getVehicleParkingService();}

    /** {@link Graph#getNoticesByEntity(TransitEntity)} */
    public Collection<Notice> getNoticesByEntity(TransitEntity entity) {
        return this.graph.getNoticesByEntity(entity);
    }

    /** {@link Graph#getTripPatternForId(FeedScopedId)} */
    public TripPattern getTripPatternForId(FeedScopedId id) {
        return this.graph.getTripPatternForId(id);
    }

    /** {@link Graph#getTripPatterns()} */
    public Collection<TripPattern> getTripPatterns() {return this.graph.getTripPatterns();}

    /** {@link Graph#getNotices()} */
    public Collection<Notice> getNotices() {return this.graph.getNotices();}

    /** {@link Graph#getStopsByBoundingBox(double, double, double, double)} */
    public Collection<StopLocation> getStopsByBoundingBox(
            double minLat,
            double minLon,
            double maxLat,
            double maxLon
    ) {return this.graph.getStopsByBoundingBox(minLat, minLon, maxLat, maxLon);}

    /** {@link Graph#getStopsInRadius(WgsCoordinate, double)} */
    public List<T2<Stop, Double>> getStopsInRadius(
            WgsCoordinate center,
            double radius
    ) {return this.graph.getStopsInRadius(center, radius);}

    /** {@link Graph#getStationById(FeedScopedId)} */
    public Station getStationById(FeedScopedId id) {return this.graph.getStationById(id);}

    /** {@link Graph#getMultiModalStation(FeedScopedId)} */
    public MultiModalStation getMultiModalStation(FeedScopedId id) {
        return this.graph.getMultiModalStation(id);
    }

    /** {@link Graph#getStations()} */
    public Collection<Station> getStations() {return this.graph.getStations();}

    /** {@link Graph#getServiceCodes()} */
    public Map<FeedScopedId, Integer> getServiceCodes() {return this.graph.getServiceCodes();}

    /** {@link Graph#getTransfersByStop(StopLocation)} */
    public Collection<PathTransfer> getTransfersByStop(StopLocation stop) {
        return this.graph.getTransfersByStop(stop);
    }

    /** {@link Graph#getDrivingDirection()} */
    public DrivingDirection getDrivingDirection() {return this.graph.getDrivingDirection();}

    /** {@link Graph#setDrivingDirection(DrivingDirection)} */
    public void setDrivingDirection(DrivingDirection drivingDirection) {
        this.graph.setDrivingDirection(drivingDirection);
    }

    /** {@link Graph#getIntersectionTraversalModel()} */
    public IntersectionTraversalCostModel getIntersectionTraversalModel() {return this.graph.getIntersectionTraversalModel();}

    /** {@link Graph#setIntersectionTraversalCostModel(IntersectionTraversalCostModel)} */
    public void setIntersectionTraversalCostModel(IntersectionTraversalCostModel intersectionTraversalCostModel) {
        this.graph.setIntersectionTraversalCostModel(intersectionTraversalCostModel);
    }

    /** {@link Graph#getLocationById(FeedScopedId)} */
    public FlexStopLocation getLocationById(FeedScopedId id) {
        return this.graph.getLocationById(id);
    }

    /** {@link Graph#getAllFlexStopsFlat()} */
    public Set<StopLocation> getAllFlexStopsFlat() {return this.graph.getAllFlexStopsFlat();}

    /** {@link GraphIndex#getAgencyForId(FeedScopedId)} */
    public Agency getAgencyForId(FeedScopedId id) {return this.graphIndex.getAgencyForId(id);}

    /** {@link GraphIndex#getStopForId(FeedScopedId)} */
    public StopLocation getStopForId(FeedScopedId id) {return this.graphIndex.getStopForId(id);}

    /** {@link GraphIndex#getRouteForId(FeedScopedId)} */
    public Route getRouteForId(FeedScopedId id) {return this.graphIndex.getRouteForId(id);}

    /** {@link GraphIndex#addRoutes(Route)} */
    public void addRoutes(Route route) {this.graphIndex.addRoutes(route);}

    /** {@link GraphIndex#getRoutesForStop(StopLocation)} */
    public Set<Route> getRoutesForStop(StopLocation stop) {
        return this.graphIndex.getRoutesForStop(stop);
    }

    /** {@link GraphIndex#getPatternsForStop(StopLocation)} */
    public Collection<TripPattern> getPatternsForStop(StopLocation stop) {
        return this.graphIndex.getPatternsForStop(stop);
    }

    /** {@link GraphIndex#getPatternsForStop(StopLocation, TimetableSnapshot)} */
    public Collection<TripPattern> getPatternsForStop(
            StopLocation stop,
            TimetableSnapshot timetableSnapshot
    ) {return this.graphIndex.getPatternsForStop(stop, timetableSnapshot);}

    /** {@link GraphIndex#getAllOperators()} */
    public Collection<Operator> getAllOperators() {return this.graphIndex.getAllOperators();}

    /** {@link GraphIndex#getOperatorForId()} */
    public Map<FeedScopedId, Operator> getOperatorForId() {return this.graphIndex.getOperatorForId();}

    /** {@link GraphIndex#getAllStops()} */
    public Collection<StopLocation> getAllStops() {return this.graphIndex.getAllStops();}

    /** {@link GraphIndex#getTripForId()} */
    public Map<FeedScopedId, Trip> getTripForId() {return this.graphIndex.getTripForId();}

    /** {@link GraphIndex#getAllRoutes()} */
    public Collection<Route> getAllRoutes() {return this.graphIndex.getAllRoutes();}

    /** {@link GraphIndex#getStopVertexForStop()} */
    public Map<Stop, TransitStopVertex> getStopVertexForStop() {return this.graphIndex.getStopVertexForStop();}

    /** {@link GraphIndex#getPatternForTrip()} */
    public Map<Trip, TripPattern> getPatternForTrip() {return this.graphIndex.getPatternForTrip();}

    /** {@link GraphIndex#getPatternsForFeedId()} */
    public Multimap<String, TripPattern> getPatternsForFeedId() {return this.graphIndex.getPatternsForFeedId();}

    /** {@link GraphIndex#getPatternsForRoute()} */
    public Multimap<Route, TripPattern> getPatternsForRoute() {return this.graphIndex.getPatternsForRoute();}

    /** {@link GraphIndex#getMultiModalStationForStations()} */
    public Map<Station, MultiModalStation> getMultiModalStationForStations() {return this.graphIndex.getMultiModalStationForStations();}

    /** {@link GraphIndex#getStopSpatialIndex()} */
    public HashGridSpatialIndex<TransitStopVertex> getStopSpatialIndex() {return this.graphIndex.getStopSpatialIndex();}

    /** {@link GraphIndex#getServiceCodesRunningForDate()} */
    public Map<ServiceDate, TIntSet> getServiceCodesRunningForDate() {return this.graphIndex.getServiceCodesRunningForDate();}

    /** {@link GraphIndex#getFlexIndex()} */
    public FlexIndex getFlexIndex() {return this.graphIndex.getFlexIndex();}

    /** {@link GraphFinder#findClosestStops(double, double, double)} */
    public List<NearbyStop> findClosestStops(
            double lat,
            double lon,
            double radiusMeters
    ) {return this.graphFinder.findClosestStops(lat, lon, radiusMeters);}

    /** {@link GraphFinder#findClosestPlaces(double, double, double, int, List, List, List, List, List, List, List, RoutingService)} */
    public List<PlaceAtDistance> findClosestPlaces(
            double lat,
            double lon,
            double radiusMeters,
            int maxResults,
            List<TransitMode> filterByModes,
            List<PlaceType> filterByPlaceTypes,
            List<FeedScopedId> filterByStops,
            List<FeedScopedId> filterByRoutes,
            List<String> filterByBikeRentalStations,
            List<String> filterByBikeParks,
            List<String> filterByCarParks,
            RoutingService routingService
    ) {
        return this.graphFinder.findClosestPlaces(lat, lon, radiusMeters, maxResults, filterByModes,
                filterByPlaceTypes, filterByStops, filterByRoutes, filterByBikeRentalStations,
                filterByBikeParks, filterByCarParks, routingService
        );
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
