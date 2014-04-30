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

package org.opentripplanner.extra_graph;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.routing.graph.Edge;

import java.util.Collection;

public class EdgesForRoute {
    public Multimap<Route, Edge> edgesForRoute = ArrayListMultimap.create();

    public Collection<Edge> get(Route route) {
        return edgesForRoute.get(route);
    }
}
