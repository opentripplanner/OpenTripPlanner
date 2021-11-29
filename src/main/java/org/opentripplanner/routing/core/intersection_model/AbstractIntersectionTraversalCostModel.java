package org.opentripplanner.routing.core.intersection_model;

import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

/**
 * Abstract turn cost model provides various methods most implementations will use.
 * 
 * @author avi
 */
public abstract class AbstractIntersectionTraversalCostModel implements
        IntersectionTraversalCostModel {

    /** Factor by which absolute turn angles are divided to get turn costs for non-driving scenarios. */
    protected double nonDrivingTurnCostFactor = 1.0 / 20.0;


    /**
     * Computes the turn cost in seconds for non-driving traversal modes.
     * 
     * TODO(flamholz): this should probably account for whether there is a traffic light?
     */
    protected double computeNonDrivingTraversalCost(IntersectionVertex v, StreetEdge from,
            StreetEdge to, float fromSpeed, float toSpeed) {
        int outAngle = to.getOutAngle();
        int inAngle = from.getInAngle();
        int turnCost = Math.abs(outAngle - inAngle);
        if (turnCost > 180) {
            turnCost = 360 - turnCost;
        }

        // NOTE: This makes the turn cost lower the faster you're going
        return (this.nonDrivingTurnCostFactor * turnCost) / toSpeed;
    }

    /**
     * Calculates the turn angle from the incoming/outgoing edges and routing request.
     * 
     * Corrects for the side of the street they are driving on.
     */
    protected int calculateTurnAngle(StreetEdge from, StreetEdge to,
            RoutingRequest options) {
        int angleOutOfIntersection = to.getInAngle();
        int angleIntoIntersection = from.getOutAngle();
        
        // Put out to the right of in; i.e. represent everything as one long right turn
        // Also ensures that turnAngle is always positive.
        if (angleOutOfIntersection < angleIntoIntersection) {
            angleOutOfIntersection += 360;
        }

        return angleOutOfIntersection - angleIntoIntersection;
    }

    /* Concrete subclasses must implement this */
    @Override
    public abstract double computeTraversalCost(IntersectionVertex v, StreetEdge from,
            StreetEdge to, TraverseMode mode, RoutingRequest options, float fromSpeed,
            float toSpeed);

}
