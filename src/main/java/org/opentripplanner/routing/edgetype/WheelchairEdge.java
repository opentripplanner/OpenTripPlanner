package org.opentripplanner.routing.edgetype;

/**
 * An edge that contains information about whether it can be traversed in a wheelchair.
 */
public interface WheelchairEdge {
    boolean isWheelchairAccessible();
}
