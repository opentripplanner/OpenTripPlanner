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
import java.util.Iterator;
import java.util.List;

import org.apache.commons.math3.util.FastMath;
import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.algorithm.strategies.SkipTraverseResultStrategy;
import org.opentripplanner.routing.algorithm.strategies.TransitLocalStreetService;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.graph.AbstractVertex;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ArrayMultiShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.spt.ShortestPathTreeFactory;
import org.opentripplanner.routing.vertextype.TransitStop;

import com.vividsolutions.jts.geom.Coordinate;

public class TargetBound implements SearchTerminationStrategy, SkipTraverseResultStrategy, RemainingWeightHeuristic, ShortestPathTreeFactory {

    private static final long serialVersionUID = -5296036164138922096L;

    private static final long WORST_TIME_DIFFERENCE = 3600;

    private static final double WORST_WEIGHT_DIFFERENCE_FACTOR = 1.3;

    List<State> bounders;

    private Vertex realTarget;

    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    private Coordinate realTargetCoordinate;

    private double distanceToNearestTransitStop;

    private TransitLocalStreetService transitLocalStreets;

    private double speedUpperBound;

    //this is saved so that it can be reused in various skipping functions
    private double targetDistance;

    private double speedWeight;

    /**
     * How much longer the worst path can be than the best in terms of time.
     * Setting this lower will cut off some less-walking more-time paths.
     * Setting it higher will slow down the search a lot.
     */
    private double timeBoundFactor = 2;

    private List<Integer> previousArrivalTime = new ArrayList<Integer>();

    private RoutingRequest options;

    public ShortestPathTree spt = new ArrayMultiShortestPathTree(options);

    double[] distance = new double[AbstractVertex.getMaxIndex()];

    public double bestTargetDistance = Double.POSITIVE_INFINITY;

    public List<State> removedBoundingStates = new ArrayList<State>();

    private List<State> transitStopsVisited = new ArrayList<State>();

    private double transferTimeInWalkDistance;

    public TargetBound(RoutingRequest options) {
        this.options = options;
        if (options.rctx.target != null) {
            this.realTarget = options.rctx.target;
            this.realTargetCoordinate = realTarget.getCoordinate();
            this.distanceToNearestTransitStop = realTarget.getDistanceToNearestTransitStop();
            bounders = new ArrayList<State>();
            transitLocalStreets = options.rctx.graph.getService(TransitLocalStreetService.class);
            speedUpperBound = options.getSpeedUpperBound();
            this.speedWeight = options.getWalkReluctance() / speedUpperBound;
            this.transferTimeInWalkDistance = options.getTransferSlack() / options.getWalkSpeed();
        }
    }

    @Override
    public boolean shouldSearchContinue(Vertex origin, Vertex target, State current,
            ShortestPathTree spt, RoutingRequest traverseOptions) {
        final Vertex vertex = current.getVertex();
        if (vertex instanceof TransitStop) {
            transitStopsVisited.add(current);
        }
        if (vertex == realTarget) {
            addBounder(current);
        }
        return true;
    }

    public void addBounder(State bounder) {
        for (Iterator<State> it = bounders.iterator(); it.hasNext(); ) {
            State old = it.next();

            //exact dup
            if (old.getNumBoardings() == bounder.getNumBoardings()
                && old.getTime() == bounder.getTime()
                && old.getWalkDistance() == bounder.getWalkDistance())
            return;
            if (bounder.dominates(old)) {
                it.remove();
                removedBoundingStates.add(old);
            } else if (bounder.getNumBoardings() <= old.getNumBoardings() && options.arriveBy ? (
                        bounder.getTime() - WORST_TIME_DIFFERENCE > old.getTime())
                        : (bounder.getTime() + WORST_TIME_DIFFERENCE < old.getTime())) {
                it.remove();
                removedBoundingStates.add(old);
            }
        }
        bounders.add(bounder);
        RaptorState state = (RaptorState) bounder.getExtension("raptorParent");
        if (state == null) {
            previousArrivalTime.add(-1);
            return;
        }
        RaptorStop stop = state.stop;
        //get previous alight at stop
        if (options.isArriveBy()) {
            final int nextDepartTime = getNextDepartTime(options, (state.arrivalTime - options.getBoardSlack()) - 2, stop.stopVertex);
            previousArrivalTime.add((int) ((nextDepartTime - options.getAlightSlack()) - bounder.getElapsedTime()));
        } else {
            final int previousArriveTime = getPreviousArriveTime(options, state.arrivalTime - options.getAlightSlack() + 2, stop.stopVertex);
            previousArrivalTime.add((int) (previousArriveTime + options.getAlightSlack() + bounder.getElapsedTime()));
        }
    }

