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
import org.opentripplanner.routing.edgetype.*;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.*;

/**
 * Render traversal permissions for each edge by color and label (walk, bicycle, car, stairs).
 * 
 * @author laurent
 */
public class TraversalPermissionsEdgeRenderer implements EdgeVertexRenderer {

    private static final Color LINK_COLOR_EDGE = Color.ORANGE;

    private static final Color STAIRS_COLOR_EDGE = Color.PINK;

    private static final Color STREET_COLOR_VERTEX = Color.DARK_GRAY;

    private static final Color TRANSIT_STOP_COLOR_VERTEX = new Color(0.0f, 0.0f, 0.8f);

    private static final Color TRANSIT_STATION_COLOR_VERTEX = new Color(0.4f, 0.0f, 0.8f);

    private static final Color BIKE_RENTAL_COLOR_VERTEX = new Color(0.0f, 0.7f, 0.0f);

    private static final Color PARK_AND_RIDE_COLOR_VERTEX = Color.RED;

    private static final Color BARRIER_COLOR_VERTEX = new Color(0.5803922f, 0.21568628f,
        0.24313726f);

    @Override
    public boolean renderEdge(Edge e, EdgeVisualAttributes attrs) {
        if (e instanceof StreetEdge) {
            StreetEdge pse = (StreetEdge) e;
            if (pse.isStairs()) {
                attrs.color = STAIRS_COLOR_EDGE;
                attrs.label = "stairs";
            } else {
                attrs.color = getColor(pse.getPermission());
                attrs.label = getLabel(pse.getPermission());
            }
        } else if (e instanceof StreetTransitLink) {
            attrs.color = LINK_COLOR_EDGE;
            attrs.label = "link";
        } else if (e instanceof StreetBikeRentalLink) {
            attrs.color = LINK_COLOR_EDGE;
            attrs.label = "link";
        } else if (e instanceof ParkAndRideLinkEdge) {
            attrs.color = LINK_COLOR_EDGE;
            attrs.label = "link";
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean renderVertex(Vertex v, VertexVisualAttributes attrs) {
        if (v instanceof IntersectionVertex) {
            attrs.color = STREET_COLOR_VERTEX;
            if (v instanceof BarrierVertex) {
                attrs.color = BARRIER_COLOR_VERTEX;
            }
        } else if (v instanceof TransitStop) {
            attrs.color = TRANSIT_STOP_COLOR_VERTEX;
            attrs.label = v.getName();
        } else if (v instanceof TransitStation) {
            attrs.color = TRANSIT_STATION_COLOR_VERTEX;
            attrs.label = v.getName();
        } else if (v instanceof BikeRentalStationVertex) {
            attrs.color = BIKE_RENTAL_COLOR_VERTEX;
            attrs.label = v.getName();
        } else if (v instanceof ParkAndRideVertex) {
            attrs.color = PARK_AND_RIDE_COLOR_VERTEX;
            attrs.label = v.getName();
        } else {
            return false;
        }
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

    @Override
    public String getName() {
        return "Traversal permissions";
    }
}