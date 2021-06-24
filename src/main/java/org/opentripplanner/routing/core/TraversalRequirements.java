package org.opentripplanner.routing.core;

import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.api.request.RoutingRequest;

/**
 * Preferences for how to traverse the graph.
 * 
 * @author avi
 */
public class TraversalRequirements {

    /**
     * Modes allowed in graph traversal. Defaults to allowing all.
     */
    public TraverseModeSet modes = TraverseModeSet.allModes();

    /**
     * If true, trip must be wheelchair accessible.
     */
    private boolean wheelchairAccessible = false;

    /**
     * The maximum slope of streets for wheelchair trips.
     * 
     * ADA max wheelchair ramp slope is a good default.
     */
    private double maxWheelchairSlope = 0.0833333333333;

    /**
     * Default constructor.
     * 
     * By default, accepts all modes of travel and does not require wheelchair access.
     */
    public TraversalRequirements() {
    }

    /**
     * Construct from RoutingRequest.
     * 
     * @param options
     */
    public TraversalRequirements(RoutingRequest options) {
        this();

        if (options == null) {
            return;
        }

        // Initialize self.
        initFromRoutingRequest(this, options);
    }

    /**
     * Initialize TraversalRequirements from a RoutingRequest.
     * 
     * @param req
     * @param options
     */
    private static void initFromRoutingRequest(TraversalRequirements req, RoutingRequest options) {
        req.modes = options.streetSubRequestModes.clone();
        req.wheelchairAccessible = options.wheelchairAccessible;
        req.maxWheelchairSlope = options.maxWheelchairSlope;
    }

    /** Returns true if this StreetEdge can be traversed. */
    private boolean canBeTraversedInternal(StreetEdge e) {
        if (wheelchairAccessible) {
            if (!e.isWheelchairAccessible()) {
                return false;
            }
            if (e.getMaxSlope() > maxWheelchairSlope) {
                return false;
            }
        }
        return e.canTraverse(modes);
    }

    /**
     * Returns true if this StreetEdge can be traversed.
     * Also checks if we can walk our bike on this StreetEdge.
     */
    public boolean canBeTraversed(StreetEdge e) {
        if (canBeTraversedInternal(e)) {
            return true;
        }
        return false;
    }

}
