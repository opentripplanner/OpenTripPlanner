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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.opentripplanner.osm.OSMFromToNodeWayIds;
import org.opentripplanner.routing.graph.Edge;

/**
 * This class is used to store some optional extra information about edges, which for memory-saving
 * reasons we do not want to store (even as a single pointer) in the Edge instance itself.
 * 
 * Used as an optional service in the Graph class.
 */
public class EdgesExtra implements Serializable {

    private static final long serialVersionUID = -8267295762562813810L;

    private Map<Edge, OSMFromToNodeWayIds> osmExtras = new HashMap<Edge, OSMFromToNodeWayIds>();

    public EdgesExtra() {
    }

    public void addOsmFromToNodeWayId(Edge edge, OSMFromToNodeWayIds osmExtra) {
        if (osmExtras.containsKey(edge))
            throw new IllegalArgumentException("Only a single OSM ids per edge is allowed. Edge: "
                    + edge);
        osmExtras.put(edge, osmExtra);
    }

    public OSMFromToNodeWayIds getOsmFromToNodeWayIds(Edge edge) {
        return osmExtras.get(edge);
    }

    public void removeEdge(Edge edge) {
        osmExtras.remove(edge);
    }
}
