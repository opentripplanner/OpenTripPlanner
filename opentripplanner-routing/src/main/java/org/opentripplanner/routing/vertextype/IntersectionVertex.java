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

package org.opentripplanner.routing.vertextype;

import lombok.Getter;
import lombok.Setter;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.graph.Graph;

/** Represents an ordinary location in space, typically an intersection */
public class IntersectionVertex extends StreetVertex {

    private static final long serialVersionUID = 1L;
    
    /**
     * Does this intersection have a traffic light?
     */
    @Getter @Setter
    private boolean trafficLight;
    
    /**
     * Is this a free-flowing intersection, i.e. should it have no delay at all?
     * e.g., freeway ramps, &c.
     */
    @Getter @Setter
    private boolean freeFlowing;
    
    /**
     * 
     * @param from
     * @param to
     * @param mode
     * @param options
     * @param fromSpeed
     * @param toSpeed
     * @return
     */
    public double computeTraversalCost(PlainStreetEdge from, PlainStreetEdge to, TraverseMode mode,
            RoutingRequest options, float fromSpeed, float toSpeed) {
        int outAngle = to.getOutAngle();
        int inAngle = from.getInAngle();
        
        if (this.freeFlowing)
            return 0;
        
        // hack to infer freeflowing (freeway) operation
        if (fromSpeed > 25 && toSpeed > 25 && Math.abs(fromSpeed - toSpeed) < 7)
            return 0;
        
        if (mode != TraverseMode.CAR) {
            int turnCost = Math.abs(outAngle - inAngle);
            if (turnCost > 180) {
                turnCost = 360 - turnCost;
            }
            
            // TODO: This makes the turn cost lower the faster you're going
            return (turnCost / 20.0) / toSpeed;
        }
        else {
            // car routing
            
            // put out to the right of in; i.e. represent everything as one long right turn
            if (outAngle < inAngle)
                outAngle += 360;
            
            int turnAngle = outAngle - inAngle;
            double turnCost = 0;
            // the probability that they will have to stop to turn
            float probabilityStopToTurn = 0;
            
            // if they drive on the left, flip it around mirror-image so that drive-on-right-based
            // calculations work
            if (!options.driveOnRight)
                turnAngle = 360 - turnAngle;
            
            // check if this intersection has a traffic signal
            // Traffic signal times are based on a simple probabilistic model. Assuming a signal
            // cycle of 2 minutes, with two straight phases (N/S and E/W), each active 1/3 of the 
            // time, and two left turn phases, each active 1/6 of the time. This model is used to
            // calculate the delay by multiplying the average delay if one is stopped by the
            // probability one will be stopped, and calculating the probability one will have to
            // stop the same way                 
            // TODO: make configurable
            if (this.trafficLight) {            
                // estimate the amount of time stopped at the intersection
                if (turnAngle < 135) {
                    // right turn
                    turnCost += 15; // seconds
                    // on a right turn, you have to stop unless the light is green, a 1/3
                    // probability
                    probabilityStopToTurn = .66666667F;
                }
                else if (turnAngle < 225) {
                    // roughly straight
                    turnCost += 26.2666666667;
                    probabilityStopToTurn = .66666667F;
                }
                else {
                    // left turn
                    turnCost += 41.666666667;
                    probabilityStopToTurn = .8333333333f;
                }
            }
            
            ///
            else {
                // estimate the amount of time stopped at the intersection
                if (turnAngle < 135) {
                    // right turn
                    turnCost += 10; // seconds
                    probabilityStopToTurn = 1; // always stop when turning unsignalized
                }
                else if (turnAngle < 225) {
                    // roughly straight
                    turnCost += 12; // LOS B: http://en.wikipedia.org/wiki/Level_of_Service#LOS_for_At-Grade_Intersections
                    probabilityStopToTurn = 0.75f; // most unsignalized have stop signs
                }
                else {
                    // left turn
                    turnCost += 15;
                    probabilityStopToTurn = 1;
                }
            }
            
            // note that both acceleration and deceleration are multipled by 0.5, because half
            // of the acceleration/deceleration time has already been accounted for in the base
            // time calculations (this requires some algebra, but is correct).

            // calculate deceleration by multiplying the time for deceleration by the probability
            // of stopping (expected value)
            double decelerationTime = fromSpeed / options.carDecelerationSpeed;
            turnCost += decelerationTime * 0.5 * probabilityStopToTurn;
            
            // calculate acceleration the same way
            double accelerationTime = (toSpeed / options.carAccelerationSpeed);
            turnCost += accelerationTime * 0.5 * probabilityStopToTurn;
      
            return turnCost;
        }
    }

    public IntersectionVertex(Graph g, String label, double x, double y, String name) {
        super(g, label, x, y, name);
        freeFlowing = true;
        trafficLight = false;
    }
    
    public IntersectionVertex(Graph g, String label, double x, double y) {
        this(g, label, x, y, label);
    }

}
