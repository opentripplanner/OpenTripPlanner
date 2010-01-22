package org.opentripplanner.api.ws;

import org.opentripplanner.api.model.AbsoluteDirection;
import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.api.model.WalkStep;

import junit.framework.TestCase;

public class TestWalkStep extends TestCase {

    public void testRelativeDirection() {
        WalkStep step = new WalkStep();

        double angle1 = degreesToRadians(0);
        double angle2 = degreesToRadians(90);

        step.setDirections(angle1, angle2);
        assertEquals(RelativeDirection.RIGHT, step.relativeDirection);
        assertEquals(AbsoluteDirection.EAST, step.absoluteDirection);

        angle1 = degreesToRadians(0);
        angle2 = degreesToRadians(5);

        step.setDirections(angle1, angle2);
        assertEquals(RelativeDirection.CONTINUE, step.relativeDirection);
        assertEquals(AbsoluteDirection.NORTH, step.absoluteDirection);

        angle1 = degreesToRadians(0);
        angle2 = degreesToRadians(240);

        step.setDirections(angle1, angle2);
        assertEquals(RelativeDirection.HARD_LEFT, step.relativeDirection);
        assertEquals(AbsoluteDirection.SOUTHWEST, step.absoluteDirection);

    }

    private double degreesToRadians(double deg) {
        return deg * Math.PI / 180;
    }
}
