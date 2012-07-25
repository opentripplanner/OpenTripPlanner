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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.common.pqueue.OTPPriorityQueue;
import org.opentripplanner.common.pqueue.OTPPriorityQueueFactory;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.algorithm.strategies.TransitLocalStreetService;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.pathparser.BasicPathParser;
import org.opentripplanner.routing.pathparser.PathParser;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.transit_index.RouteSegment;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.springframework.beans.factory.annotation.Autowired;

import com.vividsolutions.jts.geom.Coordinate;

public class RaptorPrecompWalk implements PathService {

    @Autowired
    private GraphService graphService;

    private RaptorStop[] stops;

    private List<RaptorRoute> routes = new ArrayList<RaptorRoute>();

    private List<RaptorRoute>[] routesForStop;

    private HashMap<AgencyAndId, RaptorStop> raptorStopsForStopId = new HashMap<AgencyAndId, RaptorStop>();

    private List<T2<Double, RaptorStop>>[] nearbyStops;

    private int nTotalStops;

    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    @SuppressWarnings("unchecked")
    public void init() {
        if (stops != null)
            return;

        Graph graph = graphService.getGraph();

        TransitIndexService transitIndex = graph.getService(TransitIndexService.class);

        nTotalStops = 0;
        for (Vertex v : graph.getVertices()) {
            if (v instanceof TransitStop) {
                nTotalStops++;
            }
        }
        routesForStop = new List[nTotalStops];

        stops = new RaptorStop[nTotalStops];

        for (String agency : transitIndex.getAllAgencies()) {
            for (RouteVariant variant : transitIndex.getVariantsForAgency(agency)) {
                ArrayList<Stop> variantStops = variant.getStops();
                final int nStops = variantStops.size();

                int nPatterns = variant.getSegments().size() / nStops;
                RaptorRoute route = new RaptorRoute(nStops, nPatterns);
                routes.add(route);

                for (int i = 0; i < nStops; ++i) {

                    final Stop stop = variantStops.get(i);
                    RaptorStop raptorStop = makeRaptorStop(stop);
                    route.stops[i] = raptorStop;
                    if (routesForStop[raptorStop.index] == null)
                        routesForStop[raptorStop.index] = new ArrayList<RaptorRoute>();
                    routesForStop[raptorStop.index].add(route);
                }

                List<RouteSegment> segments = variant.getSegments();
                // this sorter ensures that route segments are ordered by stop sequence, and, at a
                // given stop, patterns are in a consistent order
                Collections.sort(segments, new RouteSegmentComparator());
                int stop = 0;
                int pattern = 0;
                for (RouteSegment segment : segments) {
                    if (stop != nStops - 1) {
                        for (Edge e : segment.board.getFromVertex().getIncoming()) {
                            if (e instanceof PreBoardEdge) {
                                route.stops[stop].stopVertex = (TransitStop) e.getFromVertex();
                            }
                        }
                        route.boards[stop][pattern] = (PatternBoard) segment.board;
                    }
                    if (stop != 0) {
                        for (Edge e : segment.alight.getToVertex().getOutgoing()) {
                            if (e instanceof PreAlightEdge) {
                                route.stops[stop].stopVertex = (TransitStop) e.getToVertex();
                            }
                        }

                        route.alights[stop - 1][pattern] = (PatternAlight) segment.alight;
                    }
                    if (++pattern == nPatterns) {
                        pattern = 0;
                        stop++;
                    }
                }
                if (stop != nStops || pattern != 0) {
                    throw new RuntimeException("Wrong number of segments");
                }
            }
        }

        stops = Arrays.copyOfRange(stops, 0, raptorStopsForStopId.size());
        nTotalStops = stops.length;
        // initNearbyStops();
        //initPaths(graph);
    }
/*
    private void initPaths(Graph graph) {
        TransitLocalStreetService svc = graph.getService(TransitLocalStreetService.class);
        HashMap<Vertex, HashMap<Vertex, int[]>> paths = svc.getPaths();
        for (Map.Entry<Vertex, HashMap<Vertex, int[]>> entry : paths.entrySet()) {
            Vertex fromv = entry.getKey();
            HashMap<Vertex, int[]> map = entry.getValue();
            for (Map.Entry<Vertex, int[]> entry2 : map.entrySet()) {
                
            }
        }
    }
*/
    // this doesn't speed things up
    @SuppressWarnings("unchecked")
    private void initNearbyStops() {
        nearbyStops = new List[nTotalStops];
        for (int i = 0; i < nTotalStops; ++i) {
            if (i % 500 == 0) {
                System.out.println("HERE:" + i);
            }
            nearbyStops[i] = new ArrayList<T2<Double, RaptorStop>>();
            RaptorStop stop = stops[i];
            Coordinate coord = stop.stopVertex.getCoordinate();
            for (RaptorStop other : stops) {
                Coordinate otherCoord = other.stopVertex.getCoordinate();
                double distance = distanceLibrary.fastDistance(coord, otherCoord);
                if (distance > 8047) // 5 mi
                    continue;
                nearbyStops[i].add(new T2<Double, RaptorStop>(distance, other));
            }
            Collections.sort(nearbyStops[i], new Comparator<T2<Double, RaptorStop>>() {

                @Override
                public int compare(T2<Double, RaptorStop> arg0, T2<Double, RaptorStop> arg1) {
                    return (int) Math.signum(arg0.getFirst() - arg1.getFirst());
                }

            });
        }
    }

