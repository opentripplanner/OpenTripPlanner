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
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.RemainingWeightHeuristicFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This trivial RemainingWeightHeuristicFactory returns a Euclidean heuristic instance
 * for every search, irrespective of destination, modes, etc.
 * 
 * @author andrewbyrd
 */
public class DefaultRemainingWeightHeuristicFactoryImpl 
    implements RemainingWeightHeuristicFactory {

    private static final Logger LOG = 
    	LoggerFactory.getLogger(DefaultRemainingWeightHeuristicFactoryImpl.class);

    @Override
    public RemainingWeightHeuristic getInstanceForSearch(RoutingRequest opt) {
        //LOG.debug("Using Euclidean heuristic independent of search type.");
        return new DefaultRemainingWeightHeuristic();
    }

}
