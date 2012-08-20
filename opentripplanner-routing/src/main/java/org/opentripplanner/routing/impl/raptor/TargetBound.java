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
import java.util.Iterator;
import java.util.List;

import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.algorithm.strategies.SkipTraverseResultStrategy;
import org.opentripplanner.routing.algorithm.strategies.TransitLocalStreetService;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
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

    List<State> bounders;

    private Vertex realTarget;

    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    private Coordinate realTargetCoordinate;

    private double distanceToNearestTransitStop;

    private TransitLocalStreetService transitLocalStreets;

    private double speedUpperBound;

    private List<int[]> minTimesNearEnd;

    //this is saved so that it can be reused in various skipping functions
    private double targetDistance;

    private double speedWeight;

    /**
     * How much longer the worst path can be than the best in terms of time.
     * Setting this lower will cut off some less-walking more-time paths.
     * Setting it higher will slow down the search a lot.
     */
    private double timeBoundFactor = 1.5;

    private List<Integer> previousArrivalTime = new ArrayList<Integer>();

    private RoutingRequest options;

    public ShortestPathTree spt = new ArrayMultiShortestPathTree(options);

    //private List<RaptorState> boundingStates;

    double[] distance = new double[AbstractVertex.getMaxIndex()];

    public double bestTargetDistance = Double.POSITIVE_INFINITY;

    public List<State> removedBoundingStates = new ArrayList<State>();

    private List<State> transitStopsVisited = new ArrayList<State>();

    public TargetBound(RoutingRequest options) {
        this.options = options;
        this.realTarget = options.rctx.target;
        this.realTargetCoordinate = realTarget.getCoordinate();
        this.distanceToNearestTransitStop = realTarget.getDistanceToNearestTransitStop();
        bounders = new ArrayList<State>();
        transitLocalStreets = options.rctx.graph.getService(TransitLocalStreetService.class);
        speedUpperBound = options.getSpeedUpperBound();
        this.speedWeight = options.getWalkReluctance() / speedUpperBound;
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

    private void addBounder(State bounder) {
        for (Iterator<State> it = bounders.iterator(); it.hasNext(); ) {
            State old = it.next();
            if (bounder.dominates(old)) {
                it.remove();
                removedBoundingStates.add(old);
            } else if (bounder.getNumBoardings() <= old.getNumBoardings()
                    && bounder.getTime() + WORST_TIME_DIFFERENCE < old.getTime()) {
                it.remove();
                removedBoundingStates.add(old);
            }
        }
        bounders.add(bounder);
        RaptorState state = (RaptorState) bounder.getExtension("raptorParent");
        RaptorStop stop = state.stop;
        //get previous alight at stop
        final int previousArriveTime = getPreviousArriveTime(options, state.arrivalTime - options.getAlightSlack() - 2, stop.stopVertex);
        previousArrivalTime.add((int) (previousArriveTime + options.getAlightSlack() + bounder.getElapsedTime()));
    }

    @Override
    public boolean shouldSkipTraversalResult(Vertex origin, Vertex target, State parent,
            State current, ShortestPathTree spt, RoutingRequest traverseOptions) {
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
            minTime = traverseOptions.getBoardSlack();

            if (current.getBackEdge() instanceof StreetEdge && !transitLocalStreets.transferrable(vertex)) {
                return true;
            }
        } else {
            // could walk directly to destination
            if (targetDistance < distanceToNearestTransitStop || !transitLocalStreets.transferrable(vertex))
                minWalk = targetDistance;
            else
                minWalk = distanceToNearestTransitStop;
        }
        if (minWalk > remainingWalk)
            return true;

        final double optimisticDistance = current.getWalkDistance() + minWalk;
        
        minTime += (targetDistance - minWalk) / Raptor.MAX_TRANSIT_SPEED + minWalk
                / speedUpperBound;
        
        //oh well, it was worth a try
/*
        int index = vertex.getIndex();
        if (index < AbstractVertex.getMaxIndex()) {
            int region = vertex.getGroupIndex();
            double minPrecomputedTime = Double.POSITIVE_INFINITY;
            if (region != -1) {
                for (int[] minTimes : minTimesNearEnd) {
                    int regionMinTime = minTimes[region];
                    if (regionMinTime < minPrecomputedTime)
                        minPrecomputedTime = regionMinTime;
                }
                if (minPrecomputedTime > minTime)
                    minTime = minPrecomputedTime;
            }
        }
*/
        double stateTime = current.getTime() + minTime - traverseOptions.dateTime;
        
        // this makes speed worse for some reason. I have no idea why.
/*
        for (RaptorState bounder : boundingStates) {
            if (optimisticDistance > bounder.walkDistance && current.getTime() + minTime > bounder.arrivalTime)
                return true;
                
                double bounderTime = bounder.arrivalTime - traverseOptions.dateTime;
                if (bounderTime * 1.5 < stateTime) {
                    return true;
                }
        }
*/

        int i = 0;
        boolean prevBounded = !bounders.isEmpty();
        for (State bounder : bounders) {
            int prevTime = previousArrivalTime.get(i++);
            
            if (optimisticDistance * 1.1 > bounder.getWalkDistance()
                    && current.getNumBoardings() >= bounder.getNumBoardings()) {
                if (current.getTime() + minTime > bounder.getTime()) {
                    return true;
                } else if (current.getTime() + minTime <= prevTime) {
                    prevBounded = false;
                }
            } else {
                prevBounded = false;
            }

            //check that the new path is not much longer in time than the bounding path
            double bounderTime = bounder.getTime() - traverseOptions.dateTime;

            if (bounderTime * timeBoundFactor < stateTime) {
                return true;
            }
        }
        return prevBounded;
    }

    public static int getPreviousArriveTime(RoutingRequest request, int arrivalTime, Vertex stopVertex) {

        int bestArrivalTime = -1;

        request.arriveBy = true;

        // find the alights
        for (Edge prealight : stopVertex.getIncoming()) {
            if (prealight instanceof PreAlightEdge) {
                Vertex arrival = prealight.getFromVertex(); // this is the arrival vertex
                for (Edge alight : arrival.getIncoming()) {
                    if (alight instanceof PatternAlight) {
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
}
