package org.opentripplanner.routing.algorithm;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.EdgeNarrative;
import org.opentripplanner.routing.core.OptimizeType;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.TraverseResult;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.OnBoardForwardEdge;
import org.opentripplanner.routing.edgetype.OnBoardReverseEdge;
import org.opentripplanner.routing.spt.SPTVertex;

public class DefaultRemainingWeightHeuristic implements RemainingWeightHeuristic {

    private TraverseOptions options;

    private boolean useTransit = false;

    private double maxSpeed;

    @Override
    public double computeInitialWeight(Vertex origin, Vertex target, TraverseOptions traverseOptions) {
        
        this.options = traverseOptions;
        this.useTransit = traverseOptions.getModes().getTransit();
        this.maxSpeed = getMaxSpeed(options);
        
        return origin.distance(target) / maxSpeed;
    }

    @Override
    public double computeForwardWeight(SPTVertex from, Edge edge, TraverseResult traverseResult,
            Vertex target) {

        EdgeNarrative narrative = traverseResult.getEdgeNarrative();
        Vertex tov = narrative.getToVertex();

        double euclidianDistance = tov.distance(target);

        if (useTransit) {
            if (from.state.alightedLocal) {
                return options.walkReluctance * euclidianDistance / options.speed;
            } else {
                int boardCost;
                if (edge instanceof OnBoardForwardEdge) {
                    boardCost = options.boardCost;
                } else {
                    boardCost = 0;
                }

                if (euclidianDistance < target.getDistanceToNearestTransitStop()) {
                    return options.walkReluctance * euclidianDistance / options.speed;
                } else {
                    double mandatoryWalkDistance = target.getDistanceToNearestTransitStop()
                            + tov.getDistanceToNearestTransitStop();
                    double distance = (euclidianDistance - mandatoryWalkDistance) / maxSpeed
                            + mandatoryWalkDistance * options.walkReluctance / options.speed
                            + boardCost;
                    return Math.min(distance, options.walkReluctance * euclidianDistance
                            / options.speed);
                }
            }
        }
        else {
            // This was missing from the original AStar, but it only makes sense
            return options.walkReluctance * euclidianDistance / maxSpeed;
        }
    }

    @Override
    public double computeReverseWeight(SPTVertex from, Edge edge, TraverseResult traverseResult,
            Vertex target) {
        
        EdgeNarrative narrative = traverseResult.getEdgeNarrative();
        Vertex fromv = narrative.getFromVertex();

        double euclidianDistance = fromv.distance(target);
        
        if (useTransit) {
            if (from.state.alightedLocal) {
                return options.walkReluctance * euclidianDistance / options.speed;
            } else {
                int boardCost;
                if (edge instanceof OnBoardReverseEdge) {
                    boardCost = options.boardCost;
                } else {
                    boardCost = 0;
                }

                if (euclidianDistance < target.getDistanceToNearestTransitStop()) {
                    return options.walkReluctance * euclidianDistance
                            / options.speed;
                } else {
                    double mandatoryWalkDistance = target
                            .getDistanceToNearestTransitStop()
                            + fromv.getDistanceToNearestTransitStop();
                    double distance = (euclidianDistance - mandatoryWalkDistance) / maxSpeed
                            + mandatoryWalkDistance * options.walkReluctance
                            / options.speed + boardCost;
                    return distance = Math.min(distance, options.walkReluctance
                            * euclidianDistance / options.speed);
                }
            }
        } else {
            return options.walkReluctance * euclidianDistance / maxSpeed;
        }
    }
    

    public static double getMaxSpeed(TraverseOptions options) {
        if (options.getModes().contains(TraverseMode.TRANSIT)) {
            // assume that the max average transit speed over a hop is 10 m/s, which is so far true
            // for
            // New York and Portland
            return 10;
        } else {
            if (options.optimizeFor == OptimizeType.QUICK) {
                return options.speed;
            } else {
                // assume that the best route is no more than 10 times better than
                // the as-the-crow-flies flat base route.
                return options.speed * 10;
            }
        }
    }
}
