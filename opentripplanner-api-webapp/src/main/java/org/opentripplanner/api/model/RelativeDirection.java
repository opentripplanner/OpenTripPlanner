package org.opentripplanner.api.model;

/**
 * Represents a turn direction, relative to the current heading.
 * 
 * CIRCLE_CLOCKWISE and CIRCLE_CLOCKWISE are used to represent traffic circles. 
 * 
 */
public enum RelativeDirection {
    HARD_LEFT, LEFT, SLIGHTLY_LEFT, CONTINUE, SLIGHTLY_RIGHT, RIGHT, HARD_RIGHT, CIRCLE_CLOCKWISE, CIRCLE_COUNTERCLOCKWISE;
}