    @Override
    public boolean shouldSkipTraversalResult(Vertex origin, Vertex target, State parent,
            State current, ShortestPathTree spt, RoutingRequest traverseOptions) {
        if (realTarget == null)
            return false;

        final Vertex vertex = current.getVertex();
        int vertexIndex = vertex.getIndex();
        if (vertexIndex < distance.length) {
            if (distance[vertexIndex] > 0.0) {
                targetDistance = distance[vertexIndex];
            } else {
                targetDistance = distanceLibrary.fastDistance(realTargetCoordinate.y, realTargetCoordinate.x,
                        vertex.getY(), vertex.getX());
                distance[vertexIndex] = targetDistance;
                if (vertex instanceof TransitStop && targetDistance < bestTargetDistance) {
                    bestTargetDistance = targetDistance;
                }
            }
        } else {
            targetDistance = distanceLibrary.fastDistance(realTargetCoordinate.y, realTargetCoordinate.x,
                    vertex.getY(), vertex.getX());
        }

        final double remainingWalk = traverseOptions.maxWalkDistance
                - current.getWalkDistance();
        final double minWalk;
        double minTime = 0;
        if (targetDistance > remainingWalk) {
            // then we must have some transit + some walk.
            minWalk = this.distanceToNearestTransitStop + vertex.getDistanceToNearestTransitStop();
            minTime = options.isArriveBy() ? traverseOptions.getAlightSlack() : traverseOptions.getBoardSlack();

            if (current.getBackEdge() instanceof StreetEdge && transitLocalStreets != null &&  !transitLocalStreets.transferrable(vertex)) {
                return true;
            }
        } else {
            // could walk directly to destination
            if (targetDistance < distanceToNearestTransitStop || transitLocalStreets == null || !transitLocalStreets.transferrable(vertex))
                minWalk = targetDistance;
            else
                minWalk = distanceToNearestTransitStop;
        }
        if (minWalk > remainingWalk)
            return true;

        final double optimisticDistance = current.getWalkDistance() + minWalk;
        
        final double walkTime = minWalk
                / speedUpperBound;
        minTime += (targetDistance - minWalk) / Raptor.MAX_TRANSIT_SPEED + walkTime;
        
        double stateTime = current.getOptimizedElapsedTime() + minTime;

        double walkDistance = FastMath.max(optimisticDistance * Raptor.WALK_EPSILON, optimisticDistance + transferTimeInWalkDistance);

        int i = 0;
        boolean prevBounded = !bounders.isEmpty();
        for (State bounder : bounders) {
            if (removedBoundingStates.contains(bounder))
                continue;
            if (current.getWeight() + minTime + walkTime * (options.getWalkReluctance() - 1) > bounder.getWeight() * WORST_WEIGHT_DIFFERENCE_FACTOR) {
                return true;
            }
            int prevTime = previousArrivalTime.get(i++);

            if (walkDistance > bounder.getWalkDistance()
                    && current.getNumBoardings() >= bounder.getNumBoardings()) {
                if (current.getElapsedTime() + minTime >= bounder.getElapsedTime()) {
                    return true;
                } else if (prevTime > 0 && (options.arriveBy ? (current.getTime() - minTime >= prevTime) : ((current.getTime() + minTime) <= prevTime))) {
                    prevBounded = false;
                }
            } else {
                prevBounded = false;
            }

            //check that the new path is not much longer in time than the bounding path
            if (bounder.getOptimizedElapsedTime() * timeBoundFactor < stateTime) {
                return true;
            }
        }
        return prevBounded;
    }

