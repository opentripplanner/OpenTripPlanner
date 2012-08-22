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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.common.pqueue.OTPPriorityQueue;
import org.opentripplanner.common.pqueue.OTPPriorityQueueFactory;
import org.opentripplanner.routing.algorithm.GenericDijkstra;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
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

        for (RaptorStop stop : visitedLastRound) {
            for (RaptorRoute route : data.routesForStop[stop.index]) {
                routesToVisit.add(route);
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
            boolean started = false;

            int firstStop, lastStop, direction, lastBoardStop;
            if (options.isArriveBy()) {
                firstStop = route.getNStops() - 1;
                lastStop = -1;
                direction = -1;
                lastBoardStop = 0;
            } else {
                firstStop = 0;
                lastStop = route.getNStops();
                direction = 1;
                lastBoardStop = lastStop - 1;
            }
            for (int stopNo = firstStop; stopNo != lastStop; stopNo += direction) {
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

                    RaptorState newState = new RaptorState(options.arriveBy);

                    ServiceDay sd = boardState.serviceDay;

                    if (options.isArriveBy()) {
                        if (!route.alights[0][boardState.patternIndex].getPattern().canBoard(stopNo))
                            continue;
                        int boardTime = route.getBoardTime(boardState.tripTimes, stopNo);
                        newState.arrivalTime = (int) sd.time(boardTime);
                        // add in slack
                        newState.arrivalTime -= options.getBoardSlack();
                    } else {
                        if (!route.boards[0][boardState.patternIndex].getPattern()
                                .canAlight(stopNo))
                            continue;
                        int alightTime = route.getAlightTime(boardState.tripTimes, stopNo);
                        newState.arrivalTime = (int) sd.time(alightTime);
                        // add in slack
                        newState.arrivalTime += options.getAlightSlack();
                    }

                    newState.boardStop = boardState.boardStop;
                    newState.boardStopSequence = boardState.boardStopSequence;
                    newState.route = route;
                    newState.patternIndex = boardState.patternIndex;
                    newState.tripTimes = boardState.tripTimes;
                    newState.nBoardings = boardState.nBoardings;
                    newState.walkDistance = boardState.walkDistance;
                    newState.parent = boardState.parent;
                    newState.stop = stop;

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

                        RaptorBoardSpec boardSpec;
                        if (options.isArriveBy()) {
                            boardSpec = route.getTripIndexReverse(options, oldState.arrivalTime
                                    - boardSlack, stopNo);
                        } else {
                            boardSpec = route.getTripIndex(options, oldState.arrivalTime
                                    + boardSlack, stopNo);
                        }
                        if (boardSpec == null)
                            continue;

                        RaptorState boardState = new RaptorState(oldState);
                        boardState.nBoardings = nBoardings;
                        boardState.boardStop = stop;
                        boardState.boardStopSequence = stopNo;
                        boardState.arrivalTime = boardSpec.departureTime;
                        boardState.patternIndex = boardSpec.patternIndex;
                        boardState.tripTimes = boardSpec.tripTimes;
                        boardState.serviceDay = boardSpec.serviceDay;
                        boardState.route = route;
                        boardState.walkDistance = oldState.walkDistance;

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

    public void walkPhase(RoutingRequest options, RoutingRequest walkOptions, int nBoardings,
            List<RaptorState> createdStates) {

        final double distanceToNearestTransitStop = options.rctx.target
                .getDistanceToNearestTransitStop();
        final int boardSlack = nBoardings == 1 ? options.getBoardSlack() : (options
                .getTransferSlack() - options.getAlightSlack());
        ShortestPathTree spt;
        GenericDijkstra dijkstra = new GenericDijkstra(walkOptions);
        List<State> transitStopStates = new ArrayList<State>();
        if (nBoardings == 0) {
            //TODO: use goal direction & bounder if within walk distance of destination
            //TODO: retry min-time bounding with this and with maxtime

            MaxWalkState start = new MaxWalkState(options.rctx.origin, walkOptions);
            spt = dijkstra.getShortestPathTree(start);
            for (State state : spt.getAllStates()) {
                if (state.getVertex() instanceof TransitStop)
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

                    addStopNearTarget(stop, state.getWalkDistance(), (int) state.getElapsedTime());
                }
            }
        } else {

            final List<MaxWalkState> startPoints = new ArrayList<MaxWalkState>();

            for (RaptorState state : createdStates) {

                // bounding states
                // this reduces the number of initial vertices
                // and the state space size

                Vertex stopVertex = state.stop.stopVertex;

                double minWalk = distanceToNearestTransitStop;

                double targetDistance = bounder.getTargetDistance(stopVertex);

                if (targetDistance + state.walkDistance > options.getMaxWalkDistance()) {
                    // can't walk to destination, so we can't alight at a local vertex
                    if (state.stop.stopVertex.isLocal())
                        continue;
                }

                if (minWalk + state.walkDistance > options.getMaxWalkDistance()) {
                    continue;
                }

                StateEditor dijkstraState = new MaxWalkState.MaxWalkStateEditor(walkOptions,
                        stopVertex);
                dijkstraState.setStartTime(options.dateTime);
                dijkstraState.setNumBoardings(state.nBoardings);
                dijkstraState.setWalkDistance(state.walkDistance);
                dijkstraState.setTime(state.arrivalTime);
                dijkstraState.setExtension("raptorParent", state);
                dijkstraState.setOptions(walkOptions);
                dijkstraState.incrementWeight(options.arriveBy ? 
                        (options.dateTime - state.arrivalTime) : (state.arrivalTime - options.dateTime));
                MaxWalkState newState = (MaxWalkState) dijkstraState.makeState();
                startPoints.add(newState);
            }
            if (startPoints.size() == 0) {
                System.out.println("warning: no walk in round " + nBoardings);
                return;
            }
            System.out.println("walk starts: " + startPoints.size() + " / " + visitedEver.size());
            dijkstra.setPriorityQueueFactory(new PrefilledPriorityQueueFactory(startPoints.subList(
                    1, startPoints.size())));

            dijkstra.setShortestPathTreeFactory(bounder);
            bounder.addSptStates(startPoints.subList(1, startPoints.size()));

            bounder.prepareForSearch();

            dijkstra.setSearchTerminationStrategy(bounder);
            dijkstra.setSkipTraverseResultStrategy(bounder);
            dijkstra.setHeuristic(bounder);

            // Do local search
            spt = dijkstra.getShortestPathTree(startPoints.get(0));
            transitStopStates = bounder.getTransitStopsVisited();
        }

        final List<? extends State> targetStates = spt.getStates(walkOptions.rctx.target);
        if (targetStates != null) {
            TARGET: for (State targetState : targetStates) {
                RaptorState parent = (RaptorState) targetState.getExtension("raptorParent");
                RaptorState state;
                if (parent != null) {
                    state = new RaptorState(parent);
                    state.nBoardings = parent.nBoardings;
                } else {
                    state = new RaptorState(options.arriveBy);
                }
                state.walkDistance = targetState.getWalkDistance();
                state.arrivalTime = (int) targetState.getTime();
                state.walkPath = targetState;
                for (RaptorState oldState : getTargetStates()) {
                    if (oldState.eDominates(state)) {
                        continue TARGET;
                    }
                }
                addTargetState(state);
                log.debug("Found target at: " + state);
            }
        }
        for (State state : bounder.removedBoundingStates) {
            removeTargetState(state);
        }

        SPTSTATE: for (State state : transitStopStates) {
            final Vertex vertex = state.getVertex();

            RaptorStop stop = data.raptorStopsForStopId.get(((TransitStop) vertex).getStopId());
            if (stop == null) {
                // we have found a stop is totally unused, so skip it
                continue;
            }


            double minWalk = distanceToNearestTransitStop;

            double targetDistance = bounder.getTargetDistance(vertex);
            final double remainingWalk = options.maxWalkDistance - state.getWalkDistance();

            if (maxTimeDayIndex > 0 && remainingWalk < 3218) {
                double minTime = (targetDistance - minWalk) / Raptor.MAX_TRANSIT_SPEED + minWalk
                        / options.getSpeedUpperBound();
                if (targetDistance > remainingWalk)
                    minTime += boardSlack;

                int maxTimeForVertex = 0;
                int region = vertex.getGroupIndex();
                final int elapsedTime = (int) state.getElapsedTime();
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

            List<RaptorState> states = statesByStop[stop.index];
            if (states == null) {
                states = new ArrayList<RaptorState>();
                statesByStop[stop.index] = states;
            }

            RaptorState baseState = (RaptorState) state.getExtension("raptorParent");
            RaptorState newState = new RaptorState(options.arriveBy);
            if (baseState != null) {
                newState.nBoardings = baseState.nBoardings;
            }
            newState.walkDistance = state.getWalkDistance();
            newState.arrivalTime = (int) state.getTime();
            newState.walkPath = state;
            newState.parent = baseState;
            newState.stop = stop;

            for (RaptorState oldState : states) {
                if (oldState.eDominates(newState)) {
                    continue SPTSTATE;
                }
            }

            visitedLastRound.add(stop);
            visitedEver.add(stop);
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

}
