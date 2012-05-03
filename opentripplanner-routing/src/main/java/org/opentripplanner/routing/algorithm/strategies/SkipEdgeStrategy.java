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
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;

/**
 * Strategy interface to provide additional logic to decide if a given edge should not be considered
 * for traversal.
 * 
 * @author bdferris
 * 
 */
public interface SkipEdgeStrategy {

    /**
     * 
     * @param origin the origin vertex
     * @param target the target vertex, may be null in an undirected search
     * @param current the current vertex
     * @param edge the current edge to potentially be skipped
     * @param spt the shortest path tree
     * @param traverseOptions the traverse options
     * @return true if the given edge should not be considered for traversal
     */
    public boolean shouldSkipEdge(Vertex origin, Vertex target, State current, Edge edge,
            ShortestPathTree spt, RoutingRequest traverseOptions);
}
