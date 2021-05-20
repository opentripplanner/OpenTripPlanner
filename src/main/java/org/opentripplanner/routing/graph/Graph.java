package org.opentripplanner.routing.graph;

import static org.opentripplanner.model.projectinfo.OtpProjectInfo.projectInfo;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import gnu.trove.list.TDoubleList;
import gnu.trove.list.linked.TDoubleLinkedList;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.common.TurnRestriction;
import org.opentripplanner.common.geometry.CompactElevationProfile;
import org.opentripplanner.common.geometry.GraphUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.graph_builder.issues.NoFutureDates;
import org.opentripplanner.graph_builder.linking.VertexLinker;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource.DrivingDirection;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.FlexLocationGroup;
import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.model.GraphBundle;
import org.opentripplanner.model.GroupOfStations;
import org.opentripplanner.model.MultiModalStation;
import org.opentripplanner.model.Notice;
import org.opentripplanner.model.Operator;
import org.opentripplanner.model.SimpleTransfer;
import org.opentripplanner.model.Station;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.TimetableSnapshotProvider;
import org.opentripplanner.model.TransitEntity;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.calendar.impl.CalendarServiceImpl;
import org.opentripplanner.model.projectinfo.OtpProjectInfo;
import org.opentripplanner.model.transfer.TransferService;
import org.opentripplanner.routing.algorithm.raptor.transit.TransitLayer;
import org.opentripplanner.routing.algorithm.raptor.transit.mappers.TransitLayerUpdater;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.core.intersection_model.IntersectionTraversalCostModel;
import org.opentripplanner.routing.core.intersection_model.SimpleIntersectionTraversalCostModel;
import org.opentripplanner.routing.edgetype.EdgeWithCleanup;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.impl.DelegatingTransitAlertServiceImpl;
import org.opentripplanner.routing.impl.StreetVertexIndex;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.routing.services.notes.StreetNotesService;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.util.ConcurrentPublished;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.updater.GraphUpdaterConfigurator;
import org.opentripplanner.updater.GraphUpdaterManager;
import org.opentripplanner.util.WorldEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A graph is really just one or more indexes into a set of vertexes. It used to keep edgelists for each vertex, but those are in the vertex now.
 */
