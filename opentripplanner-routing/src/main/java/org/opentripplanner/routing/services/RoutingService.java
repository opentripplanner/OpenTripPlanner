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

import java.util.List;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;

public interface RoutingService {

    /**
     * In the case of "arrive-by" routing, the origin state is actually the user's end location and
     * the target vertex is the user's start location.
     * 
     * @param origin
     *            the start state to begin the route search from
     * @param target
     *            the end vertex to route to
     * @return the set of paths routing between the origin and the target
     */
    public List<GraphPath> route(State origin, Vertex target);

    /**
     * Here we wish to plan a trip that starts at "fromVertex", travels through the intermediate
     * vertices in either the optimal order or the specified order, and eventually end up at "toVertex".
     * 
     * @param fromPlace
     * @param toPlace
     * @param intermediatePlaces
     * @param dateTime
     * @param ordered whether the ordering is optimal (false), or specified (true)
     * @param options
     * @return
     */
    public GraphPath route(Vertex fromVertex, Vertex toVertex, List<Vertex> intermediateVertices,
            boolean ordered, int dateTime, TraverseOptions options);
}
