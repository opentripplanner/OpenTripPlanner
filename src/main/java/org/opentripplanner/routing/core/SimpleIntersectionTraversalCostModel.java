package org.opentripplanner.routing.core;

import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource.DrivingDirection;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class SimpleIntersectionTraversalCostModel extends AbstractIntersectionTraversalCostModel implements Serializable {

    public static final Logger LOG = LoggerFactory.getLogger(SimpleIntersectionTraversalCostModel.class);
    // Model parameters are here. //
    // Constants for when there is a traffic light.

    /** Expected time it takes to make a right at a light. */
    private Double expectedRightAtLightTimeSec = 15.0;

    /** Expected time it takes to continue straight at a light. */
    private Double expectedStraightAtLightTimeSec = 15.0;

    /** Expected time it takes to turn left at a light. */
    private Double expectedLeftAtLightTimeSec = 15.0;

    // Constants for when there is no traffic light

    /** Expected time it takes to make a right without a stop light. */
    private Double expectedRightNoLightTimeSec = 8.0;

    /** Expected time it takes to continue straight without a stop light. */
    private Double expectedStraightNoLightTimeSec = 5.0;

    /** Expected time it takes to turn left without a stop light. */
    private Double expectedLeftNoLightTimeSec = 8.0;

    private double safeBicycleTurnModifier = 5;

    /** Since doing a left turn on a bike is quite dangerous we add a cost for it**/
    private double acrossTrafficBicyleTurnMultiplier = safeBicycleTurnModifier * 3;

    private final DrivingDirection drivingDirection;

    public SimpleIntersectionTraversalCostModel(DrivingDirection drivingDirection) {
        this.drivingDirection = drivingDirection;
    }

    @Override
    public double computeTraversalCost(IntersectionVertex v, StreetEdge from, StreetEdge to, TraverseMode mode,
            RoutingRequest request, float fromSpeed, float toSpeed) {

        // If the vertex is free-flowing then (by definition) there is no cost to traverse it.
        if (v.inferredFreeFlowing()) {
            return 0;
        }

        if (mode.isDriving()) {
            return computeDrivingTraversalCost(v, from, to, request);
        }
        else if(mode.isCycling()) {
            var c = computeCyclingTraversalCost(v, from, to, fromSpeed, toSpeed, request);
            if(LOG.isTraceEnabled()){
                LOG.trace("Turning from {} to {} has a cost of {}", from, to, c);
            }
            return c;
        }
        else {
            return computeNonDrivingTraversalCost(v, from, to, fromSpeed, toSpeed);
        }
    }

    private double computeDrivingTraversalCost(IntersectionVertex v, StreetEdge from, StreetEdge to, RoutingRequest request) {
        double turnCost = 0;

        int turnAngle = calculateTurnAngle(from, to, request);
        if (v.trafficLight) {
            // Use constants that apply when there are stop lights.
            if (isEasyTurn(turnAngle)) {
                turnCost = expectedRightAtLightTimeSec;
            } else if (isTurnAcrossTraffic(turnAngle)) {
                turnCost = expectedLeftAtLightTimeSec;
            } else {
                turnCost = expectedStraightAtLightTimeSec;
            }
        } else {

            //assume highway vertex
            if(from.getCarSpeed()>25 && to.getCarSpeed()>25) {
                return 0;
            }

            // Use constants that apply when no stop lights.
            if (isEasyTurn(turnAngle)) {
                turnCost = expectedRightNoLightTimeSec;
            } else if (isTurnAcrossTraffic(turnAngle)) {
                turnCost = expectedLeftNoLightTimeSec;
            } else {
                turnCost = expectedStraightNoLightTimeSec;
            }
        }

        return turnCost;
    }

    private double computeCyclingTraversalCost(IntersectionVertex v, StreetEdge from,
            StreetEdge to, float fromSpeed, float toSpeed, RoutingRequest request) {
        var turnAngle = calculateTurnAngle(from, to, request);
        final var baseCost = computeNonDrivingTraversalCost(v, from, to, fromSpeed, toSpeed);

        if(isTurnAcrossTraffic(turnAngle)) {
            return baseCost * bicycleTurnAcrossTrafficMultiplier;
        } else if(isEasyTurn(turnAngle)) {
            return baseCost * biycleEasyTurnMultiplier;
            } else {
                return baseCost;
            }
        }
    }
}