    public static int getNextDepartTime(RoutingRequest request, int departureTime, Vertex stopVertex) {

        int bestArrivalTime = Integer.MAX_VALUE;

        request.arriveBy = false;

        // find the boards
        for (Edge preboard : stopVertex.getOutgoing()) {
            if (preboard instanceof PreBoardEdge) {
                Vertex departure = preboard.getToVertex(); // this is the departure vertex
                for (Edge board : departure.getOutgoing()) {
                    if (board instanceof TransitBoardAlight) {
                        State state = new State(board.getFromVertex(), departureTime, request);
                        State result = board.traverse(state);
                        if (result == null)
                            continue;
                        int time = (int) result.getTime();
                        if (time < bestArrivalTime) {
                            bestArrivalTime = time;
                        }
                    }
                }
            }
        }

        request.arriveBy = true;
        return bestArrivalTime;

    }

    public static int getPreviousArriveTime(RoutingRequest request, int arrivalTime, Vertex stopVertex) {

        int bestArrivalTime = -1;

        request.arriveBy = true;

        // find the alights
        for (Edge prealight : stopVertex.getIncoming()) {
            if (prealight instanceof PreAlightEdge) {
                Vertex arrival = prealight.getFromVertex(); // this is the arrival vertex
                for (Edge alight : arrival.getIncoming()) {
                    if (alight instanceof TransitBoardAlight) {
                        State state = new State(alight.getToVertex(), arrivalTime, request);
                        State result = alight.traverse(state);
                        if (result == null)
                            continue;
                        int time = (int) result.getTime();
                        if (time > bestArrivalTime) {
                            bestArrivalTime = time;
                        }
                    }
                }
            }
        }

        request.arriveBy = false;
        return bestArrivalTime;

    }

    @Override
    public double computeInitialWeight(State s, Vertex target) {
        return computeForwardWeight(s, target);
    }


    /**
     * This actually does have to be admissible, since when we find the target, it used to bound the rest of the search.
     */
    @Override
    public double computeForwardWeight(State s, Vertex target) {
        return targetDistance * speedWeight;
    }

    @Override
    public double computeReverseWeight(State s, Vertex target) {
        return computeForwardWeight(s, target);
    }

    /** Reset the heuristic */
    @Override
    public void reset() {
    }

    public double getTimeBoundFactor() {
        return timeBoundFactor;
    }

    public void setTimeBoundFactor(double timeBoundFactor) {
        this.timeBoundFactor = timeBoundFactor;
    }

    @Override
    public ShortestPathTree create(RoutingRequest options) {
        return spt;
    }

    public void addSptStates(List<MaxWalkState> states) {
        for (MaxWalkState state : states) {
            if (state.getVertex() instanceof TransitStop) {
                transitStopsVisited.add(state);
            }
            spt.add(state);
        }
    }

    public double getTargetDistance(Vertex vertex) {
        int vertexIndex = vertex.getIndex();
        if (vertexIndex < distance.length) {
            if (distance[vertexIndex] > 0.0) {
                return distance[vertexIndex];
            } else {
                double d = distanceLibrary.fastDistance(realTargetCoordinate.y,
                        realTargetCoordinate.x, vertex.getY(), vertex.getX());
                distance[vertexIndex] = d;
                return d;
            }
        } else {
            return distanceLibrary.fastDistance(realTargetCoordinate.y, realTargetCoordinate.x,
                    vertex.getY(), vertex.getX());
        }
    }

    public List<State> getTransitStopsVisited() {
        return transitStopsVisited;
    }

    public void prepareForSearch() {
        transitStopsVisited.clear();
    }

    public void reset(RoutingRequest options) {
        this.options = options;
        if (realTarget != options.rctx.target) {
            this.realTarget = options.rctx.target;
            this.realTargetCoordinate = realTarget.getCoordinate();
            this.distanceToNearestTransitStop = realTarget.getDistanceToNearestTransitStop();
            bounders = new ArrayList<State>();
            Arrays.fill(distance, -1);
        }
        spt = new ArrayMultiShortestPathTree(options);
        transitLocalStreets = options.rctx.graph.getService(TransitLocalStreetService.class);
        speedUpperBound = options.getSpeedUpperBound();
        this.speedWeight = options.getWalkReluctance() / speedUpperBound;

    }
}
