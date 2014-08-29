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

package org.opentripplanner.inspector;

import java.awt.Color;

import org.opentripplanner.inspector.EdgeVertexTileRenderer.EdgeVertexRenderer;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.EdgeVisualAttributes;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.VertexVisualAttributes;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetTraversalPermission;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

/**
 * Render traversal permissions for each edge by color and label (walk, bicycle, car, stairs).
 * 
 * @author laurent
 */
public class TraversalPermissionsEdgeRenderer implements EdgeVertexRenderer {

    // TODO
    private static final Color LINK_COLOR = Color.ORANGE;

    private static final Color STAIRS_COLOR = Color.PINK;

    public TraversalPermissionsEdgeRenderer() {
    }

    @Override
    public boolean renderEdge(Edge e, EdgeVisualAttributes attrs) {
        // TODO Add link edges
        if (e instanceof PlainStreetEdge) {
            PlainStreetEdge pse = (PlainStreetEdge) e;
            if (pse.isStairs()) {
                attrs.color = STAIRS_COLOR;
                attrs.label = "stairs";
            } else {
                attrs.color = getColor(pse.getPermission());
                attrs.label = getLabel(pse.getPermission());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean renderVertex(Vertex v, VertexVisualAttributes attrs) {
        // TODO
        attrs.color = Color.DARK_GRAY;
        return true;
    }

    private Color getColor(StreetTraversalPermission permissions) {
        /*
         * We use the trick that there are 3 main traversal modes (WALK, BIKE and CAR) and 3 color
         * channels (R, G, B).
         */
        float r = 0.2f;
        float g = 0.2f;
        float b = 0.2f;
        if (permissions.allows(StreetTraversalPermission.PEDESTRIAN))
            g += 0.5f;
        if (permissions.allows(StreetTraversalPermission.BICYCLE))
            b += 0.5f;
        if (permissions.allows(StreetTraversalPermission.CAR))
            r += 0.5f;
        // TODO CUSTOM_VEHICLE (?)
        return new Color(r, g, b);
    }

    private String getLabel(StreetTraversalPermission permissions) {
        StringBuffer sb = new StringBuffer();
        if (permissions.allows(StreetTraversalPermission.PEDESTRIAN))
            sb.append("walk,");
        if (permissions.allows(StreetTraversalPermission.BICYCLE))
            sb.append("bike,");
        if (permissions.allows(StreetTraversalPermission.CAR))
            sb.append("car,");
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1); // Remove last comma
        } else {
            sb.append("none");
        }
        return sb.toString();
    }

}