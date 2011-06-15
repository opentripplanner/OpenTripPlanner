package org.opentripplanner.routing.algorithm.strategies;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.Vertex;

/**
 * Interface for classes that provides an admissible estimate of (lower bound on) 
 * the weight of a path to the target, starting from a given state.
 */
public interface RemainingWeightHeuristic {
	
	/** 
	 * It is important to evaluate the initial weight before computing additional weights, 
	 * because this method also performs any one-time setup and precomputation that will be used
	 * by the heuristic during the search. 
	 */
    public double computeInitialWeight(State s, Vertex target);

    public double computeForwardWeight(State s, Vertex target);

    public double computeReverseWeight(State s, Vertex target);
}
