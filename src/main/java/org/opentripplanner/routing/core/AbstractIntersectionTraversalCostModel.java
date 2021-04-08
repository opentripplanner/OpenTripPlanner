package org.opentripplanner.routing.core;

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

    protected int minRightTurnAngle = 45;
    
    protected int maxRightTurnAngle = 135;

    protected int minLeftTurnAngle = 225;
    
    protected int maxLeftTurnAngle = 315;

    /**
     * Returns if this angle represents an easy turn were incoming traffic does not have to be crossed.
     *
     * In right hand driving countries, this is a right turn.
     * In left hand driving countries this is a left turn.
     * */
    protected boolean isEasyTurn(int turnAngle) {
        return (turnAngle >= minRightTurnAngle && turnAngle < maxRightTurnAngle);
    }

    /**
     * Returns if this angle represents a turn across incoming traffic.
     *
     * In right hand driving countries this is a left turn.
     * In left hand driving countries this is a right turn.
     * */
    protected boolean isTurnAcrossTraffic(int turnAngle) {
        return (turnAngle >= minLeftTurnAngle && turnAngle < maxLeftTurnAngle);
    }

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

        int turnAngle = angleOutOfIntersection - angleIntoIntersection;

        if (!options.driveOnRight) {
            turnAngle = 360 - turnAngle;
        }

        return turnAngle;
    }

    /* Concrete subclasses must implement this */
    @Override
    public abstract double computeTraversalCost(IntersectionVertex v, StreetEdge from,
            StreetEdge to, TraverseMode mode, RoutingRequest options, float fromSpeed,
            float toSpeed);

}
