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

package org.opentripplanner.routing.vertextype;

import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Graph;

/**
 * This vertex is created from all barrier tags.
 *
 * Currently only barrier=bollard is supported.
 * Node barrier=bollard implies access=no, foot=yes, bicycle=yes
 *
 * On this vertex geometry is split and two new edges are created.
 *
 * If start/end vertex of {@link org.opentripplanner.routing.edgetype.StreetEdge} is BarrierVertex
 * edge isn't traversable with CAR.
 *
 * Created by mabu on 11.5.2015.
 */
public class BarrierVertex extends OsmVertex {

    private StreetTraversalPermission barrierPermissions;

    //According to OSM default permissions are access=no, foot=yes, bicycle=yes
    public static final StreetTraversalPermission defaultBarrierPermissions = StreetTraversalPermission.PEDESTRIAN_AND_BICYCLE;

    public BarrierVertex(Graph g, String label, double x, double y, long nodeId) {
        super(g, label, x, y, nodeId);
        barrierPermissions = defaultBarrierPermissions;
    }

    public StreetTraversalPermission getBarrierPermissions() {
        return barrierPermissions;
    }

    public void setBarrierPermissions(StreetTraversalPermission barrierPermissions) {
        this.barrierPermissions = barrierPermissions;
    }
}
