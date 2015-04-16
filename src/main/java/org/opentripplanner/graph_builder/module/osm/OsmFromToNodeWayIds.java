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

package org.opentripplanner.graph_builder.module.osm;

import com.google.common.base.Objects;

public class OsmFromToNodeWayIds {

    public final long fromNodeId;

    public final long toNodeId;

    public final long wayId;

    public OsmFromToNodeWayIds(long fromNodeId, long toNodeId, long wayId) {
        this.fromNodeId = fromNodeId;
        this.toNodeId = toNodeId;
        this.wayId = wayId;
    }

    @Override
    public String toString() {
        return String.format("OSM(%d/%d>%d)", wayId, fromNodeId, toNodeId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fromNodeId, toNodeId, wayId);
    }

    @Override
    public boolean equals(Object another) {
        if (!(another instanceof OsmFromToNodeWayIds))
            return false;
        if (another == this)
            return true;

        OsmFromToNodeWayIds rhs = (OsmFromToNodeWayIds) another;
        return fromNodeId == rhs.fromNodeId && toNodeId == rhs.toNodeId && wayId == rhs.wayId;
    }
}
