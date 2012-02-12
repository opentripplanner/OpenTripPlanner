package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.algorithm.strategies.DefaultRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.LBGRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.RemainingWeightHeuristicFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This RemainingWeightHeuristicFactory returns an appropriately configured LBG
 * heuristic instance for transit searches, and a Euclidean heuristic instance
 * for non-transit searches.
 * 
 * @author andrewbyrd
 */
public class LBGRemainingWeightHeuristicFactoryImpl implements
		RemainingWeightHeuristicFactory {

	private static final Logger LOG = LoggerFactory
			.getLogger(LBGRemainingWeightHeuristicFactoryImpl.class);

	private GraphService _graphService;

	@Autowired
	public LBGRemainingWeightHeuristicFactoryImpl(GraphService gs) {
		_graphService = gs;
	}

	@Override
	public RemainingWeightHeuristic getInstanceForSearch(TraverseOptions opt,
			Vertex target) {
		if (opt.getModes().getTransit()) {
			LOG.debug("Transit itinerary requested.");
			return new LBGRemainingWeightHeuristic(_graphService.getGraph(), opt);
		} else {
			LOG.debug("Non-transit itinerary requested.");
			return new DefaultRemainingWeightHeuristic();
		}
	}

}
