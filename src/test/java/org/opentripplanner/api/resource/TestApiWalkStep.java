package org.opentripplanner.api.resource;

import org.opentripplanner.api.model.ApiAbsoluteDirection;
import org.opentripplanner.api.model.ApiRelativeDirection;
import org.opentripplanner.api.model.ApiWalkStep;

import junit.framework.TestCase;

public class TestApiWalkStep extends TestCase {

    public void testRelativeDirection() {
        ApiWalkStep step = new ApiWalkStep();

        double angle1 = degreesToRadians(0);
        double angle2 = degreesToRadians(90);

        step.setDirections(angle1, angle2, false);
        assertEquals(ApiRelativeDirection.RIGHT, step.relativeDirection);
        assertEquals(ApiAbsoluteDirection.EAST, step.absoluteDirection);

        angle1 = degreesToRadians(0);
        angle2 = degreesToRadians(5);

        step.setDirections(angle1, angle2, false);
        assertEquals(ApiRelativeDirection.CONTINUE, step.relativeDirection);
        assertEquals(ApiAbsoluteDirection.NORTH, step.absoluteDirection);

        angle1 = degreesToRadians(0);
        angle2 = degreesToRadians(240);

        step.setDirections(angle1, angle2, false);
        assertEquals(ApiRelativeDirection.HARD_LEFT, step.relativeDirection);
        assertEquals(ApiAbsoluteDirection.SOUTHWEST, step.absoluteDirection);

    }

    private double degreesToRadians(double deg) {
        return deg * Math.PI / 180;
    }
}
