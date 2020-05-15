package org.opentripplanner.routing.algorithm.astar.strategies;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.State;

import java.io.Serializable;

/**
 * Interface for classes that provides an admissible estimate of (lower bound on) 
 * the weight of a path to the target, starting from a given state.
 */
public interface RemainingWeightHeuristic extends Serializable {
	
    /** 
     * Perform any one-time setup and pre-computation that will be needed by later calls to
     * computeForwardWeight/computeReverseWeight. We may want to start from multiple origin states, so initialization
     * cannot depend on the origin vertex or state.
     * @param abortTime time since the Epoch in milliseconds at which we should bail out of initialization,
     *                  or Long.MAX_VALUE for no limit.
     */
    public void initialize (RoutingRequest options, long abortTime);

    public double estimateRemainingWeight (State s);

    /** Reset any cached data in the heuristic, before reuse in a search with the same destination. */
    public void reset();
    
    /** 
     * Call to cause the heuristic to perform some predetermined amount of work improving its 
     * estimate. Avoids thread synchronization evil by interleaving forward and backward searches. 
     */
    public void doSomeWork();
    
}


// Perhaps directionality should also be defined during the setup,
// instead of having two separate methods for the two directions.
// We might not even need a setup method if the routing options are just passed into the
// constructor.
