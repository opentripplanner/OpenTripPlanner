package org.opentripplanner.routing.services;

import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Vertex;

/**
 * An interface for classes which produce RemainingWeightHeuristic instances 
 * specific to a given path search, taking the TraverseOptions, transport modes, 
 * target vertex, etc. into account.
 * 
 * @author andrewbyrd
 */
public interface RemainingWeightHeuristicFactory {

	public RemainingWeightHeuristic getInstanceForSearch(TraverseOptions opt, Vertex target);
		
}
