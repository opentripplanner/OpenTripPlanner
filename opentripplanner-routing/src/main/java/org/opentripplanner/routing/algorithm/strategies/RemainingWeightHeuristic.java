package org.opentripplanner.routing.algorithm.strategies;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;

/**
 * 
 * 
 */
public interface RemainingWeightHeuristic {
	
	/**
	 * Provides an admissible estimate of (lower bound on) the weight of a path to 
	 * the target, starting with the given state.
	 * 
	 * It is important to evaluate the initial weight before computing additional weights, 
	 * because this method also performs any one-time setup and precomputation that will be used
	 * by the heuristic during the search. 
	 * 
	 * @param s
	 * @param target
	 * @param options
	 * @return
	 */
    public double computeInitialWeight(State s, Vertex target, TraverseOptions options);

    public double computeForwardWeight(State s, Vertex target);

    public double computeReverseWeight(State s, Vertex target);
}