    private RaptorStop makeRaptorStop(Stop stop) {
        RaptorStop rs = raptorStopsForStopId.get(stop.getId());
        if (rs == null) {
            rs = new RaptorStop();
            rs.index = raptorStopsForStopId.size();
            stops[rs.index] = rs;
            raptorStopsForStopId.put(stop.getId(), rs);
        }
        return rs;
    }

    @Override
    public List<GraphPath> getPaths(RoutingRequest options) {

        if (options.rctx == null) {
            options.setRoutingContext(graphService.getGraph(options.getRouterId()));
            options.rctx.pathParsers = new PathParser[1];
            options.rctx.pathParsers[0] = new BasicPathParser();
        }
        init();

        options.setAlightSlack(0);
        options.setBoardSlack(0);
        options.setTransferSlack(0);

        RoutingRequest walkOptions = options.clone();
        walkOptions.rctx.pathParsers = new PathParser[0];
        TraverseModeSet modes = options.getModes().clone();
        modes.setTransit(false);
        walkOptions.setModes(modes);
        RaptorPathSet routeSet = new RaptorPathSet(stops.length);
        // options.setMaxTransfers(5);
        // options.setMaxTransfers(0);
        for (int i = 0; i < options.getMaxTransfers() + 2; ++i) {
            round(options, walkOptions, routeSet, i);
            if (routeSet.getTargetStates().size() >= options.getNumItineraries())
                break;
        }

        if (routeSet.getTargetStates().isEmpty()) {
            System.out.println("RAPTOR found no paths (try retrying?)");
        }

        List<GraphPath> paths = new ArrayList<GraphPath>();
        for (RaptorState targetState : routeSet.getTargetStates()) {
            // reconstruct path
            ArrayList<RaptorState> states = new ArrayList<RaptorState>();
            RaptorState cur = targetState;
            while (cur != null) {
                states.add(cur);
                cur = cur.parent;
            }
            // states is in reverse order of time
            State state = new State(options);
            for (int i = states.size() - 1; i >= 0; --i) {
                cur = states.get(i);
                if (cur.walkPath != null) {
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
                    for (Edge e : state.getVertex().getOutgoing()) {
                        if (e instanceof PreBoardEdge) {
                            state = e.traverse(state);
                        }
                    }
                    PatternBoard board = cur.route.boards[cur.boardStopSequence][cur.patternIndex];
                    State oldState = state;
                    state = board.traverse(state);
                    if (state == null) {
                        state = board.traverse(oldState);
                    }
                    // now traverse the hops and dwells until we find the alight we're looking for
                    HOP: while (true) {
                        for (Edge e : state.getVertex().getOutgoing()) {
                            if (e instanceof PatternDwell) {
                                state = e.traverse(state);
                            } else if (e instanceof PatternHop) {
                                state = e.traverse(state);
                                for (Edge e2 : state.getVertex().getOutgoing()) {
                                    if (e2 instanceof PatternAlight) {
                                        for (Edge e3 : e2.getToVertex().getOutgoing()) {
                                            if (e3 instanceof PreAlightEdge) {
                                                if (raptorStopsForStopId.get(((TransitStop) e3
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
            paths.add(new GraphPath(state, true));
        }

        return paths;
    }

    private void round(RoutingRequest options, RoutingRequest walkOptions, RaptorPathSet cur,
            int nBoardings) {

        Set<RaptorStop> visitedLastRound = cur.visitedLastRound;
        Set<RaptorRoute> routesToVisit = new HashSet<RaptorRoute>();
        for (RaptorStop stop : visitedLastRound) {
            for (RaptorRoute route : routesForStop[stop.index]) {
                routesToVisit.add(route);
            }
        }

        cur.visitedLastRound = new HashSet<RaptorStop>();
        // RoutingContext rctx = walkOptions.rctx;
        List<RaptorState>[] statesByStop = cur.getStates();
        /*
         * RaptorPathSet cur = new RaptorPathSet(prev.getNStops()); List<RaptorState>[] statesByStop
         * = prev.getStates(); for (int stop = 0; stop < statesByStop.length; ++stop) {
         * 
         * //The first stage of round k sets τk (p) = τk−1 (p) f or all stops p: this sets an
         * //upper bound on the earliest arrival time at p with at most k trips. if
         * (statesByStop[stop] != null) { System.out.println("filling in for " + stop + ": " +
         * statesByStop[stop].size()); } cur.addStates(stop, statesByStop[stop]); }
         */
        /*
         * Consider a route r, and let T (r) = (t0 , t1 , . . . , t|T (r)|−1 ) be the sequence of
         * trips that follow route r, from earliest to latest. When processing route r, we consider
         * journeys where the last (k’th) trip taken is in route r. Let et(r, pi ) be the earliest
         * trip in route r that one can catch at stop pi , i. e., the earliest trip t such that τdep
         * (t, pi ) ≥ τk−1 (pi ). (Note that this trip may not exist, in which case et(r, pi ) is
         * undefined.) To process the route, we visit its stops in order until we find a stop pi
         * such that et(r, pi ) is defined. This is when we can “hop on” the route. Let the
         * corresponding trip t be the current trip for k. We keep traversing the route. For each
         * subsequent stop pj , we can update τk (pj ) using this trip. To reconstruct the journey,
         * we set a parent pointer to the stop at which t was boarded. Moreover, we may need to
         * update the current trip for k: at each stop pi along r it may be possible to catch an
         * earlier trip (because a quicker path to pi has been found in a previous round). Thus, we
         * have to check if τk−1 (pi ) < τarr (t, pi ) and update t by recomputing et(r, pi ).
         */

        // TODO: limit route set to those discovered in the last round

        List<RaptorState> createdStates = new ArrayList<RaptorState>();
        System.out.println("Round " + nBoardings);
        for (RaptorRoute route : routesToVisit) {
            List<RaptorState> boardStates = new ArrayList<RaptorState>(); // not really states
            boolean started = false;
            for (int stopNo = 0; stopNo < route.getNStops(); ++stopNo) {
                // find the current time at this stop
                RaptorStop stop = route.stops[stopNo];
                if (!started && !visitedLastRound.contains(stop))
                    continue;
                started = true;

                List<RaptorState> states = statesByStop[stop.index];
                List<RaptorState> newStates = new ArrayList<RaptorState>();

                if (states == null) {
                    states = new ArrayList<RaptorState>();
                    statesByStop[stop.index] = states;
                }
                // this checks the case of continuing on the current trips.
                CONTINUE: for (RaptorState boardState : boardStates) {

                    RaptorState newState = new RaptorState();

                    ServiceDay sd = boardState.serviceDay;

                    int alightTime = route.getAlightTime(boardState.patternIndex,
                            boardState.tripIndex, stopNo);
                    newState.arrivalTime = (int) sd.time(alightTime);

                    newState.boardStop = boardState.boardStop;
                    newState.boardStopSequence = boardState.boardStopSequence;
                    newState.route = route;
                    newState.patternIndex = boardState.patternIndex;
                    newState.tripIndex = boardState.tripIndex;
                    newState.nBoardings = boardState.nBoardings;
                    newState.walkDistance = boardState.walkDistance;
                    newState.parent = boardState.parent;
                    newState.stop = stop;
                    // todo: waiting time, which presently is not handled

                    for (RaptorState oldState : states) {
                        if (eDominates(oldState, newState)) {
                            continue CONTINUE;
                        }
                    }

                    for (RaptorState oldState : newStates) {
                        if (oldState != newState && eDominates(oldState, newState)) {
                            continue CONTINUE;
                        }
                    }

                    cur.visitedLastRound.add(stop);
                    cur.visitedEver.add(stop);
                    newStates.add(newState);
                }

                if (newStates.size() > 10) {
                    // System.out.println("HERE: " + newStates.size());
                }
                if (stopNo < route.getNStops() - 1) {

                    // try boarding here
                    TRYBOARD: for (RaptorState oldState : states) {
                        if (oldState.nBoardings != nBoardings - 1)
                            continue;
                        if (oldState.route == route)
                            continue; // we got here via this route, so no reason to transfer

                        RaptorBoardSpec boardSpec = route.getTripIndex(options,
                                oldState.arrivalTime, stopNo);
                        if (boardSpec == null)
                            continue;
                        RaptorState boardState = new RaptorState();
                        boardState.nBoardings = nBoardings;
                        boardState.boardStop = stop;
                        boardState.boardStopSequence = stopNo;
                        boardState.arrivalTime = boardSpec.departureTime;
                        boardState.patternIndex = boardSpec.patternIndex;
                        boardState.tripIndex = boardSpec.tripIndex;
                        boardState.parent = oldState;
                        boardState.serviceDay = boardSpec.serviceDay;
                        boardState.route = route;
                        boardState.walkDistance = oldState.walkDistance;

                        for (RaptorState state : newStates) {
                            if (eDominates(state, boardState)) {
                                continue TRYBOARD;
                            }
                        }

                        for (RaptorState state : states) {
                            if (state != oldState && eDominates(state, boardState)) {
                                continue TRYBOARD;
                            }
                        }
                        boardStates.add(boardState);
                    }
                }
                createdStates.addAll(newStates);
                states.addAll(newStates);
            }
        }

        /*
         * finally, the third stage of round k considers foot- paths. For each foot-path (pi , pj )
         * ∈ F it sets τk (pj ) = min{τk (pj ), τk (pi ) + (pi , pj )}. Note that since F is
         * transitive, we always find the fastest walking path, if one exists.
         */

        ShortestPathTree spt;
        GenericDijkstra dijkstra = new GenericDijkstra(walkOptions);
        if (nBoardings == 0) {
            State start = new MaxWalkState(options.rctx.origin, walkOptions);
            spt = dijkstra.getShortestPathTree(start);
            final List<? extends State> targetStates = spt.getStates(walkOptions.rctx.target);
            if (targetStates != null) {
                for (State targetState : targetStates) {
                    RaptorState state = new RaptorState();
                    RaptorState parent = (RaptorState) targetState.getExtension("raptorParent");
                    state.parent = parent;
                    state.walkDistance = targetState.getWalkDistance();
                    state.arrivalTime = (int) targetState.getTime();
                    if (parent != null) {
                        state.nBoardings = parent.nBoardings;
                        state.waitingTime = parent.waitingTime;
                    }
                    state.walkPath = targetState;
                    cur.addTargetState(state);
                    System.out.println("TARGET: " + state);
                }
            }
            SPTSTATE: for (State state : spt.getAllStates()) {
                // FIXME: build a special-case SPT that tracks states which are stops or target

                final Vertex vertex = state.getVertex();

                if (!(vertex instanceof TransitStop))
                    continue;
                RaptorStop stop = raptorStopsForStopId.get(((TransitStop) vertex).getStopId());
                if (stop == null) {
                    // we have found a stop is totally unused, so skip it
                    continue;
                }
                cur.visitedLastRound.add(stop);
                cur.visitedEver.add(stop);
                List<RaptorState> states = statesByStop[stop.index];
                if (states == null) {
                    states = new ArrayList<RaptorState>();
                    statesByStop[stop.index] = states;
                }
                RaptorState baseState = (RaptorState) state.getExtension("raptorParent");
                RaptorState newState = new RaptorState();
                if (baseState != null) {
                    newState.nBoardings = baseState.nBoardings;
                }
                newState.walkDistance = state.getWalkDistance();
                newState.arrivalTime = (int) state.getTime();
                newState.walkPath = state;
                newState.parent = baseState;
                newState.stop = stop;

                for (RaptorState oldState : states) {
                    if (eDominates(oldState, newState)) {
                        continue SPTSTATE;
                    }
                }

                Vertex dest = options.rctx.target;
                double minWalk = dest.getDistanceToNearestTransitStop();
                double minWalkTime = minWalk / options.getSpeedUpperBound();
                if (newState.walkDistance + minWalk > options.getMaxWalkDistance())
                    continue SPTSTATE;
                for (RaptorState oldState : cur.getTargetStates()) {
                    // newstate would have to take some transit and then walk to the destination.
                    if (oldState.arrivalTime <= newState.arrivalTime + minWalkTime
                            && oldState.walkDistance <= newState.walkDistance + minWalk)
                        // todo waiting time?
                        continue SPTSTATE;
                }

                states.add(newState);
            }
        } else {
            TransitLocalStreetService service = options.rctx.graph.getService(TransitLocalStreetService.class);
            HashMap<Vertex, HashMap<Vertex, T2<Double, Integer>>> costs = service.getCosts();
            for (RaptorState state : createdStates) {
                Vertex fromv = state.stop.stopVertex;
                HashMap<Vertex, T2<Double, Integer>> costsForVertex = costs.get(fromv);
                WALKEDTO: for (Map.Entry<Vertex, T2<Double, Integer>> entry : costsForVertex.entrySet()) {
                    T2<Double, Integer> costsForWalk = entry.getValue();
                    double walk = costsForWalk.getFirst() + state.walkDistance;
                    if (walk > options.getMaxWalkDistance()) continue;
                    int time = costsForWalk.getSecond() + state.arrivalTime;
                    
                    RaptorState newState = new RaptorState();
                    newState.walkDistance = walk;
                    newState.nBoardings = state.nBoardings;
                    newState.arrivalTime = time;
                    
                    TransitStop tov = (TransitStop) entry.getKey();
                    RaptorStop dest = raptorStopsForStopId.get(tov.getStopId());
                    if (dest == null) {
                        //this is somehow an unreachable point -- perhaps a parent stop
                        //System.out.println("unexpectedly unreachable stop " + tov);
                        continue;
                    }
                    List<RaptorState> states = statesByStop[dest.index];
                    if (states == null) {
                        states = new ArrayList<RaptorState>();
                    }
                    for (RaptorState oldState : states) {
                        if (eDominates(oldState, state)) {
                            continue WALKEDTO;
                        }
                    }
                    newState.parent = state; 
                    state.stop = dest;
                    state.tripIndex = -2; //this sentinel value will mean
                    cur.visitedLastRound.add(dest);
                    cur.visitedEver.add(dest);
                    //that the walk path is recomputed later
                    //for now, we can skip that, because we only care about the search
                    //time.  We should really ensure that most of the time is not spent in 
                    //fixup, but I bet it's not
                }
                
            }
        }

        // fill in
        for (int stop = 0; stop < statesByStop.length; ++stop) {
            cur.setStates(stop, statesByStop[stop]);
        }
    }

    class PrefilledPriorityQueueFactory implements OTPPriorityQueueFactory {

        private List<State> startPoints;

        public PrefilledPriorityQueueFactory(List<State> startPoints) {
            this.startPoints = startPoints;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        public <T> OTPPriorityQueue<T> create(int maxSize) {
            BinHeap heap = new BinHeap<T>();
            for (State state : startPoints) {
                heap.insert(state, 0);
            }
            return heap;
        }

    }

    private boolean eDominates(RaptorState state, RaptorState oldState) {
        // todo: epsilon dominance

        return state.nBoardings <= oldState.nBoardings && state.waitingTime <= oldState.waitingTime
                && state.walkDistance <= oldState.walkDistance
                && state.arrivalTime <= oldState.arrivalTime
        /*
         * && (state.nBoardings < oldState.nBoardings || state.waitingTime < oldState.waitingTime ||
         * state.walkDistance < oldState.walkDistance || state.arrivalTime < oldState.arrivalTime);
         */;
    }

}
