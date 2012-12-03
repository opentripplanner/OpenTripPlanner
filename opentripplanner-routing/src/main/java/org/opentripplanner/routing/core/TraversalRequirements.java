package org.opentripplanner.routing.core;

import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;

import lombok.Data;

/**
 * Preferences for how to traverse the graph.
 * 
 * @author avi
 */
@Data
public class TraversalRequirements {

	/**
	 * Modes allowed in graph traversal.
	 */
	private TraverseModeSet modes = new TraverseModeSet();
	
	/**
	 * The maximum distance (meters) the user is willing to walk. Defaults to
	 * 1/2 mile.
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
	 * Specific requirements for walking.
	 * 
	 * Do we actually need this? I think it may be extraneous...
	 */
	private TraversalRequirements walkingRequirements;
	
	/**
	 * Default constructor.
	 */
	public TraversalRequirements() {}
	
	/**
	 * Construct from RoutingRequest.
	 * 
	 * @param options
	 */
	public TraversalRequirements(RoutingRequest options) {
		initFromRoutingRequest(this, options);
		
		// Initialize walking requirements if any given.
		RoutingRequest walkOptions = options.getWalkingOptions();
		if (walkOptions != null) {
			walkingRequirements = new TraversalRequirements();
			initFromRoutingRequest(walkingRequirements, walkOptions);
		}
	}
	
	/**
	 * Initialize TraversalRequirements from a RoutingRequest.
	 * 
	 * @param req
	 * @param options
	 */
	private static void initFromRoutingRequest(TraversalRequirements req, RoutingRequest options) {
		req.modes = options.getModes().clone();
		req.wheelchairAccessible = options.wheelchairAccessible;
		req.maxWheelchairSlope = options.maxSlope;
		req.maxWalkDistance = options.maxWalkDistance;
	}
	
	/**
	 * True if walking requirements are defined. 
	 * @return
	 */
	public boolean hasWalkingRequirements() {
		return walkingRequirements != null;
	}
	
	/**
	 * Returns true if this StreetEdge can be traversed.
	 * 
	 * @param e
	 * @return
	 */
	public boolean canBeTraversed(StreetEdge e) {
		if (wheelchairAccessible) {
			if (!e.isWheelchairAccessible()) {
				return false;
			}
			if (e.getElevationProfileSegment().getMaxSlope() > maxWheelchairSlope) {
				return false;
			}
        }
        if (modes.getWalk() && e.getPermission().allows(StreetTraversalPermission.PEDESTRIAN)) {
            return true;
        }
        if (modes.getBicycle() && e.getPermission().allows(StreetTraversalPermission.BICYCLE)) {
            return true;
        }
        if (modes.getCar() && e.getPermission().allows(StreetTraversalPermission.CAR)) {
            return true;
        }
        return false;
	}
	
}
