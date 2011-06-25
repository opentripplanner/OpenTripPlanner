package org.opentripplanner.routing.algorithm.strategies;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.Vertex;

/**
 * A trivial heuristic that always returns 0, which is always admissible. 
 * For use in testing and troubleshooting.
 * 
 * @author andrewbyrd
 */
public class TrivialRemainingWeightHeuristic implements RemainingWeightHeuristic {

    @Override
    public double computeInitialWeight(State s, Vertex target) {
        return 0;
    }

    @Override
    public double computeForwardWeight(State s, Vertex target) {
    	return 0;
    }

    @Override
    public double computeReverseWeight(State s, Vertex target) {
    	return 0;
    }

	@Override
	public void reset() {		
	}

}
