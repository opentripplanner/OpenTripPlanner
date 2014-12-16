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

import java.io.Serializable;

import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

public class SimpleIntersectionTraversalCostModel extends AbstractIntersectionTraversalCostModel implements Serializable {

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

    @Override
    public double computeTraversalCost(IntersectionVertex v, StreetEdge from, StreetEdge to, TraverseMode mode,
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

        int turnAngle = calculateTurnAngle(from, to, options);
        if (v.trafficLight) {
            // Use constants that apply when there are stop lights.
            if (isRightTurn(turnAngle)) {
                turnCost = expectedRightAtLightTimeSec;
            } else if (isLeftTurn(turnAngle)) {
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
            if (isRightTurn(turnAngle)) {
                turnCost = expectedRightNoLightTimeSec;
            } else if (isLeftTurn(turnAngle)) {
                turnCost = expectedLeftNoLightTimeSec;
            } else {
                turnCost = expectedStraightNoLightTimeSec;
            }
        }

        return turnCost;
    }

}
