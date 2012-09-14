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

package org.opentripplanner.routing.impl.raptor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PatternInterlineDwell;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.pathparser.BasicPathParser;
import org.opentripplanner.routing.pathparser.NoThruTrafficPathParser;
import org.opentripplanner.routing.pathparser.PathParser;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class Raptor implements PathService {
    private static final Logger log = LoggerFactory.getLogger(Raptor.class);

    static final double MAX_TRANSIT_SPEED = 25;

    private static final int MAX_WALK_MULTIPLE = 8;

    @Autowired
    private GraphService graphService;

    private List<ServiceDay> cachedServiceDays;

    private RaptorData cachedRaptorData;

    private double multiPathTimeout = 0; // seconds

    /** Give up on searching for additional itineraries after this many seconds have elapsed. */
    public void setTimeout (double seconds) {
        multiPathTimeout = seconds;
    }

    /**
     * Stop searching for additional itineraries (beyond the first one) after this many seconds 
     * have elapsed, relative to the beginning of the search for the first itinerary. 
     * A negative or zero value means search forever. 
     */
    public void setMultiPathTimeout (double seconds) {
        multiPathTimeout = seconds;
    }

    //fallback for nontransit trips
    @Autowired public SPTService sptService;

    @Override
    public List<GraphPath> getPaths(RoutingRequest options) {

        final Graph graph = graphService.getGraph(options.getRouterId());
        if (options.rctx == null) {
            options.setRoutingContext(graph);
            options.rctx.pathParsers = new PathParser[] { new BasicPathParser(),
                    new NoThruTrafficPathParser() };
        }

        if (!options.getModes().isTransit()) {
            return sptService.getShortestPathTree(options).getPaths();
        }

        RaptorData data = graph.getService(RaptorDataService.class).getData();

        //we multiply the initial walk distance by 1.1 to account for epsilon dominance.
        double initialWalk = options.getMaxWalkDistance() * 1.1;
        //do not even bother with obviously impossible walks
        double minWalk = options.rctx.origin.getDistanceToNearestTransitStop() + options.rctx.target.getDistanceToNearestTransitStop();
        if (initialWalk < minWalk) {
            initialWalk = minWalk;
        }

        options.setMaxWalkDistance(initialWalk);

        RoutingRequest walkOptions = options.clone();
        walkOptions.rctx.pathParsers = new PathParser[0];
        TraverseModeSet modes = options.getModes().clone();
        modes.setTransit(false);
        walkOptions.setModes(modes);
        RaptorSearch search = new RaptorSearch(data, options);

        if (data.maxTransitRegions != null) {
            Calendar tripDate = Calendar.getInstance(graph.getTimeZone());
            tripDate.setTime(new Date(1000L * options.dateTime));

            Calendar maxTransitStart = Calendar.getInstance(graph.getTimeZone());
            maxTransitStart.set(Calendar.YEAR, data.maxTransitRegions.startYear);
            maxTransitStart.set(Calendar.MONTH, data.maxTransitRegions.startMonth);
            maxTransitStart.set(Calendar.DAY_OF_MONTH, data.maxTransitRegions.startDay);

            int day = 0;
            while (tripDate.after(maxTransitStart)) {
                day++;
                tripDate.add(Calendar.DAY_OF_MONTH, -1);
            }
            if (day > data.maxTransitRegions.maxTransit.length || options.isWheelchairAccessible()) {
                day = -1;
            }

            search.maxTimeDayIndex = day;
        }

        options.setMaxTransfers(6);

        int rushAheadRound = rushAhead(data, options, walkOptions, search);

        long searchBeginTime = System.currentTimeMillis();

        int bestElapsedTime = Integer.MAX_VALUE;
        int foundSoFar = 0;
        RETRY: do {
            for (int round = 0; round < options.getMaxTransfers() + 2; ++round) {
                round(data, options, walkOptions, search, round);

                long elapsed = System.currentTimeMillis() - searchBeginTime;
                if (elapsed > multiPathTimeout * 1000 && multiPathTimeout > 0
                        && search.getTargetStates().size() > 0)
                    break RETRY;

                if (search.getTargetStates().size() >= options.getNumItineraries() && round >= rushAheadRound) {
                    int oldBest = bestElapsedTime;
                    for (RaptorState state : search.getTargetStates()) {
                        final int elapsedTime = (int) Math
                                .abs(state.arrivalTime - options.dateTime);
                        if (elapsedTime < bestElapsedTime) {
                            bestElapsedTime = elapsedTime;
                        }
                    }
                    int improvement = oldBest - bestElapsedTime;
                    if (improvement < 600)
                        break RETRY;
                }

            }

            if (foundSoFar > search.getTargetStates().size()) {
                foundSoFar = search.getTargetStates().size();
            } else if (foundSoFar > 0) {
                //we didn't find anything new in this round, and we already have
                //some paths, so bail out
                break;
            }

            options.setMaxWalkDistance(options.getMaxWalkDistance() * 2);
            walkOptions.setMaxWalkDistance(options.getMaxWalkDistance());

            options.setWalkReluctance(options.getWalkReluctance() * 2);
            walkOptions.setWalkReluctance(options.getWalkReluctance());
            search.reset(options);

        } while (options.getMaxWalkDistance() < initialWalk * MAX_WALK_MULTIPLE && initialWalk < Double.MAX_VALUE);

        collectRoutesUsed(data, options, search.getTargetStates());

        List<RaptorState> targetStates = search.getTargetStates();
        if (targetStates.isEmpty()) {
            log.info("RAPTOR found no paths (try retrying?)");
        }
        Collections.sort(targetStates);

        List<GraphPath> paths = new ArrayList<GraphPath>();
        for (RaptorState targetState : targetStates) {
            // reconstruct path
            ArrayList<RaptorState> states = new ArrayList<RaptorState>();
            RaptorState cur = targetState;
            while (cur != null) {
                states.add(cur);
                cur = cur.getParent();
            }
            // states is in reverse order of time
            State state = getState(options, data, states);
            paths.add(new GraphPath(state, true));
        }

        return paths;
    }

    private void collectRoutesUsed(RaptorData data, RoutingRequest options, List<RaptorState> targetStates) {
        // find start/end regions
        List<Integer> startRegions = getRegionsForVertex(data.regionData, options.rctx.fromVertex);
        int startRegion;
        if (startRegions.size() == 1) {
            startRegion = startRegions.get(0);
        } else {
            // on boundary
            return;
        }
        List<Integer> endRegions = getRegionsForVertex(data.regionData, options.rctx.toVertex);
        int endRegion;
        if (endRegions.size() == 1) {
            endRegion = endRegions.get(0);
        } else {
            // on boundary
            return;
        }

        HashSet<RaptorRoute> routes = data.regionData.routes[startRegion][endRegion];
        HashSet<RaptorStop> stops = data.regionData.stops[startRegion][endRegion];
        TARGETSTATE: for (RaptorState state : targetStates) {
            for (RaptorState dom : targetStates) {
                if (dom.nBoardings <= state.nBoardings && dom.arrivalTime < state.arrivalTime) {
                    continue TARGETSTATE;
                }
            }
            while (state != null) {
                if (state.route != null)
                    routes.add(state.route);
                if (state.stop != null) {
                    stops.add(state.stop);
                }
                state = state.getParent();
            }
        }
    }

    private int rushAhead(RaptorData data, RoutingRequest options, RoutingRequest walkOptions,
            RaptorSearch search) {
        //find start/end regions
        List<Integer> startRegions = getRegionsForVertex(data.regionData, options.rctx.fromVertex);
        int startRegion;
        if (startRegions.size() == 1) {
            startRegion = startRegions.get(0);
        } else {
            log.debug("Trip start spans regions, not using rush-ahead optimization");
            return 0;
        }
        List<Integer> endRegions = getRegionsForVertex(data.regionData, options.rctx.toVertex);
        int endRegion;
        if (endRegions.size() == 1) {
            endRegion = endRegions.get(0);
        } else {
            log.debug("Trip end spans regions, not using rush-ahead optimization");
            return 0;
        }

        // create a reduced set of RaptorData with only the stops/routes previously seen on trips
        // from the start region to the end region

        RaptorData trimmedData = new RaptorData();
        trimmedData.raptorStopsForStopId = new HashMap<AgencyAndId, RaptorStop>();
        HashSet<RaptorStop> stops = data.regionData.stops[startRegion][endRegion];
        for (RaptorStop stop : stops) {
            trimmedData.raptorStopsForStopId.put(stop.stopVertex.getStopId(), stop);
        }
        trimmedData.regionData = data.regionData;
        trimmedData.routes = data.regionData.routes[startRegion][endRegion];
        trimmedData.stops = data.stops;
        //trimmedData.allowedStops = stops;
        trimmedData.routesForStop = data.routesForStop;
        walkOptions.setMaxWalkDistance(6000);
        if (trimmedData.routes.size() == 0) {
            log.debug("No cached routes, so not using rush-ahead optimization");
            return 0; //no precomputed routes
        }

        RaptorSearch rushSearch = new RaptorSearch(trimmedData, options);
        int bestElapsedTime = Integer.MAX_VALUE;
        int round;
        for (round = 0; round < options.getMaxTransfers() + 2; round++) {
            round(trimmedData, options, walkOptions,
                    rushSearch, round);

            if (search.getTargetStates().size() > 0) {
                int oldBest = bestElapsedTime;
                for (RaptorState state : search.getTargetStates()) {
                    final int elapsedTime = (int) Math
                            .abs(state.arrivalTime - options.dateTime);
                    if (elapsedTime < bestElapsedTime) {
                        bestElapsedTime = elapsedTime;
                    }
                }
                int improvement = oldBest - bestElapsedTime;
                if (improvement < 600)
                    break;
            }
        }
        for (RaptorState state : rushSearch.getTargetStates()) {
            search.bounder.addBounder(state.walkPath);
            search.addTargetState(state);
        }

        walkOptions.setMaxWalkDistance(options.getMaxWalkDistance());
        return round;
    }

    /** 
     * Some vertices aren't associated with a region, because they're synthetic, or
     * maybe for some other region.  So instead, we check their connected vertices,
     * recursively, to try to find their region.
     *
     * @param regionData
     * @param vertex
     * @return
     */
    static List<Integer> getRegionsForVertex(RegionData regionData, Vertex vertex) {
        return new ArrayList<Integer>(getRegionsForVertex(regionData, vertex, new HashSet<Vertex>(), 0));
    }

    /**
     * Internals of getRegionsForVertex; keeps track of seen vertices to avoid loops.
     * @param regionData
     * @param vertex
     * @param seen
     * @param depth
     * @return
     */
    private static HashSet<Integer> getRegionsForVertex(RegionData regionData, Vertex vertex,
            HashSet<Vertex> seen, int depth) {
        seen.add(vertex);
        HashSet<Integer> regions = new HashSet<Integer>();
        int region = vertex.getGroupIndex();
        if (region >= 0) {
            regions.add(region);
        } else {
            for (Edge e: vertex.getOutgoing()) {
                final Vertex tov = e.getToVertex();
                if (!seen.contains(tov))
                    regions.addAll(getRegionsForVertex(regionData, tov, seen, depth + 1));
            }
            for (Edge e: vertex.getIncoming()) {
                final Vertex fromv = e.getFromVertex();
                if (!seen.contains(fromv))
                    regions.addAll(getRegionsForVertex(regionData, fromv, seen, depth + 1));
            }
        }
        return regions;
    }

    private State getState(RoutingRequest options, RaptorData data, ArrayList<RaptorState> states) {
        if (options.arriveBy) {
            return getStateArriveBy(options, data, states);
        } else {
            return getStateDepartAt(options, data, states);
        }
    }

    private State getStateDepartAt(RoutingRequest options, RaptorData data,
            ArrayList<RaptorState> states) {
        State state = new State(options);
        for (int i = states.size() - 1; i >= 0; --i) {
            RaptorState cur = states.get(i);
            if (cur.walkPath != null) { //a walking step
                GraphPath path = new GraphPath(cur.walkPath, true);
                for (Edge e : path.edges) {
                    State oldState = state;
                    state = e.traverse(state);
                    if (state == null) {
                        e.traverse(oldState);
                    }
                }
            } else {
                // so, cur is at this point at a transit stop; we have a route to board
                if (cur.getParent() == null || ! cur.getParent().interlining) {
                    for (Edge e : state.getVertex().getOutgoing()) {
                        if (e instanceof PreBoardEdge) {
                            state = e.traverse(state);
                            break;
                        }
                    }
                    TransitBoardAlight board = cur.getRoute().boards[cur.boardStopSequence][cur.patternIndex];
                    state = board.traverse(state);
                }
                // now traverse the hops and dwells until we find the alight we're looking for
                HOP: while (true) {
                    for (Edge e : state.getVertex().getOutgoing()) {
                        if (e instanceof PatternDwell) {
                            state = e.traverse(state);
                        } else if (e instanceof PatternHop) {
                            state = e.traverse(state);
                            if (cur.interlining) {
                                for (Edge e2 : state.getVertex().getOutgoing()) {
                                    RaptorState next = states.get(i - 1);
                                    if (e2 instanceof PatternInterlineDwell) {
                                        if (((TransitVertex) e2.getToVertex()).getStop() == next.boardStop.stopVertex.getStop()) {
                                            state = e2.traverse(state);
                                            break HOP;
                                        }
                                    }
                                }
                            }
                            for (Edge e2 : state.getVertex().getOutgoing()) {
                                if (e2 instanceof TransitBoardAlight) {
                                    for (Edge e3 : e2.getToVertex().getOutgoing()) {
                                        if (e3 instanceof PreAlightEdge) {
                                            if (data.raptorStopsForStopId.get(((TransitStop) e3
                                                    .getToVertex()).getStopId()) == cur.stop) {
                                                state = e2.traverse(state);
                                                state = e3.traverse(state);
                                                break HOP;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return state;
    }

    private State getStateArriveBy(RoutingRequest options, RaptorData data,
            ArrayList<RaptorState> states) {
        State state = new State(options.rctx.origin, options);
        for (int i = states.size() - 1; i >= 0; --i) {
            RaptorState cur = states.get(i);
            if (cur.walkPath != null) {
                GraphPath path = new GraphPath(cur.walkPath, true);
                for (ListIterator<Edge> it = path.edges.listIterator(path.edges.size()); it.hasPrevious();) {
                    Edge e = it.previous();
                    State oldState = state;
                    state = e.traverse(state);
                    if (state == null) {
                        e.traverse(oldState);
                    }
                }
            } else {
                // so, cur is at this point at a transit stop; we have a route to alight from
                if (cur.getParent() == null || ! cur.getParent().interlining) {
                    for (Edge e : state.getVertex().getIncoming()) {
                        if (e instanceof PreAlightEdge) {
                            state = e.traverse(state);
                        }
                    }
                    TransitBoardAlight alight = cur.getRoute().alights[cur.boardStopSequence - 1][cur.patternIndex];
                    State oldState = state;
                    state = alight.traverse(state);
                    if (state == null) {
                        state = alight.traverse(oldState);
                    }
                }
                // now traverse the hops and dwells until we find the board we're looking for
                HOP: while (true) {
                    for (Edge e : state.getVertex().getIncoming()) {
                        if (e instanceof PatternDwell) {
                            state = e.traverse(state);
                        } else if (e instanceof PatternHop) {
                            state = e.traverse(state);
                            if (cur.interlining) {
                                for (Edge e2 : state.getVertex().getIncoming()) {
                                    RaptorState next = states.get(i - 1);
                                    if (e2 instanceof PatternInterlineDwell) {
                                        if (((TransitVertex) e2.getFromVertex()).getStop() == next.boardStop.stopVertex.getStop()) {
                                            state = e2.traverse(state);
                                            break HOP;
                                        }
                                    }
                                }
                            }
                            for (Edge e2 : state.getVertex().getIncoming()) {
                                if (e2 instanceof TransitBoardAlight) {
                                    for (Edge e3 : e2.getFromVertex().getIncoming()) {
                                        if (e3 instanceof PreBoardEdge) {
                                            if (data.raptorStopsForStopId.get(((TransitStop) e3
                                                    .getFromVertex()).getStopId()) == cur.stop) {
                                                state = e2.traverse(state);
                                                state = e3.traverse(state);
                                                break HOP;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return state;
    }

    /**
     * Prune raptor data to include only routes and boardings which have trips today. Doesn't
     * actually improve speed
     */
    @SuppressWarnings("unchecked")
    private RaptorData pruneDataForServiceDays(Graph graph, ArrayList<ServiceDay> serviceDays) {

        if (serviceDays.equals(cachedServiceDays))
            return cachedRaptorData;
        RaptorData data = graph.getService(RaptorDataService.class).getData();
        RaptorData pruned = new RaptorData();
        pruned.raptorStopsForStopId = data.raptorStopsForStopId;
        pruned.stops = data.stops;
        pruned.routes = new ArrayList<RaptorRoute>();
        pruned.routesForStop = new List[pruned.stops.length];

        for (RaptorRoute route : data.routes) {
            ArrayList<Integer> keep = new ArrayList<Integer>();

            for (int i = 0; i < route.boards[0].length; ++i) {
                Edge board = route.boards[0][i];
                int serviceId;
                if (board instanceof TransitBoardAlight) {
                    serviceId = ((TransitBoardAlight) board).getPattern().getServiceId();
                } else {
                    log.debug("Unexpected nonboard among boards");
                    continue;
                }
                for (ServiceDay day : serviceDays) {
                    if (day.serviceIdRunning(serviceId)) {
                        keep.add(i);
                        break;
                    }
                }
            }
            if (keep.isEmpty())
                continue;
            int nPatterns = keep.size();
            RaptorRoute prunedRoute = new RaptorRoute(route.getNStops(), nPatterns);
            for (int stop = 0; stop < route.getNStops() - 1; ++stop) {
                for (int pattern = 0; pattern < nPatterns; ++pattern) {
                    prunedRoute.boards[stop][pattern] = route.boards[stop][keep.get(pattern)];
                }
            }
            pruned.routes.add(route);
            for (RaptorStop stop : route.stops) {
                List<RaptorRoute> routes = pruned.routesForStop[stop.index];
                if (routes == null) {
                    routes = new ArrayList<RaptorRoute>();
                    pruned.routesForStop[stop.index] = routes;
                }
                routes.add(route);
            }
        }
        for (RaptorStop stop : data.stops) {
            if (pruned.routesForStop[stop.index] == null) {
                pruned.routesForStop[stop.index] = Collections.emptyList();
            }
        }
        cachedServiceDays = serviceDays;
        cachedRaptorData = pruned;
        return pruned;
    }

    private void round(RaptorData data, RoutingRequest options, RoutingRequest walkOptions,
            final RaptorSearch search, int nBoardings) {

        log.debug("Round " + nBoardings);

        /* Phase 2: handle transit */
        List<RaptorState> createdStates = search.transitPhase(options, nBoardings);

        /* Phase 3: handle walking paths */

        search.walkPhase(options, walkOptions, nBoardings,
                createdStates);
    }

    public RaptorStateSet getStateSet(RoutingRequest options) {

        final Graph graph;
        if (options.rctx == null) {
            graph = graphService.getGraph(options.getRouterId());
            options.setRoutingContext(graph);
            options.rctx.pathParsers = new PathParser[] { new BasicPathParser(),
                    new NoThruTrafficPathParser() };
        } else {
            graph = options.rctx.graph;
        }

        RaptorData data = graph.getService(RaptorDataService.class).getData();

        //we multiply the initial walk distance by 1.1 to account for epsilon dominance.
        options.setMaxWalkDistance(options.getMaxWalkDistance() * 1.1);

        RoutingRequest walkOptions = options.clone();
        walkOptions.rctx.pathParsers = new PathParser[0];
        TraverseModeSet modes = options.getModes().clone();
        modes.setTransit(false);
        walkOptions.setModes(modes);
        RaptorSearch search = new RaptorSearch(data, options);

        for (int i = 0; i < options.getMaxTransfers() + 2; ++i) {
            round(data, options, walkOptions, search, i);
        }
        RaptorStateSet result = new RaptorStateSet();
        result.statesByStop = search.statesByStop;
        return result;
    }

}
