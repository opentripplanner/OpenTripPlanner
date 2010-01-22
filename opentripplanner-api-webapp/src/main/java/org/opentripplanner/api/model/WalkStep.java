/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
 */

package org.opentripplanner.api.model;

/**
 * Represents one instruction in walking directions. Three examples from New York City:
 * <p>
 * Turn onto Broadway from W 57th St (coming from 7th Ave): <br/>
 * distance = 100 (say) <br/>
 * walkDirection = RIGHT <br/>
 * streetName = Broadway <br/>
 * everything else null/false <br/>
 * </p><p>
 * Now, turn from Broadway onto Central Park S via Columbus Circle <br/>
 * distance = 200 (say) <br/>
 * walkDirection = CIRCLE_COUNTERCLOCKWISE <br/>
 * streetName = Central Park S <br/>
 * exit = 1 (first exit) <br/>
 * immediately everything else false <br/>
 * </p><p>
 * Instead, go through the circle to continue on Broadway <br/>
 * distance = 100 (say) <br/>
 * walkDirection = CIRCLE_COUNTERCLOCKWISE <br/>
 * streetName = Broadway <br/>
 * exit = 3 <br/>
 * stayOn = true <br/>
 * everything else false <br/>
 * </p>
 * */
public class WalkStep {

    /**
     * The distance in meters that this step takes.
     */
    public double distance = 0;

    /** 
     * The relative direction of this step.
     */
    public RelativeDirection relativeDirection;
    /**
     * The name of the street.
     */
    public String streetName;

    /**
     * The absolute direction of this step. 
     */
    public AbsoluteDirection absoluteDirection;
    
    /**  
     * When exiting a highway or traffic circle, the exit name/number.
     */

    public String exit;
    /**
     * Indicates whether or not a street changes direction at an intersection.
     */
    public Boolean stayOn = false;
    
    /**
     * A case where there is both a change of direction and a change
     * of street name, but not an intersection.
     */
    public Boolean becomes = false; 

    /**
     * The longitude of start of the step
     */
    public double lon;
    /**
     * The latitude of start of the step
     */
    public double lat;

    public void setDirections(double lastAngle, double thisAngle) {
        relativeDirection = getRelativeDirection(lastAngle, thisAngle);
        setAbsoluteDirection(thisAngle);
    }

    public String toString() {
        String direction = absoluteDirection.toString();
        if (relativeDirection != null) {
            direction = relativeDirection.toString();
        }
        return "WalkStep(" + direction + " on " + streetName + " for " + distance + ")";
    }

    public static RelativeDirection getRelativeDirection(double lastAngle, double thisAngle) {

        double angleDiff = thisAngle - lastAngle;
        if (angleDiff < 0) {
            angleDiff += Math.PI * 2;
        }
        double ccwAngleDiff = Math.PI * 2 - angleDiff;

        if (angleDiff < 0.15 || ccwAngleDiff < 0.15) {
            return RelativeDirection.CONTINUE;
        } else if (angleDiff < 0.7) {
            return RelativeDirection.SLIGHTLY_RIGHT;
        } else if (ccwAngleDiff < 0.7) {
            return RelativeDirection.SLIGHTLY_LEFT;
        } else if (angleDiff < 2) {
            return RelativeDirection.RIGHT;
        } else if (ccwAngleDiff < 2) {
            return RelativeDirection.LEFT;
        } else if (angleDiff < Math.PI) {
            return RelativeDirection.HARD_RIGHT;
        } else {
            return RelativeDirection.HARD_LEFT;
        }
    }

    public void setAbsoluteDirection(double thisAngle) {
        int octant = (int) (8 + Math.round(thisAngle * 8 / (Math.PI * 2))) % 8;
        absoluteDirection = AbsoluteDirection.values()[octant];
    }
}
