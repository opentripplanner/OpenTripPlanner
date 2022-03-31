package org.opentripplanner.routing.algorithm.astar.strategies;

import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.RoutingContext;
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
     */
    void initialize(RoutingContext routingContext);

    double estimateRemainingWeight(State s);
}