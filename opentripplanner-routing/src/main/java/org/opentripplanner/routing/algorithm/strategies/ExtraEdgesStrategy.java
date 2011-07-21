/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.algorithm.strategies;

import java.util.List;
import java.util.Map;

import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Vertex;

/**
 * When a temporary {@link Vertex} is generated at the origin or destination of a search, it is
 * often necessary to provide temporary edges linking the {@link Vertex} to the rest of the graph.
 * The extra edges strategy provides a plugin-interface for providing those connections.
 * 
 * @author bdferris
 * @see DefaultExtraEdgesStrategy
 */
public interface ExtraEdgesStrategy {

    /**
     * Add any incoming edges from vertices in the graph to the origin vertex in a backwards search.
     * 
     * @param extraEdges
     * @param origin
     */
    public void addIncomingEdgesForOrigin(Map<Vertex, List<Edge>> extraEdges, Vertex origin);

    /**
     * Add any incoming edges from the target vertex to vertices in the graph in a backwards search.
     * 
     * @param extraEdges
     * @param origin
     */
    public void addIncomingEdgesForTarget(Map<Vertex, List<Edge>> extraEdges, Vertex target);

    /**
     * Add any outgoing edges from the origin vertex to vertices in the graph in a forwards search.
     * 
     * @param extraEdges
     * @param origin
     */
    public void addOutgoingEdgesForOrigin(Map<Vertex, List<Edge>> extraEdges, Vertex origin);

    /**
     * Add any outgoing edges from vertices in the graph to the target vertex in a forward search.
     * 
     * @param extraEdges
     * @param target
     */
    public void addOutgoingEdgesForTarget(Map<Vertex, List<Edge>> extraEdges, Vertex target);
}
