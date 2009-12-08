package org.opentripplanner.api.ws;

import org.opentripplanner.api.model.RelativeDirection;
import org.opentripplanner.api.model.WalkStep;

import junit.framework.TestCase;

public class TestWalkStep extends TestCase {

    public void testRelativeDirection() {
        WalkStep step = new WalkStep();

        double angle1 = Math.atan2(10, 0);
        double angle2 = Math.atan2(0, 5);

        step.setRelativeDirection(angle1, angle2);
        assertEquals(RelativeDirection.RIGHT, step.relativeDirection);

        angle1 = Math.atan2(10, 0);
        angle2 = Math.atan2(10, 1);

        step.setRelativeDirection(angle1, angle2);
        assertEquals(RelativeDirection.CONTINUE, step.relativeDirection);
        
        
        angle1 = Math.atan2(10, 0);
        angle2 = Math.atan2(-7, -5);

        step.setRelativeDirection(angle1, angle2);
        assertEquals(RelativeDirection.HARD_LEFT, step.relativeDirection);
    }
}
