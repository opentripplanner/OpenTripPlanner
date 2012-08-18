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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.T2;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.common.pqueue.OTPPriorityQueue;
import org.opentripplanner.common.pqueue.OTPPriorityQueueFactory;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.Board;
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
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class Raptor implements PathService {
    private static final Logger log = LoggerFactory.getLogger(Raptor.class);

    static final double MAX_TRANSIT_SPEED = 25;

    @Autowired
    private GraphService graphService;

    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    private List<ServiceDay> cachedServiceDays;

    private RaptorData cachedRaptorData;

    @Override
    public List<GraphPath> getPaths(RoutingRequest options) {
        
        if (options.rctx == null) {
            options.setRoutingContext(graphService.getGraph(options.getRouterId()));
            options.rctx.pathParsers = new PathParser[1];
            options.rctx.pathParsers[0] = new BasicPathParser();
        }
        Graph graph = graphService.getGraph(options.getRouterId());
        RaptorData data = graph.getService(RaptorDataService.class).getData();

        /* this does not actually affect speed either way (perhaps unfortunately)
        options.setAlightSlack(120);
        options.setBoardSlack(120);
        options.setTransferSlack(240);
         */

        RoutingRequest walkOptions = options.clone();
        walkOptions.rctx.pathParsers = new PathParser[0];
        TraverseModeSet modes = options.getModes().clone();
        modes.setTransit(false);
        walkOptions.setModes(modes);
        RaptorPathSet routeSet = new RaptorPathSet(data.stops.length, options);

        Calendar tripDate = Calendar.getInstance(graph.getTimeZone());
        tripDate.setTime(new Date(1000L*options.dateTime));
        
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

        routeSet.maxTimeDayIndex = day; 

        options.setMaxTransfers(options.maxTransfers + 2);
        
        for (int i = 0; i < options.getMaxTransfers() + 2; ++i) {
            round(data, options, walkOptions, routeSet, i);

            if (routeSet.getTargetStates().size() >= options.getNumItineraries()) break;
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
            State state = getState(options, data, states);
            paths.add(new GraphPath(state, true));
        }

        return paths;
    }

    private State getState(RoutingRequest options, RaptorData data, ArrayList<RaptorState> states) {
        RaptorState cur;
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

    /**
     * Prune raptor data to include only routes and boardings which have trips today.
     * Doesn't actually improve speed
     */
    private RaptorData pruneDataForServiceDays(Graph graph, ArrayList<ServiceDay> serviceDays) {

        if (serviceDays.equals(cachedServiceDays)) return cachedRaptorData;
        //you are here: need to reduce list of boards
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
                if (board instanceof PatternBoard) {
                    serviceId = ((PatternBoard) board).getPattern().getServiceId();
                } else if (board instanceof Board) {
                    serviceId = ((Board) board).getServiceId();
                } else {
                    System.out.println("Unexpected nonboard among boards");
                    continue;
                }
                for (ServiceDay day : serviceDays) {
                    if (day.serviceIdRunning(serviceId)) {
                        keep.add(i);
                        break;
                    }
                }
            }
            if (keep.isEmpty()) continue;
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
            final RaptorPathSet cur, int nBoardings) {

        Collection<RaptorRoute> routesToVisit = new HashSet<RaptorRoute>();
        Set<RaptorStop> visitedLastRound = cur.visitedLastRound;
        List<RaptorState> createdStates = new ArrayList<RaptorState>();
        List<RaptorState>[] statesByStop = cur.getStates();

        for (RaptorStop stop : visitedLastRound) {
            for (RaptorRoute route : data.routesForStop[stop.index]) {
                routesToVisit.add(route);
            }
        }
        cur.visitedLastRound = new HashSet<RaptorStop>();
        // RoutingContext rctx = walkOptions.rctx;

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
        int boardSlack = nBoardings == 1 ? options.getBoardSlack() : (options.getTransferSlack() - options.getAlightSlack());
        int trips = 0;
        System.out.println("Round " + nBoardings);
        final double distanceToNearestTransitStop = options.rctx.target
                .getDistanceToNearestTransitStop();
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

                    if (!route.boards[0][boardState.patternIndex].getPattern().canAlight(stopNo)) {
                        continue;
                    }
                    int alightTime = route.getAlightTime(boardState.tripTimes, stopNo);
                    newState.arrivalTime = (int) sd.time(alightTime);

                    //add in slack
                    newState.arrivalTime += options.getAlightSlack();
                    
                    newState.boardStop = boardState.boardStop;
                    newState.boardStopSequence = boardState.boardStopSequence;
                    newState.route = route;
                    newState.patternIndex = boardState.patternIndex;
                    newState.tripTimes = boardState.tripTimes;
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
                        if (eDominates(oldState, newState)) {
                            continue CONTINUE;
                        }
                    }

                    Iterator<RaptorState> it = states.iterator();
                    while(it.hasNext()) {
                        RaptorState oldState = it.next();
                        if (eDominates(newState, oldState)) {
                            it.remove();
                        }
                    }
                    it = newStates.iterator();
                    while(it.hasNext()) {
                        RaptorState oldState = it.next();
                        if (eDominates(newState, oldState)) {
                            it.remove();
                        }
                    }

                    cur.visitedLastRound.add(stop);
                    cur.visitedEver.add(stop);
                    newStates.add(newState);
/*
                    StopNearTarget nearTarget = cur.stopsNearTarget.get(stop);
                    if (nearTarget != null) {
                        RaptorState bound = new RaptorState();
                        bound.arrivalTime = newState.arrivalTime + nearTarget.time;
                        bound.walkDistance = newState.walkDistance + nearTarget.walkDistance;
                        if (bound.walkDistance <= options.maxWalkDistance) {
                            bound.nBoardings = newState.nBoardings;
                            bound.stop = stop;

                            for (RaptorState oldBound : cur.boundingStates) {
                                if (eDominates(oldBound, bound)) {
                                    continue CONTINUE;
                                }
                            }
                            cur.boundingStates.add(bound);
                        }
                    }
                    */
                }

                if (stopNo < route.getNStops() - 1) {

                    if (stop.stopVertex.isLocal() && nBoardings > 1) {
                        // cannot transfer at a local stop
                        createdStates.addAll(newStates);
                        states.addAll(newStates);
                        continue;
                    }

                    // try boarding here
                    TRYBOARD: for (RaptorState oldState : states) {
                        if (oldState.nBoardings != nBoardings - 1)
                            continue;
                        if (oldState.route == route)
                            continue; // we got here via this route, so no reason to transfer

                        ++trips;
                        RaptorBoardSpec boardSpec = route.getTripIndex(options,
                                oldState.arrivalTime + boardSlack, stopNo);
                        if (boardSpec == null)
                            continue;

                        RaptorState boardState = new RaptorState();
                        boardState.nBoardings = nBoardings;
                        boardState.boardStop = stop;
                        boardState.boardStopSequence = stopNo;
                        boardState.arrivalTime = boardSpec.departureTime;
                        boardState.patternIndex = boardSpec.patternIndex;
                        boardState.tripTimes = boardSpec.tripTimes;
                        boardState.parent = oldState;
                        boardState.serviceDay = boardSpec.serviceDay;
                        boardState.route = route;
                        boardState.walkDistance = oldState.walkDistance;

                        for (RaptorState state : newStates) {
                            if (eDominates(state, boardState)) {
                                continue TRYBOARD;
                            }
                        }

                        //this is unsafe because previous states have 
                        //only alighted here
                        /*
                        for (RaptorState state : states) {
                            if (state != oldState && eDominates(state, boardState)) {
                                continue TRYBOARD;
                            }
                        }
                        */
                        boardStates.add(boardState);
                    }
                }
                createdStates.addAll(newStates);
                states.addAll(newStates);
            }
        }
        System.out.println("Trips: " + trips);

        /*
         * finally, the third stage of round k considers foot- paths. For each foot-path (pi , pj )
         * ∈ F it sets τk (pj ) = min{τk (pj ), τk (pi ) + (pi , pj )}. Note that since F is
         * transitive, we always find the fastest walking path, if one exists.
         */

        ShortestPathTree spt;
        GenericDijkstra dijkstra = new GenericDijkstra(walkOptions);
        if (nBoardings == 0) {
            MaxWalkState start = new MaxWalkState(options.rctx.origin, walkOptions);
            spt = dijkstra.getShortestPathTree(start);
            // also, compute an initial spt from the target so that we can find out what transit
            // stops are nearby and what
            // the time is to them, so that we can start target bounding earlier

            RoutingRequest reversedWalkOptions = walkOptions.clone();
            reversedWalkOptions.setArriveBy(true);
            GenericDijkstra destDijkstra = new GenericDijkstra(reversedWalkOptions);
            start = new MaxWalkState(options.rctx.target, reversedWalkOptions);
            ShortestPathTree targetSpt = destDijkstra.getShortestPathTree(start);
            for (State state : targetSpt.getAllStates()) {

                final Vertex vertex = state.getVertex();

                if (!(vertex instanceof TransitStop))
                    continue;
                RaptorStop stop = data.raptorStopsForStopId.get(((TransitStop) vertex).getStopId());
                if (stop == null) {
                    // we have found a stop is totally unused, so skip it
                    continue;
                }

                cur.addStopNearTarget(stop, state.getWalkDistance(), (int) state.getElapsedTime());
            }
        } else {

            final List<MaxWalkState> startPoints = new ArrayList<MaxWalkState>();
/*
            RegionData regionData = data.regionData;
            
            List<Integer> destinationRegions = regionData.getRegionsForVertex(options.rctx.target);
            List<int[]> minTimes = new ArrayList<int[]>(2);
            //List<double[]> minWalks = new ArrayList<double[]>(2);
            for (int destinationRegion : destinationRegions) {
                minTimes.add(regionData.minTime[destinationRegion]);
            }
*/

            STARTWALK: for (RaptorState state : createdStates) {
                if (false) {
                    double maxWalk = options.getMaxWalkDistance() - state.walkDistance
                            - distanceToNearestTransitStop;
                    CHECK: for (T2<Double, RaptorStop> nearby : data.nearbyStops[state.stop.index]) {
                        double distance = nearby.getFirst();
                        RaptorStop stop = nearby.getSecond();
                        if (distance > maxWalk) {
                            // System.out.println("SKIPPED STATE: " +
                            // state.stop.stopVertex.getName());
                            // this is technically wrong because these distances are not exact
                            continue STARTWALK;
                        }
                        double minWalk = distance + state.walkDistance;
                        int minArrive = (int) (state.arrivalTime + distance
                                / options.getSpeedUpperBound());
                        if (statesByStop[stop.index] == null) {
                            break CHECK; // we have never visited this stop, and we ought to
                        }
                        for (RaptorState other : statesByStop[stop.index]) {
                            if (other.nBoardings == nBoardings - 1
                                    && (other.walkDistance > minWalk || other.arrivalTime > minArrive)) {
                                break CHECK;
                            }
                        }
                    }
                }

                // bounding states
                // this reduces the number of initial vertices
                // and the state space size

                Vertex stopVertex = state.stop.stopVertex;
                Vertex dest = options.rctx.target;

                double minWalk = distanceToNearestTransitStop;
                
                double targetDistance = cur.bounder.getTargetDistance(stopVertex);

                double minTime = (targetDistance - minWalk)
                        / MAX_TRANSIT_SPEED + minWalk
                        / options.getSpeedUpperBound();

                if (targetDistance + state.walkDistance > options.getMaxWalkDistance()) {
                    // can't walk to destination, so we can't alight at a local vertex
                    if (state.stop.stopVertex.isLocal())
                        continue;
                    //and must account for another boarding
                    minTime += boardSlack;
                }

                //this checks the precomputed table of walk distances by regions to see 
                //to get a tighter bound on the best posible walk distance to the destination
                //it (a) causes weird intermittent planner failures, (b) does not make 
                //much of a difference
                
                /*
                double minWalk = Double.MAX_VALUE;
                int index = stopVertex.getIndex();
                int fromRegion = regionData.regionForVertex[index];
                if (fromRegion == -1) {
                    minWalk = distanceToNearestTransitStop;
                    //System.out.println("unexpected missing minwalk for " + stopVertex);
                } else {
                    for (double[] byRegion : minWalks) {
                        double distanceFromThisRegion = byRegion[fromRegion];
                        if (minWalk > distanceFromThisRegion) {
                            minWalk = distanceFromThisRegion;
                        }
                    }
                }
                if (minWalk == Double.MAX_VALUE) {
                    //I don't believe you
                    System.out.println("WRONG");
                }
*/
/*
                state.arrivalTime += minTime;
                state.walkDistance += minWalk;

                for (RaptorState bound : cur.boundingStates) {
                    if (bound == state) {
                        break; //do not eliminate bounding states
                    }
                    if (eDominates(bound, state)) {
                        state.arrivalTime -= minTime;
                        state.walkDistance -= minWalk;
                        continue STARTWALK;
                    }
                }
                state.arrivalTime -= minTime;
                state.walkDistance -= minWalk;
                */
                //this bit of code was a test to see if drastically bounding the number of 
                //states explored would help; it does, but this code way overprunes.
                /*
                 * if (!cur.boundingStates.isEmpty()) { boolean found = false; OUTER: for
                 * (RaptorState bound : cur.boundingStates) { for (RaptorRoute route :
                 * routesForStop[bound.stop.index]) { for (RaptorStop stop : route.stops) { if (stop
                 * == state.stop) { found = true; break OUTER; } } } } if (!found) continue; }
                 */

                // end bounding states

                if (minWalk + state.walkDistance > options.getMaxWalkDistance()) {
                    continue;
                }

                StateEditor dijkstraState = new MaxWalkState.MaxWalkStateEditor(walkOptions,
                        stopVertex);
                dijkstraState.setNumBoardings(state.nBoardings);
                dijkstraState.setWalkDistance(state.walkDistance);
                dijkstraState.setStartTime(state.arrivalTime);
                dijkstraState.setTime(state.arrivalTime);
                dijkstraState.setExtension("raptorParent", state);
                dijkstraState.setOptions(walkOptions);
                dijkstraState.incrementWeight(state.arrivalTime - options.dateTime);
                MaxWalkState newState = (MaxWalkState) dijkstraState.makeState();
                startPoints.add(newState);
            }
            if (startPoints.size() == 0) {
                System.out.println("warning: no walk in round " + nBoardings);
                return;
            }
            System.out.println("walk starts: " + startPoints.size() + " / "
                    + cur.visitedEver.size());
            dijkstra.setPriorityQueueFactory(new PrefilledPriorityQueueFactory(startPoints.subList(
                    1, startPoints.size())));

            TargetBound bounder = cur.bounder;
            dijkstra.setShortestPathTreeFactory(bounder);
            bounder.addSptStates(startPoints.subList(1, startPoints.size()));

            dijkstra.setSearchTerminationStrategy(bounder);
            dijkstra.setSkipTraverseResultStrategy(bounder);
            dijkstra.setHeuristic(bounder);

            //Do local search
            spt = dijkstra.getShortestPathTree(startPoints.get(0));

        }

        final List<? extends State> targetStates = spt.getStates(walkOptions.rctx.target);
        if (targetStates != null) {
            TARGET: for (State targetState : targetStates) {
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
                for (RaptorState oldState : cur.getTargetStates()) {
                    if (eDominates(oldState, state)) {
                        continue TARGET;
                    }
                }
                cur.addTargetState(state);
                System.out.println("TARGET: " + state);
            }
        }
        for (State state : cur.bounder.removedBoundingStates) {
            cur.removeTargetState(state);
        }

        SPTSTATE: for (State state : spt.getAllStates()) {

            final Vertex vertex = state.getVertex();

            if (!(vertex instanceof TransitStop))
                continue;
            RaptorStop stop = data.raptorStopsForStopId.get(((TransitStop) vertex).getStopId());
            if (stop == null) {
                // we have found a stop is totally unused, so skip it
                continue;
            }

            double minWalk = distanceToNearestTransitStop;

            double targetDistance = distanceLibrary.fastDistance(
                    options.rctx.target.getCoordinate(), vertex.getCoordinate());
            double minTime = (targetDistance - minWalk) / MAX_TRANSIT_SPEED + minWalk
                    / options.getSpeedUpperBound();
            final double remainingWalk = options.maxWalkDistance - state.getWalkDistance();
            if (targetDistance > remainingWalk)
                minTime += boardSlack;

            if (cur.maxTimeDayIndex > 0 && remainingWalk < 3218) { 
                int maxTimeForVertex = 0;
                for (StopNearTarget stopNearTarget : cur.stopsNearTarget.values()) {
                    int destinationRegion = stopNearTarget.stop.stopVertex.getGroupIndex();
                    int region = vertex.getGroupIndex();
                    final int maxTimeFromThisRegion = data.maxTransitRegions.maxTransit[cur.maxTimeDayIndex][destinationRegion][region];
                    int maxTime = (int) ((state.getTime() - options.dateTime) + maxTimeFromThisRegion + stopNearTarget.time);
                    if (maxTime > maxTimeForVertex) {
                        maxTimeForVertex = maxTime;
                    }
                }
                if (maxTimeForVertex < cur.maxTime) {
                    cur.maxTime = maxTimeForVertex;
                } else {
                    if ((state.getTime() - options.dateTime) + minTime > cur.maxTime * 1.5) {
                        continue;
                    }
                }
            }

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
           
            //the following does not actually speed things up, probably 
            //because we're going to recheck it next round with more info  
/*

            // target state bounding
            for (RaptorState oldState : cur.getTargetStates()) {
                // newstate would have to take some transit and then walk to the destination.
                if (oldState.arrivalTime <= newState.arrivalTime + minTime
                        && oldState.walkDistance <= newState.walkDistance + minWalk)
                    // todo waiting time?
                    continue SPTSTATE;

                // newstate will arrive way too late (this is a hack)
                if ((oldState.arrivalTime - options.dateTime) * 3 <= (newState.arrivalTime
                        + minTime - options.dateTime))
                    continue SPTSTATE;

            }
*/
            cur.visitedLastRound.add(stop);
            cur.visitedEver.add(stop);
            states.add(newState);

        }
    }

    class PrefilledPriorityQueueFactory implements OTPPriorityQueueFactory {

        private List<? extends State> startPoints;

        public PrefilledPriorityQueueFactory(List<? extends State> startPoints) {
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
        // todo: epsilon dominance?

        return state.nBoardings <= oldState.nBoardings && state.waitingTime <= oldState.waitingTime
                && state.walkDistance <= oldState.walkDistance * 1.1
                && state.arrivalTime <= oldState.arrivalTime
        /*
         * && (state.nBoardings < oldState.nBoardings || state.waitingTime < oldState.waitingTime ||
         * state.walkDistance < oldState.walkDistance || state.arrivalTime < oldState.arrivalTime);
         */;
    }

}
