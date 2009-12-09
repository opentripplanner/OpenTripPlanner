package org.opentripplanner.api.model;

/** Represents one instruction in walking directions.  
 * Three examples from New York City: 
 * 
 * Turn onto Broadway from W 57th St (coming from 7th Ave):
 * distance = 100 (say)
 * walkDirection = RIGHT
 * streetName = Broadway
 * everything else null/false
 * 
 * Now, turn from Broadway onto Central Park S via Columbus Circle
 * distance = 200 (say)
 * walkDirection = CIRCLE_COUNTERCLOCKWISE 
 * streetName = Central Park S
 * exit = 1 //first exit, immediately
 * everything else false
 * 
 * Instead, go through the circle to continue on Broadway
 * distance = 100 (saY)
 * walkDirection = CIRCLE_COUNTERCLOCKWISE
 * streetName = Broadway
 * exit = 3
 * stayOne = true
 * everything else false
 * */
public class WalkStep {

    public double distance = 0;

    public RelativeDirection relativeDirection;
    public String streetName;

    public String absoluteDirection; // cardinals and semicardinals; optional
    public String exit; // when exiting a highway or traffic circle, the exit name/number

    public Boolean stayOn = false; // a street changes direction at an intersection
    public Boolean becomes = false; /*
                                     * a case where there is both a change of direction and a change
                                     * of street name, but not an intersection.
                                     */

    public void setRelativeDirection(double lastAngle, double thisAngle) {
        relativeDirection = getRelativeDirection(lastAngle, thisAngle);
    }

    public String toString() {
        String direction = absoluteDirection;
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
            return RelativeDirection.SLIGHTLY_LEFT;
        } else if (ccwAngleDiff < 0.7) {
            return RelativeDirection.SLIGHTLY_RIGHT;
        } else if (angleDiff < 2) {
            return RelativeDirection.LEFT;
        } else if (ccwAngleDiff < 2) {
            return RelativeDirection.RIGHT;
        } else if (angleDiff < Math.PI) {
            return RelativeDirection.HARD_LEFT;
        } else {
            return RelativeDirection.HARD_RIGHT;
        }
    }
}
