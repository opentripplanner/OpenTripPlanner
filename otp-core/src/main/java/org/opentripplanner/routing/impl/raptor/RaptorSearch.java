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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.common.pqueue.OTPPriorityQueue;
import org.opentripplanner.common.pqueue.OTPPriorityQueueFactory;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.OffboardVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitStopArrive;
import org.opentripplanner.routing.vertextype.TransitStopDepart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaptorSearch {
    private static final Logger log = LoggerFactory.getLogger(RaptorSearch.class);

    List<RaptorState>[] statesByStop;

    private List<RaptorState> targetStates = new ArrayList<RaptorState>();

    Set<RaptorStop> visitedEver = new HashSet<RaptorStop>();

    Set<RaptorStop> visitedLastRound = new HashSet<RaptorStop>();

    HashMap<RaptorStop, StopNearTarget> stopsNearTarget = new HashMap<RaptorStop, StopNearTarget>();

    public List<RaptorState> boundingStates = new ArrayList<RaptorState>();

    public TargetBound bounder;

    public boolean stalling = false;

    public int maxTime = Integer.MAX_VALUE;

    public int maxTimeDayIndex;

    private RaptorData data;


    @SuppressWarnings("unchecked")
    RaptorSearch(RaptorData data, RoutingRequest options) {
        statesByStop = new List[data.stops.length];
        bounder = new TargetBound(options);
        this.data = data;
    }

    public void addStates(int stop, List<RaptorState> list) {
        assert (statesByStop[stop] == null);
        statesByStop[stop] = list;
    }

    public void setStates(int stop, List<RaptorState> list) {
        statesByStop[stop] = list;
    }

    public int getNStops() {
        return statesByStop.length;
    }

    public void addTargetState(RaptorState state) {
        targetStates.add(state);
    }

    public List<RaptorState> getTargetStates() {
        return targetStates;
    }

    public void addStopNearTarget(RaptorStop stop, double walkDistance, int time) {
        stopsNearTarget.put(stop, new StopNearTarget(stop, walkDistance, time));
    }

    public void removeTargetState(State toRemove) {
        for (Iterator<RaptorState> it = targetStates.iterator(); it.hasNext();) {
            RaptorState state = it.next();
            if (state.walkPath == toRemove) {
                it.remove();
            }
        }
    }

    public List<RaptorState> transitPhase(RoutingRequest options, int nBoardings) {

        Collection<RaptorRoute> routesToVisit = new HashSet<RaptorRoute>();

        if (data.routesForStop == null) {
            Collection<RaptorRoute> routes = data.routes;
            for (RaptorStop stop : visitedLastRound) {
                for (RaptorRoute route : data.routesForStop[stop.index]) {
                    if (routes.contains(route)) {
                        routesToVisit.add(route);
                    }
                }
            }
        } else {
            for (RaptorStop stop : visitedLastRound) {
                for (RaptorRoute route : data.routesForStop[stop.index]) {
                    routesToVisit.add(route);
                }
            }
        }
        HashSet<RaptorStop> visitedThisRound = new HashSet<RaptorStop>();

        List<RaptorState> createdStates = new ArrayList<RaptorState>();

        int boardSlack;
        if (options.isArriveBy()) {
            boardSlack = nBoardings == 1 ? options.getAlightSlack()
                    : (options.getTransferSlack() - options.getBoardSlack());
        } else {
            boardSlack = nBoardings == 1 ? options.getBoardSlack()
                    : (options.getTransferSlack() - options.getAlightSlack());
        }
        for (RaptorRoute route : routesToVisit) {
            List<RaptorState> boardStates = new ArrayList<RaptorState>(); // not really states
            boolean started;

            int firstStop, lastStop, direction, lastBoardStop;
            if (options.isArriveBy()) {
                firstStop = route.getNStops() - 1;
                lastStop = -1;
                direction = -1;
                lastBoardStop = 0;
                //check for interlining on the first stop
                started = checkForInterliningArriveBy(options, nBoardings, route, boardStates);
            } else {
                firstStop = 0;
                lastStop = route.getNStops();
                direction = 1;
                lastBoardStop = lastStop - 1;
                started = checkForInterliningDepartAt(options, nBoardings, route, boardStates);
            }
            for (int stopNo = firstStop; stopNo != lastStop; stopNo += direction) {
                // find the current time at this stop
                RaptorStop stop = route.stops[stopNo];
                if (!started && !visitedLastRound.contains(stop))
                    continue;
                started = true;

                //skip stops which aren't in this set of data;
                //this is used for the rush ahead search
                if (!data.raptorStopsForStopId.containsKey(stop.stopVertex.getStopId())) {
                    continue;
                }
                
                // Skip banned stops
                if (options.getBannedStops().matches(stop.stopVertex.getStop())) {
                    continue;
                }
                if (options.getBannedStopsHard().matches(stop.stopVertex.getStop())) {
                    continue;
                }
                
                List<RaptorState> states = statesByStop[stop.index];
                List<RaptorState> newStates = new ArrayList<RaptorState>();

                if (states == null) {
                    states = new ArrayList<RaptorState>();
                    statesByStop[stop.index] = states;
                }
                // this checks the case of continuing on the current trips.
                CONTINUE: for (RaptorState boardState : boardStates) {

                    if (boardState.boardStop == stop) {
                        // this only happens due to interlines where
                        // the last stop of the first route is equal to the first stop of the
                        // subsequent route.
                        continue;
                    }

                    RaptorState newState = new RaptorState(boardState.getParent());

                    ServiceDay sd = boardState.serviceDay;

                    int travelTime;
                    if (options.isArriveBy()) {
                        if (!route.alights[0][boardState.patternIndex].getPattern().canBoard(stopNo))
                            continue;
                        int boardTime = route.getBoardTime(boardState.tripTimes, stopNo);
                        newState.arrivalTime = (int) sd.time(boardTime);
                        // add in slack
                        newState.arrivalTime -= options.getBoardSlack();
                        travelTime = newState.getParent().arrivalTime - newState.arrivalTime;
                    } else {
                        if (!route.boards[0][boardState.patternIndex].getPattern()
                                .canAlight(stopNo))
                            continue;
                        int alightTime = route.getAlightTime(boardState.tripTimes, stopNo);
                        newState.arrivalTime = (int) sd.time(alightTime);
                        // add in slack
                        newState.arrivalTime += options.getAlightSlack();
                        travelTime = newState.arrivalTime - newState.getParent().arrivalTime;
                    }

                    newState.weight += travelTime;

                    //TODO: consider transfer penalties
                    newState.weight += boardState.weight;
                    newState.boardStop = boardState.boardStop;
                    newState.boardStopSequence = boardState.boardStopSequence;
                    newState.route = route;
                    newState.patternIndex = boardState.patternIndex;
                    newState.tripTimes = boardState.tripTimes;
                    newState.nBoardings = boardState.nBoardings;
                    newState.walkDistance = boardState.walkDistance;
                    newState.tripId = boardState.tripId;
                    newState.stop = stop;
                    newState.serviceDay = boardState.serviceDay;

                    for (RaptorState oldState : states) {
                        if (oldState.eDominates(newState)) {
                            continue CONTINUE;
                        }
                    }

                    for (RaptorState oldState : newStates) {
                        if (oldState.eDominates(newState)) {
                            continue CONTINUE;
                        }
                    }

                    Iterator<RaptorState> it = states.iterator();
                    while (it.hasNext()) {
                        RaptorState oldState = it.next();
                        if (newState.eDominates(oldState)) {
                            it.remove();
                        }
                    }

                    it = newStates.iterator();
                    while (it.hasNext()) {
                        RaptorState oldState = it.next();
                        if (newState.eDominates(oldState)) {
                            it.remove();
                        }
                    }

                    visitedThisRound.add(stop);
                    visitedEver.add(stop);
                    newStates.add(newState);
                }

                if (stopNo != lastBoardStop) {

                    // try boarding here
                    TRYBOARD: for (RaptorState oldState : states) {
                        if (oldState.nBoardings != nBoardings - 1)
                            continue;
                        if (oldState.getRoute() == route)
                            continue; // we got here via this route, so no reason to transfer

                        RaptorBoardSpec boardSpec;
                        int waitTime;
                        if (options.isArriveBy()) {
                            int arrivalTime = oldState.arrivalTime - boardSlack;
                            boardSpec = route.getTripIndexReverse(options, arrivalTime, stopNo);
                            if (boardSpec == null)
                                continue;
                            waitTime = oldState.arrivalTime - boardSpec.departureTime;
                        } else {
                            int arrivalTime = oldState.arrivalTime + boardSlack;
                            boardSpec = route.getTripIndex(options, arrivalTime, stopNo);
                            if (boardSpec == null)
                                continue;
                            waitTime = boardSpec.departureTime - oldState.arrivalTime;
                        }

                        RaptorState boardState = new RaptorState(oldState);
                        if (nBoardings == 1) {
                            //do not count initial wait time, since it will be optimized away later
                            boardState.initialWaitTime = waitTime;
                            waitTime = 0;
                        }

                        boardState.weight = options.getBoardCost(route.mode) + waitTime;
                        boardState.nBoardings = nBoardings;
                        boardState.boardStop = stop;
                        boardState.boardStopSequence = stopNo;
                        boardState.arrivalTime = boardSpec.departureTime;
                        boardState.patternIndex = boardSpec.patternIndex;
                        boardState.tripTimes = boardSpec.tripTimes;
                        boardState.serviceDay = boardSpec.serviceDay;
                        boardState.route = route;
                        boardState.walkDistance = oldState.walkDistance;
                        boardState.tripId = boardSpec.tripId;

                        for (RaptorState state : boardStates) {
                            if (state.eDominates(boardState)) {
                                continue TRYBOARD;
                            }
                        }

                        for (RaptorState state : newStates) {
                            if (state.eDominates(boardState)) {
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
        visitedLastRound = visitedThisRound;
        return createdStates;
    }

    private boolean checkForInterliningDepartAt(RoutingRequest options, int nBoardings,
            RaptorRoute route, List<RaptorState> boardStates) {
        int firstStop = 0;
        boolean started = false;
        final List<RaptorState> oldStates = statesByStop[route.stops[firstStop].index];
        if (oldStates == null)
            return false;
        INTERLINE:for (RaptorState oldState : oldStates) {
            if (oldState.nBoardings != nBoardings - 1) {
                continue;
            }
            if (oldState.route == null) {
                continue;
            }
            if (oldState.route.stops[oldState.route.getNStops() - 1] != oldState.stop) {
                continue;
            }
            RaptorInterlineData interline = oldState.route.interlinesOut.get(oldState.tripId);
            if (interline == null || interline.toRoute != route) {
                continue;
            }
            RaptorState stayOn = oldState.clone();
            stayOn.arrivalTime -= options.getAlightSlack(); // go backwards in time to erase unnecessary slack
            stayOn.interlining = true;

            // generate a board state for this interline
            RaptorState boardState = new RaptorState(stayOn);
            //we need to subtract out the slacks that we are about to mistakenly pay
            boardState.weight -= options.getBoardSlack() - options.getAlightSlack();
            boardState.nBoardings = nBoardings - 1;
            boardState.boardStop = route.stops[firstStop];
            boardState.boardStopSequence = firstStop;

            TransitBoardAlight board = route.boards[firstStop][interline.toPatternIndex];
            TableTripPattern pattern = board.getPattern();

            boardState.tripTimes = pattern.getTripTimes(interline.toTripIndex);
            final ServiceDay serviceDay = oldState.serviceDay;
            boardState.arrivalTime = (int) serviceDay.time(boardState.tripTimes.getDepartureTime(firstStop));
            boardState.patternIndex = interline.toPatternIndex;
            boardState.tripId = interline.toTripId;

            boardState.serviceDay = serviceDay;
            boardState.route = route;
            boardState.walkDistance = oldState.walkDistance;

            for (RaptorState state : boardStates) {
                if (state.eDominates(boardState)) {
                    continue INTERLINE;
                }
            }

            boardStates.add(boardState);
            started = true;

        }
        return started;
    }

    private boolean checkForInterliningArriveBy(RoutingRequest options, int nBoardings,
            RaptorRoute route, List<RaptorState> boardStates) {
        int firstStop = route.getNStops() - 1;
        boolean started = false;
        final List<RaptorState> states = statesByStop[route.stops[firstStop].index];
        if (states == null)
            return false;
        INTERLINE: for (RaptorState oldState : states) {
            if (oldState.nBoardings != nBoardings - 1) {
                continue;
            }
            if (oldState.route == null) {
                continue;
            }
            if (oldState.route.stops[0] != oldState.stop) {
                continue;
            }
            RaptorInterlineData interline = oldState.route.interlinesIn.get(oldState.tripId);
            if (interline == null || interline.fromRoute != route) {
                continue;
            }

            RaptorState stayOn = oldState.clone();
            stayOn.arrivalTime += options.getBoardSlack(); // go backwards in time
            stayOn.interlining = true;

            // generate a board state for this interline
            RaptorState boardState = new RaptorState(stayOn);
            //we need to subtract out the boardSlack that we are about to mistakenly pay
            boardState.weight = -options.getBoardSlack() - options.getAlightSlack();
            boardState.nBoardings = boardState.nBoardings = nBoardings - 1;
            boardState.boardStop = route.stops[firstStop];
            boardState.boardStopSequence = firstStop;

            TransitBoardAlight alight = route.alights[firstStop - 1][interline.fromPatternIndex];
            TableTripPattern pattern = alight.getPattern();

            boardState.tripTimes = pattern.getTripTimes(interline.fromTripIndex);
            final ServiceDay serviceDay = oldState.serviceDay;
            boardState.arrivalTime = (int) serviceDay.time(boardState.tripTimes
                    .getArrivalTime(firstStop - 1));
            boardState.patternIndex = interline.fromPatternIndex;
            boardState.tripId = interline.fromTripId;

            boardState.serviceDay = serviceDay;
            boardState.route = route;
            boardState.walkDistance = oldState.walkDistance;

            for (RaptorState state : boardStates) {
                if (state.eDominates(boardState)) {
                    continue INTERLINE;
                }
            }

            boardStates.add(boardState);
            started = true;
        }

        return started;
    }

    /**
     * @param options
     * @param walkOptions
     * @param nBoardings
     * @param createdStates
     * @return whether search should continue
     */
    public boolean walkPhase(RoutingRequest options, RoutingRequest walkOptions, int nBoardings,
            List<RaptorState> createdStates) {

        double distanceToNearestTransitStop = 0;
        if (options.rctx.target != null) {
            distanceToNearestTransitStop = options.rctx.target.getDistanceToNearestTransitStop();
        }

        final int boardSlack = nBoardings == 1 ? options.getBoardSlack() : (options
                .getTransferSlack() - options.getAlightSlack());
        ShortestPathTree spt;
        GenericDijkstra dijkstra = new GenericDijkstra(walkOptions);
        dijkstra.setShortestPathTreeFactory(bounder);
        List<State> transitStopStates = new ArrayList<State>();

        if (nBoardings == 0) {
            //TODO: retry min-time bounding with this and with maxtime

            if (options.rctx.target != null
                    && bounder.getTargetDistance(options.rctx.origin) < options
                            .getMaxWalkDistance())
                dijkstra.setHeuristic(bounder);

            MaxWalkState start = new MaxWalkState(options.rctx.origin, walkOptions);
            spt = dijkstra.getShortestPathTree(start);
            for (State state : spt.getAllStates()) {
                if (state.getVertex() instanceof TransitStop || state.getVertex() instanceof TransitStopArrive || state.getVertex() instanceof TransitStopDepart)
                    transitStopStates.add(state);
            }
            // also, compute an initial spt from the target so that we can find out what transit
            // stops are nearby and what
            // the time is to them, so that we can start target bounding earlier
            if (maxTimeDayIndex > 0) {
                RoutingRequest reversedWalkOptions = walkOptions.clone();
                reversedWalkOptions.setArriveBy(!walkOptions.isArriveBy());
                GenericDijkstra destDijkstra = new GenericDijkstra(reversedWalkOptions);
                start = new MaxWalkState(options.rctx.target, reversedWalkOptions);
                ShortestPathTree targetSpt = destDijkstra.getShortestPathTree(start);
                for (State state : targetSpt.getAllStates()) {

                    final Vertex vertex = state.getVertex();

                    if (!(vertex instanceof TransitStop))
                        continue;
                    RaptorStop stop = data.raptorStopsForStopId.get(((TransitStop) vertex)
                            .getStopId());
                    if (stop == null) {
                        // we have found a stop is totally unused, so skip it
                        continue;
                    }

                    // Skip banned stops
                    if (options.getBannedStops().matches(stop.stopVertex.getStop())) {
                        continue;
                    }
                    if (options.getBannedStopsHard().matches(stop.stopVertex.getStop())) {
                        continue;
                    }

                    addStopNearTarget(stop, state.getWalkDistance(), (int) state.getElapsedTimeSeconds());
                }
            }
        } else {

            final List<MaxWalkState> startPoints = new ArrayList<MaxWalkState>();

            for (RaptorState state : createdStates) {

                // bounding states
                // this reduces the number of initial vertices
                // and the state space size

                Vertex stopVertex = options.isArriveBy() ? state.stop.departVertex : state.stop.arriveVertex;
                if (stopVertex == null) {
                    stopVertex = state.stop.stopVertex;
                }

                if (options.rctx.target != null) {
                    double minWalk = distanceToNearestTransitStop;

                    if (minWalk + state.walkDistance > options.getMaxWalkDistance()) {
                        continue;
                    }
                }

                StateEditor dijkstraState = new MaxWalkState.MaxWalkStateEditor(walkOptions,
                        stopVertex);
                dijkstraState.setInitialWaitTimeSeconds(state.initialWaitTime);
                dijkstraState.setStartTimeSeconds(options.dateTime);
                dijkstraState.setNumBoardings(state.nBoardings);
                dijkstraState.setWalkDistance(state.walkDistance);
                dijkstraState.setTimeSeconds(state.arrivalTime);
                dijkstraState.setExtension("raptorParent", state);
                dijkstraState.setOptions(walkOptions);
                dijkstraState.incrementWeight(state.weight);
                MaxWalkState newState = (MaxWalkState) dijkstraState.makeState();
                startPoints.add(newState);
            }
            if (startPoints.size() == 0) {
                return false;
            }
            System.out.println("walk starts: " + startPoints.size() + " / " + visitedEver.size());
            dijkstra.setPriorityQueueFactory(new PrefilledPriorityQueueFactory(startPoints.subList(
                    1, startPoints.size())));

            bounder.addSptStates(startPoints.subList(1, startPoints.size()));

            bounder.prepareForSearch();

            dijkstra.setSearchTerminationStrategy(bounder);
            if (options.rctx.target != null) {
                dijkstra.setSkipTraverseResultStrategy(bounder);
                dijkstra.setHeuristic(bounder);
            }

            // Do local search
            spt = dijkstra.getShortestPathTree(startPoints.get(0));
            transitStopStates = bounder.getTransitStopsVisited();
        }

        List<? extends State> targetStates = null;
        if (walkOptions.rctx.target != null)
            targetStates = spt.getStates(walkOptions.rctx.target);
        if (targetStates != null) {
            TARGET: for (State targetState : targetStates) {
                RaptorState parent = (RaptorState) targetState.getExtension("raptorParent");
                RaptorState state;
                if (parent != null) {
                    state = new RaptorState(parent);
                    state.nBoardings = parent.nBoardings;
                    state.rentingBike = targetState.isBikeRenting();
                } else {
                    state = new RaptorState(options);
                }
                state.weight = targetState.getWeight();
                state.walkDistance = targetState.getWalkDistance();
                state.arrivalTime = (int) targetState.getTimeSeconds();
                state.walkPath = targetState;
                for (Iterator<RaptorState> it = getTargetStates().iterator(); it.hasNext();) {
                    RaptorState oldState = it.next();
                    if (oldState.eDominates(state)) {
                        continue TARGET;
                    } else if (state.eDominates(oldState)) {
                        it.remove();
                    }
                }
                addTargetState(state);
                log.debug("Found target at: " + state + " on " + state.getTrips());
            }
        }
        for (State state : bounder.removedBoundingStates) {
            removeTargetState(state);
        }

        SPTSTATE: for (State state : transitStopStates) {
            final Vertex vertex = state.getVertex();

            RaptorStop stop = data.raptorStopsForStopId.get(((OffboardVertex) vertex).getStopId());
            if (stop == null) {
                // we have found a stop is totally unused, so skip it
                continue;
            }

            // Skip banned stops
            if (options.getBannedStops().matches(stop.stopVertex.getStop())) {
                continue;
            }
            if (options.getBannedStopsHard().matches(stop.stopVertex.getStop())) {
                continue;
            }
            
            if (options.rctx.target != null) {
                double minWalk = distanceToNearestTransitStop;

                double targetDistance = bounder.getTargetDistance(vertex);
                final double remainingWalk = options.maxWalkDistance - state.getWalkDistance();

                if (maxTimeDayIndex > 0 && remainingWalk < 3218) {
                    double minTime = (targetDistance - minWalk) / Raptor.MAX_TRANSIT_SPEED
                            + minWalk / options.getStreetSpeedUpperBound();
                    if (targetDistance > remainingWalk)
                        minTime += boardSlack;

                    int maxTimeForVertex = 0;
                    int region = vertex.getGroupIndex();
                    final int elapsedTime = (int) state.getElapsedTimeSeconds();
                    for (StopNearTarget stopNearTarget : stopsNearTarget.values()) {
                        int destinationRegion = stopNearTarget.stop.stopVertex.getGroupIndex();
                        final int maxTimeFromThisRegion = data.maxTransitRegions.maxTransit[maxTimeDayIndex][destinationRegion][region];
                        int maxTime = elapsedTime + maxTimeFromThisRegion + stopNearTarget.time;

                        if (maxTime > maxTimeForVertex) {
                            maxTimeForVertex = maxTime;
                        }
                    }
                    if (maxTimeForVertex < maxTime) {
                        maxTime = maxTimeForVertex;
                    } else {
                        if (elapsedTime + minTime > maxTime * 1.5) {
                            continue;
                        }
                    }
                }
            }
            List<RaptorState> states = statesByStop[stop.index];
            if (states == null) {
                states = new ArrayList<RaptorState>();
                statesByStop[stop.index] = states;
            }

            RaptorState parent = (RaptorState) state.getExtension("raptorParent");
            RaptorState newState;
            if (parent != null) {
                newState = new RaptorState(parent);
            } else {
                //this only happens in round 0
                newState = new RaptorState(options);
            }
            newState.weight = state.getWeight();
            newState.nBoardings = nBoardings;
            newState.walkDistance = state.getWalkDistance();
            newState.arrivalTime = (int) state.getTimeSeconds();
            newState.walkPath = state;
            newState.stop = stop;
            newState.rentingBike = state.isBikeRenting();

            for (RaptorState oldState : states) {
                if (oldState.eDominates(newState)) {
                    continue SPTSTATE;
                }
            }

            visitedLastRound.add(stop);
            visitedEver.add(stop);
            states.add(newState);

        }
        return true;
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
                heap.insert(state, state.getWeight());
            }
            return heap;
        }

    }

    public void reset(RoutingRequest options) {
        bounder.reset(options);
        Arrays.fill(statesByStop, null);
    }

}
