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

import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

import lombok.Setter;

public class SimpleIntersectionTraversalCostModel extends AbstractIntersectionTraversalCostModel {

    // Model parameters are here. //
    // Constants for when there is a traffic light.
    
    /** Probability that the left turn is active if there is a light. */
    @Setter
    private Double leftTurnActiveProb = 1.0 / 6.0;

    /** Probability that the right turn is active if there is a light. */
    @Setter
    private Double rightTurnActiveProb = 1.0 / 3.0;

    /** Probability you can continue straight ahead if there is a light. */
    @Setter
    private Double continueStraightActiveProb = 1.0 / 3.0;

    /**
     * Expected time it takes to make a right at a light.
     * 
     * NOTE(flamholz): default seems to be based on the assumption that there are rights on red?
     */
    @Setter
    private Double expectedRightAtLightTimeSec = 15.0;
    
    /** Expected time it takes to continue straight at a light. */
    @Setter
    private Double expectedStraightAtLightTimeSec = 26.2666666667;
    
    /** Expected time it takes to turn left at a light. */
    @Setter
    private Double expectedLeftAtLightTimeSec = 41.666666667;
    
    // Constants for when there is no traffic light
    
    /** Probability that you stop to turn if there is no light. */
    @Setter
    private Double noLightStopToTurnProb = 1.0;

    /**
     * Probability that you stop to continue straight if there is no light.
     * 
     * Default based on the assumption that most intersections have stop signs.
     */
    @Setter
    private Double noLightStopToContinueProb = 0.75;
    
    /** Expected time it takes to make a right without a stop light. */
    @Setter
    private Double expectedRightNoLightTimeSec = 10.0;
    
    /** 
     * Expected time it takes to continue straight without a stop light.
     *
     * LOS B: http://en.wikipedia.org/wiki/Level_of_Service#LOS_for_At-Grade_Intersections
     */
    @Setter
    private Double expectedStraightNoLightTimeSec = 12.0;
    
    /** Expected time it takes to turn left without a stop light. */
    @Setter
    private Double expectedLeftNoLightTimeSec = 15.0;
    
    @Override
    public double computeTraversalCost(IntersectionVertex v, PlainStreetEdge from, PlainStreetEdge to, TraverseMode mode,
            RoutingRequest options, float fromSpeed, float toSpeed) {
        // If the vertex is free-flowing then (by definition) there is no cost to traverse it.
        if (v.inferredFreeFlowing()) {
            return 0;
        }
        
        // Non-driving cases are much simpler. Handled generically in the base class.
        if (!mode.isDriving()) {
            return computeNonDrivingTraversalCost(v, from, to, fromSpeed, toSpeed);
        }

        double turnCost = 0;
        double probabilityStopToTurn = 0;
        
        int turnAngle = calculateTurnAngle(from, to, options);
        if (v.isTrafficLight()) {
            // Use constants that apply when there are stop lights.
            if (isRightTurn(turnAngle)) {
                turnCost = expectedRightAtLightTimeSec;
                probabilityStopToTurn = 1.0 - rightTurnActiveProb;
            } else if (isLeftTurn(turnAngle)) {
                turnCost = expectedLeftAtLightTimeSec;
                probabilityStopToTurn = 1.0 - leftTurnActiveProb;
            } else {
                turnCost = expectedStraightAtLightTimeSec;
                probabilityStopToTurn = 1.0 - continueStraightActiveProb;
            }
        } else {
            // Use constants that apply when no stop lights.
            if (isRightTurn(turnAngle)) {
                turnCost = expectedRightNoLightTimeSec;
                probabilityStopToTurn = noLightStopToTurnProb;
            } else if (isLeftTurn(turnAngle)) {
                turnCost = expectedLeftNoLightTimeSec;
                probabilityStopToTurn = noLightStopToTurnProb;
            } else {
                turnCost = expectedStraightNoLightTimeSec;
                probabilityStopToTurn = noLightStopToContinueProb;
            }
        }
        
        // Note that both acceleration and deceleration are multipled by 0.5, because half
        // of the acceleration/deceleration time has already been accounted for in the base
        // time calculations (this requires some algebra, but is correct).

        // Calculate deceleration by multiplying the time for deceleration by the probability
        // of stopping (expected value)
        double decelerationTime = fromSpeed / options.carDecelerationSpeed;
        turnCost += decelerationTime * 0.5 * probabilityStopToTurn;

        // Calculate acceleration the same way
        double accelerationTime = (toSpeed / options.carAccelerationSpeed);
        turnCost += accelerationTime * 0.5 * probabilityStopToTurn;
        
        return turnCost;
    }

}
