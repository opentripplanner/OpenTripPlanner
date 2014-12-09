/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.core;

import com.google.common.collect.Iterables;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.api.resource.DebugOutput;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.edgetype.PartialStreetEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TimetableResolver;
import org.opentripplanner.routing.error.GraphNotFoundException;
import org.opentripplanner.routing.error.TransitTimesException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultRemainingWeightHeuristicFactoryImpl;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.pathparser.PathParser;
import org.opentripplanner.routing.services.OnBoardDepartService;
import org.opentripplanner.routing.services.RemainingWeightHeuristicFactory;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.updater.stoptime.TimetableSnapshotSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A RoutingContext holds information needed to carry out a search for a particular TraverseOptions, on a specific graph. Includes things like
 * (temporary) endpoint vertices, transfer tables, service day caches, etc.
 * 
 * @author abyrd
 */
public class RoutingContext implements Cloneable {

    private static final Logger LOG = LoggerFactory.getLogger(RoutingContext.class);

    private static RemainingWeightHeuristicFactory heuristicFactory = new DefaultRemainingWeightHeuristicFactoryImpl();

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

    public final ArrayList<Vertex> intermediateVertices = new ArrayList<Vertex>();

    // public final Calendar calendar;
    public final CalendarService calendarService;

    public final Map<AgencyAndId, Set<ServiceDate>> serviceDatesByServiceId = new HashMap<AgencyAndId, Set<ServiceDate>>();

    public RemainingWeightHeuristic remainingWeightHeuristic;

    public final TransferTable transferTable;

    /** The timetableSnapshot is a {@link TimetableResolver} for looking up real-time updates. */
    public final TimetableResolver timetableSnapshot;

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

    public PathParser[] pathParsers = new PathParser[] {};

    public Vertex startingStop;

    /** An object that accumulates profiling and debugging info for inclusion in the response. */
    public DebugOutput debugOutput = new DebugOutput();

    /** Indicates that the search timed out or was otherwise aborted. */
    public boolean aborted;
    
    /* CONSTRUCTORS */

    /**
     * Constructor that automatically computes origin/target from RoutingRequest.
     */
    public RoutingContext(RoutingRequest routingRequest, Graph graph) {
        this(routingRequest, graph, null, null, true);
    }

    /**
     * Constructor that takes to/from vertices as input.
     */
    public RoutingContext(RoutingRequest routingRequest, Graph graph, Vertex from, Vertex to) {
        this(routingRequest, graph, from, to, false);
    }

