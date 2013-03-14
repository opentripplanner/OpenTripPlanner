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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.services.calendar.CalendarService;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.common.model.NamedPlace;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.TrivialRemainingWeightHeuristic;
import org.opentripplanner.routing.edgetype.TimetableResolver;
import org.opentripplanner.routing.error.TransitTimesException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.impl.DefaultRemainingWeightHeuristicFactoryImpl;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.pathparser.PathParser;
import org.opentripplanner.routing.services.RemainingWeightHeuristicFactory;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A RoutingContext holds information needed to carry out a search for a particular TraverseOptions,
 * on a specific graph. Includes things like (temporary) endpoint vertices, transfer tables, 
 * service day caches, etc.
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
    public final ArrayList<Vertex> intermediateVertices = new ArrayList<Vertex>();
    //public final Calendar calendar;
    public final CalendarService calendarService;
    public final Map<AgencyAndId, Set<ServiceDate>> serviceDatesByServiceId = new HashMap<AgencyAndId, Set<ServiceDate>>();
    public RemainingWeightHeuristic remainingWeightHeuristic;
    public final TransferTable transferTable;
    public final TimetableResolver timetableSnapshot; 
    
    /**
     * Cache lists of which transit services run on which midnight-to-midnight periods. This ties a
     * TraverseOptions to a particular start time for the duration of a search so the same options
     * cannot be used for multiple searches concurrently. To do so this cache would need to be moved
     * into StateData, with all that entails.
     */
    public ArrayList<ServiceDay> serviceDays;

    /**
     * The search will be aborted if it is still running after this time (in milliseconds since the 
     * epoch). A negative or zero value implies no limit. 
     * This provides an absolute timeout, whereas the maxComputationTime is relative to the 
     * beginning of an individual search. While the two might seem equivalent, we trigger search 
     * retries in various places where it is difficult to update relative timeout value. 
     * The earlier of the two timeouts is applied. 
     */
    public long searchAbortTime = 0;
    
    public PathParser[] pathParsers = new PathParser[] { };

    public Vertex startingStop;
    
    /* CONSTRUCTORS */
    
    /**
     * Constructor that automatically computes to/from vertices from RoutingRequest.
     * 
     * @param routingRequest
     * @param graph
     */
    public RoutingContext(RoutingRequest routingRequest, Graph graph) {
        this(routingRequest, graph, null, null, true);
    }

    /**
     * Constructor that takes to/from vertices as input.
     * 
     * @param routingRequest
     * @param graph
     * @param from
     * @param to
     */
    public RoutingContext(RoutingRequest routingRequest, Graph graph, Vertex from, Vertex to) {
        this(routingRequest, graph, from, to, false);
    }

    /**
     * Flexible constructor which may compute to/from vertices.
     * 
     * TODO(flamholz): delete this flexible constructor and move the logic to constructors above appropriately.
     * 
     * @param routingRequest
     * @param graph
     * @param from
     * @param to
     * @param findPlaces
     */
    public RoutingContext(RoutingRequest routingRequest, Graph graph, Vertex from, Vertex to,
            boolean findPlaces) {
        this.opt = routingRequest;
        this.graph = graph;

        if (findPlaces) {
            // normal mode, search for vertices based RoutingRequest
            if (!opt.batch || opt.arriveBy) {
                // non-batch mode, or arriveBy batch mode: we need a to vertex
                toVertex = graph.streetIndex.getVertexForLocation(opt.getTo(), opt);
            } else {
                toVertex = null;
            }
            if (!opt.batch || !opt.arriveBy) {
                // non-batch mode, or depart-after batch mode: we need a from vertex
                fromVertex = graph.streetIndex.getVertexForLocation(opt.getFrom(), opt, toVertex);
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
        if (opt.getStartingTransitStopId() != null) {
            TransitIndexService tis = graph.getService(TransitIndexService.class);
            if (tis == null) {
                throw new RuntimeException("Next/Previous/First/Last trip "
                        + "functionality depends on the transit index. Rebuild "
                        + "the graph with TransitIndexBuilder");
            }
            AgencyAndId stopId = opt.getStartingTransitStopId();
            startingStop = tis.getPreBoardEdge(stopId).getToVertex();
        }
        origin = opt.arriveBy ? toVertex : fromVertex;
        target = opt.arriveBy ? fromVertex : toVertex;
        calendarService = graph.getCalendarService();
        transferTable = graph.getTransferTable();
        // the graph's snapshot may be frequently updated.
        // Grab a reference to ensure a coherent view of the timetables throughout this search.
        if (graph.timetableSnapshotSource != null)
            timetableSnapshot = graph.timetableSnapshotSource.getSnapshot();
        else
            timetableSnapshot = null;
        setServiceDays();
        if (opt.batch)
            remainingWeightHeuristic = new TrivialRemainingWeightHeuristic();
        else
            remainingWeightHeuristic = heuristicFactory.getInstanceForSearch(opt);
    }
    
    
    /* INSTANCE METHODS */
    
    public void check() {
        ArrayList<String> notFound = new ArrayList<String>();

        // check origin present when not doing an arrive-by batch search
        if ( ! (opt.batch && opt.arriveBy)) 
        	if (fromVertex == null) 
        		notFound.add("from");
        
        // check destination present when not doing a depart-after batch search
        if ( !opt.batch || opt.arriveBy) // 
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
        if (opt.getModes().isTransit() && ! graph.transitFeedCovers(opt.dateTime)) {
            // user wants a path through the transit network,
            // but the date provided is outside those covered by the transit feed.
            throw new TransitTimesException();
        }
    }
    
    /**
     *  Cache ServiceDay objects representing which services are running yesterday, today, and tomorrow relative
     *  to the search time. This information is very heavily used (at every transit boarding) and Date operations were
     *  identified as a performance bottleneck. Must be called after the TraverseOptions already has a CalendarService set. 
     */
    public void setServiceDays() {
        final long SEC_IN_DAY = 60 * 60 * 24;
        final long time = opt.getSecondsSinceEpoch();
        this.serviceDays = new ArrayList<ServiceDay>(3);
        if (calendarService == null && graph.getCalendarService() != null && (opt.getModes() == null || opt.getModes().contains(TraverseMode.TRANSIT))) {
            LOG.warn("RoutingContext has no CalendarService. Transit will never be boarded.");
            return;
        }
        // This should be a valid way to find yesterday and tomorrow,
        // since DST changes more than one hour after midnight in US/EU.
        // But is this true everywhere?
        for (String agency : graph.getAgencyIds()) {
            addIfNotExists(this.serviceDays, new ServiceDay(graph, time - SEC_IN_DAY, calendarService, agency));
            addIfNotExists(this.serviceDays, new ServiceDay(graph, time, calendarService, agency));
            addIfNotExists(this.serviceDays, new ServiceDay(graph, time + SEC_IN_DAY, calendarService, agency));
        }
    }

    private static<T> void addIfNotExists(ArrayList<T> list, T item) {
        if (!list.contains(item)) {
            list.add(item);
        }
    }

    /** check if the start and end locations are accessible */
    public boolean isAccessible() {
        if (opt.isWheelchairAccessible()) {
            return isWheelchairAccessible(fromVertex) &&
                   isWheelchairAccessible(toVertex);
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
     * @returns the number of edges removed. 
     */
    public int destroy() {
        int nRemoved = 0;
        if (origin != null) 
            nRemoved += origin.removeTemporaryEdges();
        if (target != null)
            nRemoved += target.removeTemporaryEdges();
        for (Vertex v : intermediateVertices)
            nRemoved += v.removeTemporaryEdges();
        return nRemoved;
    }

}
