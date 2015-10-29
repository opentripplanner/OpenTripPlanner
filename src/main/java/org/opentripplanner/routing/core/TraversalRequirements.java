package org.opentripplanner.routing.core;

import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

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
     * The maximum distance (meters) the user is willing to walk. Defaults to 1/2 mile.
     */
    private double maxWalkDistance = 800;

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
     * Specific requirements for walking a bicycle.
     */
    private TraversalRequirements bikeWalkingRequirements;

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

        // Initialize walking requirements if any given.
        RoutingRequest bikeWalkOptions = options.bikeWalkingOptions;
        if (bikeWalkOptions != null) {
            bikeWalkingRequirements = new TraversalRequirements();
            initFromRoutingRequest(bikeWalkingRequirements, bikeWalkOptions);
        }
    }

    /**
     * Initialize TraversalRequirements from a RoutingRequest.
     * 
     * @param req
     * @param options
     */
    private static void initFromRoutingRequest(TraversalRequirements req, RoutingRequest options) {
        req.modes = options.modes.clone();
        req.wheelchairAccessible = options.wheelchairAccessible;
        req.maxWheelchairSlope = options.maxSlope;
        req.maxWalkDistance = options.maxWalkDistance;
    }

    /**
     * Returns true if bike walking requirements are defined.
     * 
     * @return
     */
    public boolean hasBikeWalkingRequirements() {
        return bikeWalkingRequirements != null;
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
        } else if (hasBikeWalkingRequirements()
                && bikeWalkingRequirements.canBeTraversedInternal(e)) {
            return true;
        }
        return false;
    }

}