    /**
     * Returns the PlainStreetEdges that overlap between two vertices edge sets.
     */
    private Set<StreetEdge> overlappingPlainStreetEdges(Vertex u, Vertex v) {
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
        Set<StreetEdge> overlap = new HashSet<StreetEdge>();
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
     * Creates a PartialPlainStreetEdge along the input PlainStreetEdge.
     * 
     * Orders the endpoints u, v so that they are consistent with the direction of the edge e.
     */
    private PartialStreetEdge makePartialEdgeAlong(StreetEdge e, StreetVertex u,
            StreetVertex v) {
        DistanceLibrary dLib = SphericalDistanceLibrary.getInstance();

        Vertex head = e.getFromVertex();
        double uDist = dLib.fastDistance(head.getCoordinate(), u.getCoordinate());
        double vDist = dLib.fastDistance(head.getCoordinate(), v.getCoordinate());

        // Order the vertices along the partial edge by distance from the head of the edge.
        // TODO(flamholz): this logic is insufficient for curvy streets/roundabouts.
        StreetVertex first = u;
        StreetVertex second = v;
        if (vDist < uDist) {
            first = v;
            second = u;
        }

        Geometry parentGeom = e.getGeometry();
        LineString myGeom = GeometryUtils.getInteriorSegment(parentGeom, first.getCoordinate(),
                second.getCoordinate());

        double lengthRatio = myGeom.getLength() / parentGeom.getLength();
        double length = e.getDistance() * lengthRatio;

        String name = first.getLabel() + " to " + second.getLabel();
        return new PartialStreetEdge(e, first, second, myGeom, name, length);
    }

    /**
     * Flexible constructor which may compute to/from vertices.
     * 
     * TODO(flamholz): delete this flexible constructor and move the logic to constructors above appropriately.
     * 
     * @param findPlaces if true, compute origin and target from RoutingRequest using spatial indices.
     */
    private RoutingContext(RoutingRequest routingRequest, Graph graph, Vertex from, Vertex to,
            boolean findPlaces) {
        if (graph == null) {
            throw new GraphNotFoundException();
        }
        this.opt = routingRequest;
        this.graph = graph;
        this.debugOutput.startedCalculating();

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

        Edge fromBackEdge = null;
        Edge toBackEdge = null;
        if (findPlaces) {
            // normal mode, search for vertices based RoutingRequest
            if (!opt.batch || opt.arriveBy) {
                // non-batch mode, or arriveBy batch mode: we need a to vertex
                toVertex = graph.streetIndex.getVertexForLocation(opt.to, opt);
                if (opt.to.hasEdgeId()) {
                    toBackEdge = graph.getEdgeById(opt.to.edgeId);
                }
            } else {
                toVertex = null;
            }
            if (opt.startingTransitTripId != null && !opt.arriveBy) {
                // Depart on-board mode: set the from vertex to "on-board" state
                OnBoardDepartService onBoardDepartService = graph.getService(OnBoardDepartService.class);
                if (onBoardDepartService == null)
                    throw new UnsupportedOperationException("Missing OnBoardDepartService");
                fromVertex = onBoardDepartService.setupDepartOnBoard(this);
            } else if (!opt.batch || !opt.arriveBy) {
                // non-batch mode, or depart-after batch mode: we need a from vertex
                fromVertex = graph.streetIndex.getVertexForLocation(opt.from, opt, toVertex);
                if (opt.from.hasEdgeId()) {
                    fromBackEdge = graph.getEdgeById(opt.from.edgeId);
                }
            } else {
                fromVertex = null;
            }
            if (opt.intermediatePlaces != null) {
                for (GenericLocation intermediate : opt.intermediatePlaces) {
                    Vertex vertex = graph.streetIndex.getVertexForLocation(intermediate, opt);
                    intermediateVertices.add(vertex);
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
        if (fromVertex instanceof StreetLocation && toVertex instanceof StreetLocation) {
            StreetVertex fromStreetVertex = (StreetVertex) fromVertex;
            StreetVertex toStreetVertex = (StreetVertex) toVertex;
            Set<StreetEdge> overlap = overlappingPlainStreetEdges(fromStreetVertex,
                    toStreetVertex);

            for (StreetEdge pse : overlap) {
                PartialStreetEdge ppse = makePartialEdgeAlong(pse, fromStreetVertex, toStreetVertex);
                // Register this edge-fragment as a temporary edge so it will be assigned a routing context and cleaned up.
                // It's connecting the from and to vertices so it could be placed in either vertex's temp edge list.
                ((StreetLocation)fromVertex).getExtra().add(ppse);
            }
        }
        
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
            remainingWeightHeuristic = heuristicFactory.getInstanceForSearch(opt);

        // If any temporary half-street-edges were created, record the fact that they should
        // only be visible to the routing context we are currently constructing.
        for (Vertex vertex : new Vertex[] {fromVertex, toVertex}) {
            if (vertex instanceof StreetLocation) {
                ((StreetLocation)vertex).setTemporaryEdgeVisibility(this);
            }
        }

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
        if (!opt.batch || opt.arriveBy) //
            if (toVertex == null)
                notFound.add("to");

        for (int i = 0; i < intermediateVertices.size(); i++) {
            if (intermediateVertices.get(i) == null) {
                notFound.add("intermediate." + i);
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

        for (String agency : graph.getAgencyIds()) {
            addIfNotExists(this.serviceDays, new ServiceDay(graph, serviceDate.previous(),
                    calendarService, agency));
            addIfNotExists(this.serviceDays, new ServiceDay(graph, serviceDate, calendarService, agency));
            addIfNotExists(this.serviceDays, new ServiceDay(graph, serviceDate.next(),
                    calendarService, agency));
        }
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
     * Tear down this routing context, removing any temporary edges.
     * 
     * @returns the number of edges removed.
     */
    public int destroy() {
        int nRemoved = 0;
        if (origin != null)
            nRemoved += origin.removeTemporaryEdges(graph);
        if (target != null)
            nRemoved += target.removeTemporaryEdges(graph);
        for (Vertex v : intermediateVertices)
            nRemoved += v.removeTemporaryEdges(graph);
        return nRemoved;
    }

}
