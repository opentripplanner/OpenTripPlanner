package org.opentripplanner.analyst.core;

import org.opentripplanner.routing.core.State;

/**
 * The basic sample operator: minimize elapsed time with no initial wait removal or path 
 * optimization.  
 */
public class ElapsedTimeSampleOperator extends SampleOperator {

    public static final double DEFAULT_WALK_SPEED = 3.33; // m/sec

    private double walkSpeed;

    public ElapsedTimeSampleOperator() {
        this(DEFAULT_WALK_SPEED);
    }

    public ElapsedTimeSampleOperator(double walkSpeed) {
        this.walkSpeed = walkSpeed;
        this.minimize = true;
    }

    @Override
    public int evaluate(State state, double distance) {
        int seconds = (int) (distance / walkSpeed);
        return seconds + (int) state.getElapsedTime();
    }

}
