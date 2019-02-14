package org.opentripplanner.routing.core;

import com.google.common.collect.Iterables;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.Agency;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.CalendarService;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.algorithm.strategies.EuclideanRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TemporaryPartialStreetEdge;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.error.TransitTimesException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.location.TemporaryStreetLocation;
import org.opentripplanner.routing.services.OnBoardDepartService;
import org.opentripplanner.routing.vertextype.TemporaryVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.opentripplanner.util.NonLocalizedString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;

/**
 * A RoutingContext holds information needed to carry out a search for a particular TraverseOptions, on a specific graph.
 * Includes things like (temporary) endpoint vertices, transfer tables, service day caches, etc.
 *
 * In addition, while the RoutingRequest should only carry parameters _in_ to the routing operation, the routing context
 * should be used to carry information back out, such as debug figures or flags that certain thresholds have been exceeded.
 */
public class RoutingContext implements Cloneable {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingContext.class);

    /* FINAL FIELDS */

    public RoutingRequest opt; // not final so we can reverse-clone

    public final Graph graph;

    public final Vertex fromVertex;

    public final Vertex toVertex;

    // origin means "where the initial state will be located" not "the beginning of the trip from the user's perspective"
    public final Vertex origin;

    // target means "where this search will terminate" not "the end of the trip from the user's perspective"
    public final Vertex target;
    
    // The back edge associated with the origin - i.e. continuing a previous search.
    // NOTE: not final so that it can be modified post-construction for testing.
    // TODO(flamholz): figure out a better way.
    public Edge originBackEdge;

    // public final Calendar calendar;
    public final CalendarService calendarService;

    public final Map<FeedScopedId, Set<ServiceDate>> serviceDatesByServiceId = new HashMap<FeedScopedId, Set<ServiceDate>>();

    public RemainingWeightHeuristic remainingWeightHeuristic;

    public final TransferTable transferTable;

    /** The timetableSnapshot is a {@link TimetableSnapshot} for looking up real-time updates. */
    public final TimetableSnapshot timetableSnapshot;

    /**
     * Cache lists of which transit services run on which midnight-to-midnight periods. This ties a TraverseOptions to a particular start time for the
     * duration of a search so the same options cannot be used for multiple searches concurrently. To do so this cache would need to be moved into
     * StateData, with all that entails.
     */
    public ArrayList<ServiceDay> serviceDays;

    /**
     * The search will be aborted if it is still running after this time (in milliseconds since the epoch). A negative or zero value implies no limit.
     * This provides an absolute timeout, whereas the maxComputationTime is relative to the beginning of an individual search. While the two might
     * seem equivalent, we trigger search retries in various places where it is difficult to update relative timeout value. The earlier of the two
     * timeouts is applied.
     */
    public long searchAbortTime = 0;

    public Vertex startingStop;

    /** An object that accumulates profiling and debugging info for inclusion in the response. */
    public DebugOutput debugOutput = new DebugOutput();

    /** Indicates that the search timed out or was otherwise aborted. */
    public boolean aborted;

    /** Indicates that a maximum slope constraint was specified but was removed during routing to produce a result. */
    public boolean slopeRestrictionRemoved = false;

    /**
     * Temporary vertices created during the request. This is needed for GTFS-Flex support. The other temporary vertices which are created have
     * known locations (the endpoints of the search), but GTFS-Flex routing may require temporary vertices to be created at other places in the
     * graph. Temporary vertices are request-specific need to be disposed of at the end-of-life of a request.
     */
    public Collection<Vertex> temporaryVertices = new ArrayList<>();

    /* CONSTRUCTORS */

    /**
     * Constructor that automatically computes origin/target from RoutingRequest.
     */
    public RoutingContext(RoutingRequest routingRequest, Graph graph) {
        this(routingRequest, graph, null, null, true, null);
    }

    /**
     * Constructor that automatically computes origin/target from RoutingRequest, and sets the
     * context's temporary vertices. This is needed for intermediate places, as a consequence of
     * the check that temporary vertices are request-specific.
     */
    public RoutingContext(RoutingRequest routingRequest, Graph graph, Collection<Vertex> temporaryVertices) {
        this(routingRequest, graph, null, null, true, temporaryVertices);
    }

    /**
     * Constructor that takes to/from vertices as input.
     */
    public RoutingContext(RoutingRequest routingRequest, Graph graph, Vertex from, Vertex to) {
        this(routingRequest, graph, from, to, false, null);
    }

    /**
     * Returns the StreetEdges that overlap between two vertices edge sets.
     */
    private Set<StreetEdge> overlappingStreetEdges(Vertex u, Vertex v) {
        Set<Integer> vIds = new HashSet<Integer>();
        Set<Integer> uIds = new HashSet<Integer>();
        for (Edge e : Iterables.concat(v.getIncoming(), v.getOutgoing())) {
            vIds.add(e.getId());
        }
        for (Edge e : Iterables.concat(u.getIncoming(), u.getOutgoing())) {
            uIds.add(e.getId());
        }

        // Intesection of edge IDs between u and v.
        uIds.retainAll(vIds);
        Set<Integer> overlappingIds = uIds;

        // Fetch the edges by ID - important so we aren't stuck with temporary edges.
        Set<StreetEdge> overlap = new HashSet<>();
        for (Integer id : overlappingIds) {
            Edge e = graph.getEdgeById(id);
            if (e == null || !(e instanceof StreetEdge)) {
                continue;
            }

            overlap.add((StreetEdge) e);
        }
        return overlap;
    }

    /**
     * Creates a PartialStreetEdge along the input StreetEdge iff its direction makes this possible.
     */
    private void makePartialEdgeAlong(StreetEdge streetEdge, TemporaryStreetLocation from,
                                      TemporaryStreetLocation to) {
        LineString parent = streetEdge.getGeometry();
        LineString head = GeometryUtils.getInteriorSegment(parent,
                streetEdge.getFromVertex().getCoordinate(), from.getCoordinate());
        LineString tail = GeometryUtils.getInteriorSegment(parent,
                to.getCoordinate(), streetEdge.getToVertex().getCoordinate());

        if (parent.getLength() > head.getLength() + tail.getLength()) {
            LineString partial = GeometryUtils.getInteriorSegment(parent,
                    from.getCoordinate(), to.getCoordinate());

            double lengthRatio = partial.getLength() / parent.getLength();
            double length = streetEdge.getDistance() * lengthRatio;

            //TODO: localize this
            String name = from.getLabel() + " to " + to.getLabel();
            new TemporaryPartialStreetEdge(streetEdge, from, to, partial, new NonLocalizedString(name), length);
        }
    }

    /**
     * Flexible constructor which may compute to/from vertices.
     * 
     * TODO(flamholz): delete this flexible constructor and move the logic to constructors above appropriately.
     * 
     * @param findPlaces if true, compute origin and target from RoutingRequest using spatial indices.
     * @param temporaryVerticesParam if not null, use this collection to keep track of temporary vertices.
     */
    private RoutingContext(RoutingRequest routingRequest, Graph graph, Vertex from, Vertex to,
            boolean findPlaces, Collection<Vertex> temporaryVerticesParam) {
        if (graph == null) {
            throw new GraphNotFoundException();
        }
        this.opt = routingRequest;
        this.graph = graph;
        this.debugOutput.startedCalculating();

        // The following block contains potentially resource-intensive things that are only relevant for transit.
        // In normal searches the impact is low, because the routing context is only constructed once at the beginning
        // of the search, but when computing transfers or doing large batch jobs, repeatedly re-constructing useless
        // transit-specific information can have an impact.
        if (opt.modes.isTransit()) {
            // the graph's snapshot may be frequently updated.
            // Grab a reference to ensure a coherent view of the timetables throughout this search.
            if (routingRequest.ignoreRealtimeUpdates) {
                timetableSnapshot = null;
            } else {
                TimetableSnapshotSource timetableSnapshotSource = graph.timetableSnapshotSource;
                if (timetableSnapshotSource == null) {
                    timetableSnapshot = null;
                } else {
                    timetableSnapshot = timetableSnapshotSource.getTimetableSnapshot();
                }
            }
            calendarService = graph.getCalendarService();
            setServiceDays();
        } else {
            timetableSnapshot = null;
            calendarService = null;
        }

        Edge fromBackEdge = null;
        Edge toBackEdge = null;
        if (findPlaces) {
            if (opt.batch) {
                // batch mode: find an OSM vertex, don't split
                // We do this so that we are always linking to the same thing in analyst mode
                // even if the transit network has changed.
                // TODO offset time by distance to nearest OSM node?
                if (opt.arriveBy) {
                    // TODO what if there is no coordinate but instead a named place?
                    toVertex = graph.streetIndex.getSampleVertexAt(opt.to.getCoordinate(), true);
                    fromVertex = null;
                }
                else {
                    fromVertex = graph.streetIndex.getSampleVertexAt(opt.from.getCoordinate(), false);
                    toVertex = null;
                }
            }

            else {
                // normal mode, search for vertices based RoutingRequest and split streets
                toVertex = graph.streetIndex.getVertexForLocation(opt.to, opt, true);
                if (opt.to.hasEdgeId()) {
                    toBackEdge = graph.getEdgeById(opt.to.edgeId);
                }

                if (opt.startingTransitTripId != null && !opt.arriveBy) {
                    // Depart on-board mode: set the from vertex to "on-board" state
                    OnBoardDepartService onBoardDepartService = graph.getService(OnBoardDepartService.class);
                    if (onBoardDepartService == null)
                        throw new UnsupportedOperationException("Missing OnBoardDepartService");
                    fromVertex = onBoardDepartService.setupDepartOnBoard(this);
                } else {
                    fromVertex = graph.streetIndex.getVertexForLocation(opt.from, opt, false);
                    if (opt.from.hasEdgeId()) {
                        fromBackEdge = graph.getEdgeById(opt.from.edgeId);
                    }
                }
            }
        } else {
            // debug mode, force endpoint vertices to those specified rather than searching
            fromVertex = from;
            toVertex = to;
        }

        // If the from and to vertices are generated and lie on some of the same edges, we need to wire them
        // up along those edges so that we don't get odd circuitous routes for really short trips.
        // TODO(flamholz): seems like this might be the wrong place for this code? Can't find a better one.
        //
        if (fromVertex instanceof TemporaryStreetLocation &&
                toVertex instanceof TemporaryStreetLocation) {
            TemporaryStreetLocation fromStreetVertex = (TemporaryStreetLocation) fromVertex;
            TemporaryStreetLocation toStreetVertex = (TemporaryStreetLocation) toVertex;
            Set<StreetEdge> overlap = overlappingStreetEdges(fromStreetVertex,
                    toStreetVertex);

            for (StreetEdge pse : overlap) {
                makePartialEdgeAlong(pse, fromStreetVertex, toStreetVertex);
            }
        }

        // Add temporary subgraphs to the routing context. If `fromVertex` or `toVertex` are not
        // tempoorary vertices, their subgraphs are empty, so this has no effect.
        if (temporaryVerticesParam != null) {
            temporaryVertices = temporaryVerticesParam;
        }
        temporaryVertices.addAll(TemporaryVertex.findSubgraph(fromVertex));
        temporaryVertices.addAll(TemporaryVertex.findSubgraph(toVertex));

        if (opt.startingTransitStopId != null) {
            Stop stop = graph.index.stopForId.get(opt.startingTransitStopId);
            TransitStop tstop = graph.index.stopVertexForStop.get(stop);
            startingStop = tstop.departVertex;
        }
        origin = opt.arriveBy ? toVertex : fromVertex;
        originBackEdge = opt.arriveBy ? toBackEdge : fromBackEdge;
        target = opt.arriveBy ? fromVertex : toVertex;
        transferTable = graph.getTransferTable();
        if (opt.batch)
            remainingWeightHeuristic = new TrivialRemainingWeightHeuristic();
        else
            remainingWeightHeuristic = new EuclideanRemainingWeightHeuristic();

        if (this.origin != null) {
            LOG.debug("Origin vertex inbound edges {}", this.origin.getIncoming());
            LOG.debug("Origin vertex outbound edges {}", this.origin.getOutgoing());
        }
        // target is where search will terminate, can be origin or destination depending on arriveBy
        LOG.debug("Target vertex {}", this.target);
        if (this.target != null) {
            LOG.debug("Destination vertex inbound edges {}", this.target.getIncoming());
            LOG.debug("Destination vertex outbound edges {}", this.target.getOutgoing());
        }
    }

    /* INSTANCE METHODS */

    public void check() {
        ArrayList<String> notFound = new ArrayList<String>();

        // check origin present when not doing an arrive-by batch search
        if (!(opt.batch && opt.arriveBy))
            if (fromVertex == null)
                notFound.add("from");

        // check destination present when not doing a depart-after batch search
        if (!opt.batch || opt.arriveBy) {
            if (toVertex == null) {
                notFound.add("to");
            }
        }
        if (notFound.size() > 0) {
            throw new VertexNotFoundException(notFound);
        }
        if (opt.modes.isTransit() && !graph.transitFeedCovers(opt.dateTime)) {
            // user wants a path through the transit network,
            // but the date provided is outside those covered by the transit feed.
            throw new TransitTimesException();
        }
    }

    /**
     * Cache ServiceDay objects representing which services are running yesterday, today, and tomorrow relative to the search time. This information
     * is very heavily used (at every transit boarding) and Date operations were identified as a performance bottleneck. Must be called after the
     * TraverseOptions already has a CalendarService set.
     */
    private void setServiceDays() {
        Calendar c = Calendar.getInstance();
        c.setTime(new Date(opt.getSecondsSinceEpoch() * 1000));
        c.setTimeZone(graph.getTimeZone());

        final ServiceDate serviceDate = new ServiceDate(c);
        this.serviceDays = new ArrayList<ServiceDay>(3);
        if (calendarService == null && graph.getCalendarService() != null
                && (opt.modes == null || opt.modes.contains(TraverseMode.TRANSIT))) {
            LOG.warn("RoutingContext has no CalendarService. Transit will never be boarded.");
            return;
        }

        // In general, there should be a set of service days (yesterday, today, tomorrow) for every
        // timezone in the graph. The parameter `serviceDayLookout` allows more service days to be
        // added to search for future service (or past, if arriveBy=true). The furthest back trips
        // can begin is 1 service day (e.g. a trip which started yesterday is usable today.) This
        // does not address the case where a trip started multiple days ago (e.g. a multi-day ferry
        // trip will not be board-able after day 2).
        for (TimeZone timeZone : graph.getAllTimeZones()) {
            // Add today
            addIfNotExists(this.serviceDays, new ServiceDay(graph, serviceDate, calendarService, timeZone));
            // Add one day previous (previous in the direction of the transit search, so yesterday if
            // arriveBy=false and tomorrow if arriveBy=true
            addIfNotExists(this.serviceDays, new ServiceDay(graph,
                    opt.arriveBy ? serviceDate.next() : serviceDate.previous(),
                    calendarService, timeZone));
            // Add one or more days in the "forward" direction
            ServiceDate sd = serviceDate;
            int lookout = Math.max(1, opt.serviceDayLookout);
            for (int i = 0; i < lookout; i++) {
                sd = opt.arriveBy ? sd.previous() : sd.next();
                addIfNotExists(this.serviceDays, new ServiceDay(graph, sd, calendarService, timeZone));
            }
        }
        serviceDays.sort(Comparator.comparing(ServiceDay::getServiceDate));
    }

    private static <T> void addIfNotExists(ArrayList<T> list, T item) {
        if (!list.contains(item)) {
            list.add(item);
        }
    }

    /** check if the start and end locations are accessible */
    public boolean isAccessible() {
        if (opt.wheelchairAccessible) {
            return isWheelchairAccessible(fromVertex) && isWheelchairAccessible(toVertex);
        }
        return true;
    }

    // this could be handled by method overloading on Vertex
    public boolean isWheelchairAccessible(Vertex v) {
        if (v instanceof TransitStop) {
            TransitStop ts = (TransitStop) v;
            return ts.hasWheelchairEntrance();
        } else if (v instanceof StreetLocation) {
            StreetLocation sl = (StreetLocation) v;
            return sl.isWheelchairAccessible();
        }
        return true;
    }

    /**
     * Tear down this routing context, removing any temporary edges from
     * the "permanent" graph objects. This enables all temporary objects
     * for garbage collection.
     */
    public void destroy() {
       TemporaryVertex.disposeAll(temporaryVertices);
       temporaryVertices.clear();
    }
}