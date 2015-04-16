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

import org.opentripplanner.osm.OSMFromToNodeWayIds;
import org.opentripplanner.routing.graph.Edge;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * This class is used to store some optional extra information about edges, which for memory-saving
 * reasons we do not want to store (even as a single pointer) in the Edge instance itself.
 * 
 * Used as an optional service in the Graph class.
 */
public class EdgesExtra implements Serializable {

    private static final long serialVersionUID = -8267295762562813810L;

    private BiMap<OSMFromToNodeWayIds, Edge> osmExtras = HashBiMap.create();

    public EdgesExtra() {
    }

    public void addOsmFromToNodeWayId(Edge edge, OSMFromToNodeWayIds osmExtra) {
        if (osmExtras.inverse().containsKey(edge))
            throw new IllegalArgumentException("Only a single OSM ids per edge is allowed. Edge: "
                    + edge);
        osmExtras.put(osmExtra, edge);
    }

    public OSMFromToNodeWayIds getOsmIdsFromEdge(Edge edge) {
        return osmExtras.inverse().get(edge);
    }

    public Edge getEdgeFromOsmIds(OSMFromToNodeWayIds osmIds) {
        return osmExtras.get(osmIds);
    }

    public void removeEdge(Edge edge) {
        /* Note: no need to synchronize, as no temporary edges should be present here. */
        osmExtras.inverse().remove(edge);
    }
}
