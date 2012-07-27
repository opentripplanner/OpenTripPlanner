package org.opentripplanner.routing.impl.raptor;

import java.util.ArrayList;
import java.util.List;

import org.opentripplanner.common.geometry.DistanceLibrary;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.algorithm.strategies.SearchTerminationStrategy;
import org.opentripplanner.routing.algorithm.strategies.SkipTraverseResultStrategy;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

import com.vividsolutions.jts.geom.Coordinate;

public class TargetBound implements SearchTerminationStrategy, SkipTraverseResultStrategy {

    List<State> bounders = new ArrayList<State>();

    private Vertex realTarget;

    private DistanceLibrary distanceLibrary = SphericalDistanceLibrary.getInstance();

    private Coordinate realTargetCoordinate;

    private double distanceToNearestTransitStop;

    public TargetBound(Vertex realTarget) {
        this.realTarget = realTarget;
        this.realTargetCoordinate = realTarget.getCoordinate();
        this.distanceToNearestTransitStop = realTarget.getDistanceToNearestTransitStop();
    }

    @Override
    public boolean shouldSearchContinue(Vertex origin, Vertex target, State current,
            ShortestPathTree spt, RoutingRequest traverseOptions) {
        if (current.getVertex() == realTarget) {
            bounders.add(current);
        }
        return true;
    }

    @Override
    public boolean shouldSkipTraversalResult(Vertex origin, Vertex target, State parent,
            State current, ShortestPathTree spt, RoutingRequest traverseOptions) {
        final Vertex vertex = current.getVertex();
        final double targetDistance = distanceLibrary.fastDistance(realTargetCoordinate,
                vertex.getCoordinate());

        final double remainingWalk = traverseOptions.getMaxWalkDistance()
                - current.getWalkDistance();
        final double minWalk;
        if (targetDistance > remainingWalk) {
            // then we must have some transit + some walk.
            minWalk = this.distanceToNearestTransitStop + vertex.getDistanceToNearestTransitStop();
            if (minWalk > remainingWalk)
                return true;
        } else {
            // could walk directly to destination
            minWalk = targetDistance;
        }
        final double optimisticDistance = current.getWalkDistance() + minWalk;
        final double minTime = (targetDistance - minWalk) / Raptor.MAX_TRANSIT_SPEED + minWalk
                / current.getOptions().getSpeedUpperBound();

        for (State bounder : bounders) {

            if (optimisticDistance < bounder.getWalkDistance())
                continue; //this path might win on distance

            if (bounder.getTime() < current.getTime() + minTime)
                return true; //this path won't win on either time or distance
        }
        return false;
    }

}
