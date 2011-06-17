package org.opentripplanner.routing.algorithm.strategies;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.Vertex;

/**
 * Intended primarily for testing of and experimentation with heuristics based on
 * the triangle inequality and metric embeddings.
 * 
 * A heuristic that performs a single-source / all destinations shortest path search
 * in a weighted, directed graph whose shortest path metric is a lower bound on
 * path weight in our main, time-dependent graph. 
 * 
 * @author andrewbyrd
 */
public class LBGRemainingWeightHeuristic implements RemainingWeightHeuristic {

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

}
