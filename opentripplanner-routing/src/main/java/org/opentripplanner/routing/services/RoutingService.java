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

import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.spt.GraphPath;

public interface RoutingService {

    public List<GraphPath> route(Vertex fromVertex, Vertex toVertex, long time,
            TraverseOptions options);

    /**
     * Here we wish to plan a trip that starts at "fromVertex", travels through the intermediate
     * vertices in some arbitrary but hopefully optimal order, and eventually end up at "toVertex".
     * 
     * @param fromPlace
     * @param toPlace
     * @param intermediatePlaces
     * @param dateTime
     * @param options
     * @return
     */
    public GraphPath route(Vertex fromVertex, Vertex toVertex, List<Vertex> intermediateVertices,
            long time, TraverseOptions options);
}
