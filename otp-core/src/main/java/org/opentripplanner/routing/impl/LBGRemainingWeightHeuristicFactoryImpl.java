/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.algorithm.strategies.DefaultRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.LBGRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.core.RoutingRequest;
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
	public RemainingWeightHeuristic getInstanceForSearch(RoutingRequest opt) {
	        Vertex target = opt.rctx.target;
		if (opt.getModes().isTransit()) {
			LOG.debug("Transit itinerary requested.");
			return new LBGRemainingWeightHeuristic(_graphService.getGraph(), opt);
		} else {
			LOG.debug("Non-transit itinerary requested.");
			return new DefaultRemainingWeightHeuristic();
		}
	}

}
