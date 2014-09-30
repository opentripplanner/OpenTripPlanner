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

package org.opentripplanner.routing.core;

import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

/**
 * Abstract turn cost model provides various methods most implementations will use.
 * 
 * @author avi
 */
public abstract class AbstractIntersectionTraversalCostModel implements
        IntersectionTraversalCostModel {

    /** Factor by which absolute turn angles are divided to get turn costs for non-driving scenarios. */
    protected Double nonDrivingTurnCostFactor = 1.0 / 20.0;

    protected Integer minRightTurnAngle = 45;
    
    protected Integer maxRightTurnAngle = 135;

    protected Integer minLeftTurnAngle = 225;
    
    protected Integer maxLeftTurnAngle = 315;

    /** Returns true if this angle represents a right turn. */
    protected boolean isRightTurn(int turnAngle) {
        return (turnAngle >= minRightTurnAngle && turnAngle < maxRightTurnAngle);
    }

    /** Returns true if this angle represents a left turn. */
    protected boolean isLeftTurn(int turnAngle) {
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
