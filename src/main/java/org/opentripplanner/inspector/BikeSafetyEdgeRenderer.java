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
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;

/**
 * Render bike safety for each edge using a color palette. Display the bike safety factor as label.
 * 
 * @author laurent
 * 
 */
public class BikeSafetyEdgeRenderer implements EdgeVertexRenderer {

    private ScalarColorPalette palette = new DefaultScalarColorPalette(1.0, 3.0, 10.0);

    public BikeSafetyEdgeRenderer() {
    }

    @Override
    public boolean renderEdge(Edge e, EdgeVisualAttributes attrs) {
        // TODO Add bike-rental station street links
        if (e instanceof PlainStreetEdge) {
            PlainStreetEdge pse = (PlainStreetEdge) e;
            if (pse.getPermission().allows(TraverseMode.BICYCLE)) {
                double bikeSafety = pse.getBicycleSafetyEffectiveLength() / pse.getLength();
                attrs.label = String.format("%.02f", bikeSafety);
                attrs.color = palette.getColor(bikeSafety);
            } else {
                attrs.color = Color.LIGHT_GRAY;
                attrs.label = "no bikes";
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean renderVertex(Vertex v, VertexVisualAttributes attrs) {
        if (v instanceof BikeRentalStationVertex)
            attrs.color = Color.GREEN;
        else
            attrs.color = Color.DARK_GRAY;
        return true;
    }

}