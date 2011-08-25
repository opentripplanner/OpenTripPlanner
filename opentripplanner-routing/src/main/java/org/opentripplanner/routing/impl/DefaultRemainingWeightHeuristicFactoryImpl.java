package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.algorithm.strategies.DefaultRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.services.RemainingWeightHeuristicFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This trivial RemainingWeightHeuristicFactory returns a Euclidean heuristic instance
 * for every search, irrespective of destination, modes, etc.
 * 
 * @author andrewbyrd
 */
public class DefaultRemainingWeightHeuristicFactoryImpl implements
		RemainingWeightHeuristicFactory {

    private static final Logger LOG = 
    	LoggerFactory.getLogger(DefaultRemainingWeightHeuristicFactoryImpl.class);

	@Override
	public RemainingWeightHeuristic getInstanceForSearch(TraverseOptions opt,
			Vertex target) {
        LOG.debug("Using Euclidean heuristic independent of search type.");
        return new DefaultRemainingWeightHeuristic();
 	}

}
