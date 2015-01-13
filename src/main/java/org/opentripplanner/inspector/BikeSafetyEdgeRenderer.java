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
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetBikeRentalLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

/**
 * Render bike safety for each edge using a color palette. Display the bike safety factor as label.
 * 
 * @author laurent
 * 
 */
public class BikeSafetyEdgeRenderer implements EdgeVertexRenderer {

    private ScalarColorPalette palette = new DefaultScalarColorPalette(1.0, 3.0, 10.0);

    private static final Color BIKE_RENTAL_COLOR_VERTEX = new Color(0.0f, 0.7f, 0.0f);

    public BikeSafetyEdgeRenderer() {
    }

    @Override
    public boolean renderEdge(Edge e, EdgeVisualAttributes attrs) {
        if (e instanceof StreetEdge) {
            StreetEdge pse = (StreetEdge) e;
            if (pse.getPermission().allows(TraverseMode.BICYCLE)) {
                double bikeSafety = pse.getBicycleSafetyFactor();
                attrs.color = palette.getColor(bikeSafety);
                attrs.label = String.format("%.02f", bikeSafety);
            } else {
                attrs.color = Color.LIGHT_GRAY;
                attrs.label = "no bikes";
            }
        } else if (e instanceof StreetBikeRentalLink) {
            attrs.color = palette.getColor(1.0f);
            attrs.label = "link";
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean renderVertex(Vertex v, VertexVisualAttributes attrs) {
        if (v instanceof BikeRentalStationVertex) {
            attrs.color = BIKE_RENTAL_COLOR_VERTEX;
            attrs.label = v.getName();
        } else if (v instanceof IntersectionVertex) {
            attrs.color = Color.DARK_GRAY;
        } else {
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return "Bike safety";
    }
}