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

package org.opentripplanner.routing.algorithm.strategies;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

/**
 * Strategy interface to provide additional logic to decide if a given traverse result should not be
 * considered further.
 * 
 * @author bdferris
 * 
 */
public interface SkipTraverseResultStrategy {

    /**
     * 
     * @param origin the origin vertex
     * @param target the target vertex, may be null in an undirected search
     * @param parent the parent shortest-path-tree vertex
     * @param traverseResult the current traverse result to consider for skipping
     * @param spt the shortest path tree
     * @param traverseOptions the current traverse options
     * @param remainingWeightEstimate the remaining weight estimate from the heuristic (or -1 if no heuristic)
     * @return true if the given traverse result should not be considered further
     */
    public boolean shouldSkipTraversalResult(Vertex origin, Vertex target, State parent,
            State current, ShortestPathTree spt, RoutingRequest traverseOptions);
}