public class Graph implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(Graph.class);

    private static final long serialVersionUID = 1L;

    public static final DrivingDirection DEFAULT_DRIVING_DIRECTION = DrivingDirection.RIGHT_HAND_TRAFFIC;

    public static final IntersectionTraversalCostModel DEFAULT_INTERSECTION_TRAVERSAL_COST_MODEL
        = new SimpleIntersectionTraversalCostModel(DEFAULT_DRIVING_DIRECTION);

    private final OtpProjectInfo projectInfo = projectInfo();

    // TODO Remove this field, use Router.routerId ?
    public String routerId;

    private final Map<Edge, List<TurnRestriction>> turnRestrictions = Maps.newHashMap();

    public final StreetNotesService streetNotesService = new StreetNotesService();

    /**
     * Allows a notice element to be attached to an object in the OTP model by its id and then
     * retrieved by the API when navigating from that object. The map key is entity id:
     * {@link TransitEntity#getId()}. The notice is part of the static transit data.
     */
    private final Multimap<TransitEntity, Notice> noticesByElement = HashMultimap.create();

    // transit feed validity information in seconds since epoch
    private long transitServiceStarts = Long.MAX_VALUE;

    private long transitServiceEnds = 0;

    private final Map<Class<?>, Serializable> services = new HashMap<>();

    private final TransferService transferService = new TransferService();

    private GraphBundle bundle;

    /* Ideally we could just get rid of vertex labels, but they're used in tests and graph building. */
    private final Map<String, Vertex> vertices = new ConcurrentHashMap<>();

    private transient CalendarService calendarService;

    private transient StreetVertexIndex streetIndex;

    public transient GraphIndex index;

    public final transient Deduplicator deduplicator = new Deduplicator();

    /**
     * Map from GTFS ServiceIds to integers close to 0. Allows using BitSets instead of {@code Set<Object>}.
     * An empty Map is created before the Graph is built to allow registering IDs from multiple feeds.   
     */
    private final Map<FeedScopedId, Integer> serviceCodes = Maps.newHashMap();

    private transient TimetableSnapshotProvider timetableSnapshotProvider = null;

    private final Collection<Agency> agencies = new ArrayList<>();

    private final Collection<Operator> operators = new ArrayList<>();

    private final Collection<String> feedIds = new HashSet<>();

    private final Map<String, FeedInfo> feedInfoForId = new HashMap<>();

    private transient TimeZone timeZone = null;

    //Envelope of all OSM and transit vertices. Calculated during build time
    private WorldEnvelope envelope = null;

    //ConvexHull of all the graph vertices. Generated at Graph build time.
    private Geometry convexHull = null;

    /** The density center of the graph for determining the initial geographic extent in the client. */
    private Coordinate center = null;

    /* The preferences that were used for graph building. */
    public Preferences preferences = null;

    /** List of transit modes that are availible in GTFS data used in this graph**/
    private final HashSet<TransitMode> transitModes = new HashSet<>();

    public boolean hasBikeSharing = false;

    public boolean hasParkRide = false;

    public boolean hasBikeRide = false;

    /**
     * Manages all updaters of this graph. Is created by the GraphUpdaterConfigurator when there are
     * graph updaters defined in the configuration.
     *
     * @see GraphUpdaterConfigurator
     */
    public transient GraphUpdaterManager updaterManager = null;

    public final Date buildTime = new Date();

    /** True if OSM data was loaded into this Graph. */
    public boolean hasStreets = false;

    /** True if GTFS data was loaded into this Graph. */
    public boolean hasTransit = false;

    /** True if direct single-edge transfers were generated between transit stops in this Graph. */
    public boolean hasDirectTransfers = false;

    /**
     * True if frequency-based services exist in this Graph (GTFS frequencies with exact_times = 0).
     */
    public boolean hasFrequencyService = false;

    /**
     * True if schedule-based services exist in this Graph (including GTFS frequencies with
     * exact_times = 1).
     */
    public boolean hasScheduledService = false;

    /**
     * The difference in meters between the WGS84 ellipsoid height and geoid height
     * at the graph's center
     */
    public Double ellipsoidToGeoidDifference = 0.0;

    /** Parent stops **/
    public Map<FeedScopedId, Station> stationById = new HashMap<>();

    /**
     * Optional level above parent stops (only supported in NeTEx)
     */
    public Map<FeedScopedId, MultiModalStation> multiModalStationById = new HashMap<>();

    /**
     * Optional grouping that can contain both stations and multimodal stations (only supported in
     * NeTEx)
     */
    public Map<FeedScopedId, GroupOfStations> groupOfStationsById = new HashMap<>();

    /**
     * TripPatterns used to be reached through hop edges, but we're not creating on-board transit
     * vertices/edges anymore.
     */
    public Map<FeedScopedId, TripPattern> tripPatternForId = Maps.newHashMap();

    /** Interlining relationships between trips. */
    public final BiMap<Trip,Trip> interlinedTrips = HashBiMap.create();

    /** Pre-generated transfers between all stops. */
    public final Multimap<StopLocation, SimpleTransfer> transfersByStop = HashMultimap.create();

    public Map<FeedScopedId, FlexStopLocation> locationsById = new HashMap<>();

    public Map<FeedScopedId, FlexLocationGroup> locationGroupsById = new HashMap<>();

    public Map<FeedScopedId, FlexTrip> flexTripsById = new HashMap<>();

    /** The distance between elevation samples used in CompactElevationProfile. */
    private double distanceBetweenElevationSamples;

    /** Data model for Raptor routing, with realtime updates applied (if any). */
    private transient TransitLayer transitLayer;

    /** Data model for Raptor routing, with realtime updates applied (if any). */
    private final transient ConcurrentPublished<TransitLayer> realtimeTransitLayer =
        new ConcurrentPublished<>();

    public transient TransitLayerUpdater transitLayerUpdater;

    private transient TransitAlertService transitAlertService;

    private DrivingDirection drivingDirection = DEFAULT_DRIVING_DIRECTION;

    private IntersectionTraversalCostModel intersectionTraversalCostModel =
        DEFAULT_INTERSECTION_TRAVERSAL_COST_MODEL;

    /**
     * Hack. I've tried three different ways of generating unique labels.
     * Previously we were just tolerating edge label collisions.
     * For some reason we're repeatedly generating splits on the same edge objects, despite a
     * comment that said it was guaranteed there would only ever be one split per edge. This is
     * going to fail as soon as we load a base OSM graph and build transit on top of it.
     */
    public long nextSplitNumber = 0;

    public Graph(Graph basedOn) {
        this();
        this.bundle = basedOn.getBundle();
        this.drivingDirection = basedOn.drivingDirection;
    }

    // Constructor for deserialization.
    public Graph() { }

    public TimetableSnapshot getTimetableSnapshot() {
        return timetableSnapshotProvider == null ? null : timetableSnapshotProvider.getTimetableSnapshot();
    }

    /**
     * TODO OTP2 - This should be replaced by proper dependency injection
     */
    @SuppressWarnings("unchecked")
    public <T extends TimetableSnapshotProvider> T getOrSetupTimetableSnapshotProvider(Function<Graph, T> creator) {
        if (timetableSnapshotProvider == null) {
            timetableSnapshotProvider = creator.apply(this);
        }
        try {
            return (T) timetableSnapshotProvider;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(
                    "We support only one timetableSnapshotSource, there are two implementation; one for GTFS and one " +
                    "for Netex/Siri. They need to be refactored to work together. This cast will fail if updaters " +
                    "try setup both.",
                    e
            );
        }
    }

    /**
     * Add the given vertex to the graph. Ideally, only vertices should add themselves to the graph,
     * when they are constructed or deserialized.
     *
     * TODO OTP2 - This strategy is error prune, problematic when testing and causes a cyclic
     *           - dependency Graph -> Vertex -> Graph. A better approach is to lett the bigger
     *           - whole (Graph) create and attach its smaller parts (Vertex). A way is to create
     *           - a VertexCollection class, let the graph hold an instance of this collection,
     *           - and create factory methods for each type of Vertex in the VertexCollection.
     */
    public void addVertex(Vertex v) {
        Vertex old = vertices.put(v.getLabel(), v);
        if (old != null) {
            if (old == v)
                LOG.error("repeatedly added the same vertex: {}", v);
            else
                LOG.error("duplicate vertex label in graph (added vertex to graph anyway): {}", v);
        }
    }

    /**
     * Removes an edge from the graph. This method is not thread-safe.
     * @param e The edge to be removed
     */
    public void removeEdge(Edge e) {
        if (e != null) {
            turnRestrictions.remove(e);
            streetNotesService.removeStaticNotes(e);

            if (e instanceof EdgeWithCleanup) ((EdgeWithCleanup) e).detach();

            if (e.fromv != null) {
                e.fromv.removeOutgoing(e);

                for (Edge otherEdge : e.fromv.getIncoming()) {
                    for (TurnRestriction turnRestriction : getTurnRestrictions(otherEdge)) {
                        if (turnRestriction.to == e) {
                            removeTurnRestriction(otherEdge, turnRestriction);
                        }
                    }
                }

                e.fromv = null;
            }

            if (e.tov != null) {
                e.tov.removeIncoming(e);
                e.tov = null;
            }
        }
    }

    /* Fetching vertices by label is convenient in tests and such, but avoid using in general. */
    @VisibleForTesting
    public Vertex getVertex(String label) {
        return vertices.get(label);
    }

    /**
     * Get all the vertices in the graph.
     */
    public Collection<Vertex> getVertices() {
        return this.vertices.values();
    }

    public <T extends Vertex> List<T> getVerticesOfType(Class<T> cls) {
        return this
            .getVertices()
            .stream()
            .filter(cls::isInstance)
            .map(cls::cast)
            .collect(Collectors.toList());
    }

    /**
     * Return all the edges in the graph. Derived from vertices on demand.
     */
    public Collection<Edge> getEdges() {
        Set<Edge> edges = new HashSet<>();
        for (Vertex v : this.getVertices()) {
            edges.addAll(v.getOutgoing());
        }
        return edges;
    }

    public <T extends Edge> List<T> getEdgesOfType(Class<T> cls) {
        return this
            .getEdges()
            .stream()
            .filter(cls::isInstance)
            .map(cls::cast)
            .collect(Collectors.toList());
    }

    /**
     * Add a {@link TurnRestriction} to the {@link TurnRestriction} {@link List} belonging to an
     * {@link Edge}. This method is not thread-safe.
     */
    public void addTurnRestriction(Edge edge, TurnRestriction turnRestriction) {
        if (edge == null || turnRestriction == null) return;
        List<TurnRestriction> turnRestrictions = this.turnRestrictions.get(edge);
        if (turnRestrictions == null) {
            turnRestrictions = Lists.newArrayList();
            this.turnRestrictions.put(edge, turnRestrictions);
        }
        turnRestrictions.add(turnRestriction);
    }

    /**
     * Remove a {@link TurnRestriction} from the {@link TurnRestriction} {@link List} belonging to
     * an {@link Edge}. This method is not thread-safe.
     */
    public void removeTurnRestriction(Edge edge, TurnRestriction turnRestriction) {
        if (edge == null || turnRestriction == null) return;
        List<TurnRestriction> turnRestrictions = this.turnRestrictions.get(edge);
        if (turnRestrictions != null && turnRestrictions.contains(turnRestriction)) {
            if (turnRestrictions.size() < 2) {
                this.turnRestrictions.remove(edge);
            } else {
                turnRestrictions.remove(turnRestriction);
            }
        }
    }

    /**
     * Get the {@link TurnRestriction} {@link List} that belongs to an {@link Edge} and return an
     * immutable copy. This method is thread-safe when used by itself, but not if addTurnRestriction
     * or removeTurnRestriction is called concurrently.
     * @return The {@link TurnRestriction} {@link List} that belongs to the {@link Edge}
     */
    public List<TurnRestriction> getTurnRestrictions(Edge edge) {
        if (edge != null) {
            List<TurnRestriction> turnRestrictions = this.turnRestrictions.get(edge);
            if (turnRestrictions != null) {
                return ImmutableList.copyOf(turnRestrictions);
            }
        }
        return Collections.emptyList();
    }

    /**
     * Return only the StreetEdges in the graph.
     */
    public Collection<StreetEdge> getStreetEdges() {
        return getEdgesOfType(StreetEdge.class);
    }

    public TransitLayer getTransitLayer() {
        return transitLayer;
    }

    public void setTransitLayer(
        TransitLayer transitLayer
    ) {
        this.transitLayer = transitLayer;
    }

    public TransitLayer getRealtimeTransitLayer() {
        return realtimeTransitLayer.get();
    }

    public boolean hasRealtimeTransitLayer() {
        return realtimeTransitLayer != null;
    }

    public void setRealtimeTransitLayer(
        TransitLayer realtimeTransitLayer
    ) {
        this.realtimeTransitLayer.publish(realtimeTransitLayer);
    }

    public boolean containsVertex(Vertex v) {
        return (v != null) && vertices.get(v.getLabel()) == v;
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> T putService(Class<T> serviceType, T service) {
        return (T) services.put(serviceType, service);
    }

    public boolean hasService(Class<? extends Serializable> serviceType) {
        return services.containsKey(serviceType);
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getService(Class<T> serviceType) {
        return (T) services.get(serviceType);
    }

    public <T extends Serializable> T getService(Class<T> serviceType, boolean autoCreate) {
        T t = (T)services.get(serviceType);
        if (t == null && autoCreate) {
            try {
                t = serviceType.getDeclaredConstructor().newInstance();
            }
            catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | InstantiationException e) {
                throw new RuntimeException(e);
            }
            services.put(serviceType, t);
        }
        return t;
    }

    public void remove(Vertex vertex) {
        vertices.remove(vertex.getLabel());
    }

    public void removeIfUnconnected(Vertex v) {
        if (v.getDegreeIn() == 0 && v.getDegreeOut() == 0) {
            remove(v);
        }
    }

    public Envelope getExtent() {
        Envelope env = new Envelope();
        for (Vertex v : getVertices()) {
            env.expandToInclude(v.getCoordinate());
        }
        return env;
    }

    public TransferService getTransferService() {
        return transferService;
    }

    // Infer the time period covered by the transit feed
    public void updateTransitFeedValidity(
            CalendarServiceData data,
            DataImportIssueStore issueStore
    ) {
        long now = new Date().getTime() / 1000;
        final long SEC_IN_DAY = 24 * 60 * 60;
        HashSet<String> agenciesWithFutureDates = new HashSet<>();
        HashSet<String> agencies = new HashSet<>();
        for (FeedScopedId sid : data.getServiceIds()) {
            agencies.add(sid.getFeedId());
            for (ServiceDate sd : data.getServiceDatesForServiceId(sid)) {
                // Adjust for timezone, assuming there is only one per graph.
                long t = sd.getAsDate(getTimeZone()).getTime() / 1000;
                if (t > now) {
                    agenciesWithFutureDates.add(sid.getFeedId());
                }
                // assume feed is unreliable after midnight on last service day
                long u = t + SEC_IN_DAY;
                if (t < this.transitServiceStarts)
                    this.transitServiceStarts = t;
                if (u > this.transitServiceEnds)
                    this.transitServiceEnds = u;
            }
        }
        for (String agency : agencies) {
            if (!agenciesWithFutureDates.contains(agency)) {
                issueStore.add(new NoFutureDates(agency));
            }
        }
    }

    // Check to see if we have transit information for a given date
    public boolean transitFeedCovers(long t) {
        return t >= this.transitServiceStarts && t < this.transitServiceEnds;
    }

    public GraphBundle getBundle() {
        return bundle;
    }

    public void setBundle(GraphBundle bundle) {
        this.bundle = bundle;
    }

    public int countVertices() {
        return vertices.size();
    }

    /**
     * Find the total number of edges in this Graph. There are assumed to be no Edges in an incoming edge list that are not in an outgoing edge list.
     * 
     * @return number of outgoing edges in the graph
     */
    public int countEdges() {
        int ne = 0;
        for (Vertex v : getVertices()) {
            ne += v.getDegreeOut();
        }
        return ne;
    }

    private void readObject(ObjectInputStream inputStream) throws ClassNotFoundException, IOException {
        inputStream.defaultReadObject();
    }

    /**
     * Adds mode of transport to transit modes in graph
     */
    public void addTransitMode(TransitMode mode) {
        transitModes.add(mode);
    }

    public HashSet<TransitMode> getTransitModes() {
        return transitModes;
    }

    /**
     * Perform indexing on vertices, edges, and timetables, and create transient data structures.
     * This used to be done in readObject methods upon deserialization, but stand-alone mode now
     * allows passing graphs from graphbuilder to server in memory, without a round trip through
     * serialization. 
     */
    public void index () {
        LOG.info("Index graph...");
        streetIndex = new StreetVertexIndex(this);
        LOG.debug("Rebuilding edge and vertex indices.");
        for (TripPattern tp : tripPatternForId.values()) {
            // Skip frequency-based patterns which have no timetable (null)
            if (tp != null) tp.scheduledTimetable.finish();
        }
        // TODO: Move this ^ stuff into the graph index
        this.index = new GraphIndex(this);
        LOG.info("Index graph complete.");
    }
    
    public CalendarService getCalendarService() {
        if (calendarService == null) {
            CalendarServiceData data = this.getService(CalendarServiceData.class);
            if (data != null) {
                this.calendarService = new CalendarServiceImpl(data);
            }
        }
        return this.calendarService;
    }

    public StreetVertexIndex getStreetIndex() {
        if (this.streetIndex == null) {
            streetIndex = new StreetVertexIndex(this);
        }
        return this.streetIndex;
    }

    public VertexLinker getLinker() {
        return getStreetIndex().getVertexLinker();
    }

    /**
     * Get or create a serviceId for a given date. This method is used when a new trip is
     * added from a realtime data update.
     *
     * TODO OTP2 - This is NOT THREAD-SAFE and is used in the real-time updaters, we need to fix
     *           - this when doing the issue #3030.
     *
     * @param serviceDate service date for the added service id
     */
    public FeedScopedId getOrCreateServiceIdForDate(ServiceDate serviceDate) {
        // We make an explicit cast here to avoid adding the 'getOrCreateServiceIdForDate(..)'
        // method to the {@link CalendarService} interface. We do not want to expose it because it
        // is not thread-safe - and we want to limit the usage. See JavaDoc above as well.
        FeedScopedId serviceId = ((CalendarServiceImpl)getCalendarService()).getOrCreateServiceIdForDate(serviceDate);

        if (!serviceCodes.containsKey(serviceId)) {
            serviceCodes.put(serviceId, serviceCodes.size());
        }
        return serviceId;
    }

    public int removeEdgelessVertices() {
        int removed = 0;
        List<Vertex> toRemove = new LinkedList<>();
        for (Vertex v : this.getVertices())
            if (v.getDegreeOut() + v.getDegreeIn() == 0)
                toRemove.add(v);
        // avoid concurrent vertex map modification
        for (Vertex v : toRemove) {
            this.remove(v);
            removed += 1;
            LOG.trace("removed edgeless vertex {}", v);
        }
        return removed;
    }

    public Collection<String> getFeedIds() {
        return feedIds;
    }

    public Collection<Agency> getAgencies() {
        return agencies;
    }

    public FeedInfo getFeedInfo(String feedId) {
        return feedInfoForId.get(feedId);
    }

    public void addAgency(String feedId, Agency agency) {
        agencies.add(agency);
        this.feedIds.add(feedId);
    }

    public void addFeedInfo(FeedInfo info) {
        this.feedInfoForId.put(info.getId(), info);
    }

    /**
     * Returns the time zone for the first agency in this graph. This is used to interpret times in
     * API requests. The JVM default time zone cannot be used because we support multiple graphs on
     * one server via the routerId. Ideally we would want to interpret times in the time zone of
     * the geographic location where the origin/destination vertex or board/alight event is
     * located. This may become necessary when we start making graphs with long distance train,
     * boat, or air services.
     */
    public TimeZone getTimeZone() {
        if (timeZone == null) {
            if (agencies.size() == 0) {
                timeZone = TimeZone.getTimeZone("GMT");
                LOG.warn("graph contains no agencies (yet); API request times will be interpreted as GMT.");
            } else {
                CalendarService cs = this.getCalendarService();
                for (Agency agency : agencies) {
                    TimeZone tz = cs.getTimeZoneForAgencyId(agency.getId());
                    if (timeZone == null) {
                        LOG.debug("graph time zone set to {}", tz);
                        timeZone = tz;
                    } else if (!timeZone.equals(tz)) {
                        LOG.error("agency time zone differs from graph time zone: {}", tz);
                    }
                }
            }
        }
        return timeZone;
    }

    public Collection<Operator> getOperators() {
        return operators;
    }

    /**
     * The timezone is cached by the graph. If you've done something to the graph that has the
     * potential to change the time zone, you should call this to ensure it is reset. 
     */
    public void clearTimeZone () {
        this.timeZone = null;
    }

    /**
     * Calculates envelope out of all OSM coordinates
     *
     * Transit stops are added to the envelope as they are added to the graph
     */
    public void calculateEnvelope() {
        this.envelope = new WorldEnvelope();

        for (Vertex v : this.getVertices()) {
            Coordinate c = v.getCoordinate();
            this.envelope.expandToInclude(c);
        }
    }

    /**
     * Calculates convexHull of all the vertices during build time
     */
    public void calculateConvexHull() {
        convexHull = GraphUtils.makeConvexHull(this);
    }

    /**
     * @return calculated convexHull;
     */
    public Geometry getConvexHull() {
        return convexHull;

    }

    /**
     * Expands envelope to include given point
     *
     * If envelope is empty it creates it (This can happen with a graph without OSM data)
     * Used when adding stops to OSM envelope
     *
     * @param  x  the value to lower the minimum x to or to raise the maximum x to
     * @param  y  the value to lower the minimum y to or to raise the maximum y to
     */
    public void expandToInclude(double x, double y) {
        //Envelope can be empty if graph building is run without OSM data
        if (this.envelope == null) {
            calculateEnvelope();
        }
        this.envelope.expandToInclude(x, y);
    }

    public WorldEnvelope getEnvelope() {
        return this.envelope;
    }

    /**
     * Calculates Transit center from median of coordinates of all transitStops if graph
     * has transit. If it doesn't it isn't calculated. (mean walue of min, max latitude and longitudes are used)
     *
     * Transit center is saved in center variable
     *
     * This speeds up calculation, but problem is that median needs to have all of latitudes/longitudes
     * in memory, this can become problematic in large installations. It works without a problem on New York State.
     */
    public void calculateTransitCenter() {
        if (hasTransit) {

            TDoubleList latitudes = new TDoubleLinkedList();
            TDoubleList longitudes = new TDoubleLinkedList();
            Median median = new Median();

            getVerticesOfType(TransitStopVertex.class)
                .stream()
                .forEach(v -> {
                    latitudes.add(v.getLat());
                    longitudes.add(v.getLon());
                });

            median.setData(latitudes.toArray());
            double medianLatitude = median.evaluate();
            median = new Median();
            median.setData(longitudes.toArray());
            double medianLongitude = median.evaluate();

            this.center = new Coordinate(medianLongitude, medianLatitude);
        }
    }

    public Optional<Coordinate> getCenter() {
        return Optional.ofNullable(center);
    }

    public long getTransitServiceStarts() {
        return transitServiceStarts;
    }

    public long getTransitServiceEnds() {
        return transitServiceEnds;
    }

    public Multimap<TransitEntity, Notice> getNoticesByElement() {
        return noticesByElement;
    }

    public void addNoticeAssignments(Multimap<TransitEntity, Notice> noticesByElement) {
        this.noticesByElement.putAll(noticesByElement);
    }

    public double getDistanceBetweenElevationSamples() {
        return distanceBetweenElevationSamples;
    }

    public void setDistanceBetweenElevationSamples(double distanceBetweenElevationSamples) {
        this.distanceBetweenElevationSamples = distanceBetweenElevationSamples;
        CompactElevationProfile.setDistanceBetweenSamplesM(distanceBetweenElevationSamples);
    }

    public TransitAlertService getTransitAlertService() {
        if (transitAlertService == null) {
            transitAlertService = new DelegatingTransitAlertServiceImpl(this);
        }
        return transitAlertService;
    }

    private Collection<Stop> getStopsForId(FeedScopedId id) {

        // GroupOfStations
        GroupOfStations groupOfStations = groupOfStationsById.get(id);
        if (groupOfStations != null) {
            return groupOfStations.getChildStops();
        }

        // Multimodal station
        MultiModalStation multiModalStation = multiModalStationById.get(id);
        if (multiModalStation != null) {
            return multiModalStation.getChildStops();
        }

        // Station
        Station station = stationById.get(id);
        if (station != null) {
            return station.getChildStops();
        }
        // Single stop
        Stop stop = index.getStopForId(id);
        if (stop != null) {
            return Collections.singleton(stop);
        }

        return null;
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

        return stops.stream().map(index.getStopVertexForStop()::get).collect(Collectors.toSet());
    }

    /** An OBA Service Date is a local date without timezone, only year month and day. */
    public BitSet getServicesRunningForDate(ServiceDate date) {
        BitSet services = new BitSet(calendarService.getServiceIds().size());
        for (FeedScopedId serviceId : calendarService.getServiceIdsOnDate(date)) {
            int n = serviceCodes.get(serviceId);
            if (n < 0) { continue; }
            services.set(n);
        }
        return services;
    }

    public BikeRentalStationService getBikerentalStationService() {
        return getService(BikeRentalStationService.class);
    }

    public Collection<Notice> getNoticesByEntity(TransitEntity entity) {
        Collection<Notice> res = getNoticesByElement().get(entity);
        return res == null ? Collections.emptyList() : res;
    }

    public TripPattern getTripPatternForId(FeedScopedId id) {
        return tripPatternForId.get(id);
    }

    public Collection<TripPattern> getTripPatterns() {
        return tripPatternForId.values();
    }

    public Collection<Notice> getNotices() {
        return getNoticesByElement().values();
    }

    /** Get all stops within a given bounding box. */
    public Collection<Stop> getStopsByBoundingBox(double minLat, double minLon, double maxLat, double maxLon) {
        Envelope envelope = new Envelope(
                new Coordinate(minLon, minLat),
                new Coordinate(maxLon, maxLat)
        );
        return streetIndex
            .getTransitStopForEnvelope(envelope)
            .stream()
            .map(TransitStopVertex::getStop)
            .collect(Collectors.toList());
    }

    /** Get all stops within a given radius. Unit: meters. */
    public List<T2<Stop, Double>> getStopsInRadius(WgsCoordinate center, double radius) {
        Coordinate coord = new Coordinate(center.longitude(), center.latitude());
        return streetIndex.getNearbyTransitStops(coord, radius).stream()
                .map(v -> new T2<>(v.getStop(), SphericalDistanceLibrary.fastDistance(v.getCoordinate(), coord)))
                .filter(t -> t.second < radius)
                .collect(Collectors.toList());
    }

    public Station getStationById(FeedScopedId id) {
        return stationById.get(id);
    }

    public MultiModalStation getMultiModalStation(FeedScopedId id) {
        return multiModalStationById.get(id);
    }

    public Collection<Station> getStations() {
        return stationById.values();
    }

    public Map<FeedScopedId, Integer> getServiceCodes() {
        return serviceCodes;
    }

    public Collection<SimpleTransfer> getTransfersByStop(StopLocation stop) {
        return transfersByStop.get(stop);
    }

    public DrivingDirection getDrivingDirection() {
        return drivingDirection;
    }

    public void setDrivingDirection(DrivingDirection drivingDirection) {
        this.drivingDirection = drivingDirection;
    }

    public IntersectionTraversalCostModel getIntersectionTraversalModel() {
        return intersectionTraversalCostModel;
    }

    public void setIntersectionTraversalCostModel(
        IntersectionTraversalCostModel intersectionTraversalCostModel
    ) {
        this.intersectionTraversalCostModel = intersectionTraversalCostModel;
    }

    /**
     *  Flex locations are generated by GTFS graph builder, but consumed only after the street graph
     *  is built
     */
    public FlexStopLocation getLocationById(FeedScopedId id) {
        return locationsById.get(id);
    }

    /**
     * Gets all the flex stop locations, including the elements of FlexLocationGroups.
     */
    public Set<StopLocation> getAllFlexStopsFlat() {
        Set<StopLocation> stopLocations = flexTripsById
            .values()
            .stream()
            .flatMap(t -> t.getStops().stream())
            .collect(Collectors.toSet());

        stopLocations.addAll(
            stopLocations
                .stream()
                .filter(s -> s instanceof FlexLocationGroup)
                .flatMap(g -> ((FlexLocationGroup) g)
                    .getLocations()
                    .stream()
                    .filter(e -> e instanceof Stop))
                .collect(Collectors.toList())
        );

        return stopLocations;
    }
}
