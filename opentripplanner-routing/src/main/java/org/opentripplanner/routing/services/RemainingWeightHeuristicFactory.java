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

package org.opentripplanner.routing.services;

import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Vertex;

/**
 * An interface for classes which produce RemainingWeightHeuristic instances 
 * specific to a given path search, taking the TraverseOptions, transport modes, 
 * target vertex, etc. into account.
 * 
 * @author andrewbyrd
 */
public interface RemainingWeightHeuristicFactory {

	public RemainingWeightHeuristic getInstanceForSearch(RoutingRequest opt);
		
}
