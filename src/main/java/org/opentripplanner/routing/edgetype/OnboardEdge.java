package org.opentripplanner.routing.edgetype;

/** 
 * Interface for edges which are on board a transit vehicle.
 */
public interface OnboardEdge {

    /** 
     * The stop index (within the trip) of the stop this edge comes from. This is equivalent
     * to a hop index. 
     */
    int getStopIndex();

}
