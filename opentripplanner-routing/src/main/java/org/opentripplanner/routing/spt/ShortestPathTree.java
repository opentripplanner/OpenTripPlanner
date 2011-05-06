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

package org.opentripplanner.routing.spt;

import java.util.List;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;

public interface ShortestPathTree {

    /**
     * Add a vertex to the shortest path tree. If the vertex is already in the tree, this adds
     * another way to get to that vertex, if that new way is not strictly dominated in both time and
     * weight by an existing way.
     * 
     * @param vertex The graph vertex
     * @param state The state on arrival
     * @param weightSum The cost to get here
     * @param options The traversal options
     * @return
     */
    public SPTVertex addVertex(Vertex vertex, State state, double weightSum, TraverseOptions options);

    public GraphPath getPath(Vertex dest);

    public GraphPath getPath(Vertex dest, boolean optimize);

    public List<GraphPath> getPaths(Vertex dest, boolean optimize);

    public void removeVertex(SPTVertex vertex);
}