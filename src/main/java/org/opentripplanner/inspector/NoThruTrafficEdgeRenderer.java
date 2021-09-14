package org.opentripplanner.inspector;

import java.awt.Color;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.EdgeVertexRenderer;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.EdgeVisualAttributes;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.VertexVisualAttributes;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

/**
 * Render no thru traffic restrictions for each street edge, along with a label describing the
 * restriction.
 */
public class NoThruTrafficEdgeRenderer implements EdgeVertexRenderer {

    public NoThruTrafficEdgeRenderer() {
    }

    @Override
    public boolean renderEdge(Edge e, EdgeVisualAttributes attrs) {
        if (e instanceof StreetEdge) {
            int [][] colors = {
                { 200, 200, 200 }, // no limitations = light gray
                { 200, 200, 0},    // no walk thru traffic = yellow
                { 0, 200, 200 },   // no bike thru = cyan
                { 0, 200, 0 },     // no walk & bike thru = green
                { 0, 0, 200},      // no car thru = blue
                { 200, 100, 0},    // no car & walk thru = orange
                { 200, 0, 200 },   // no car & bike thru = purple
                { 200, 0, 0 }      // no for all = red
            };

            StreetEdge pse = (StreetEdge) e;
            int bits = 0;

            attrs.label = "";

            if (pse.isWalkNoThruTraffic()) {
                attrs.label = " walk ";
                bits += 1;
            }
            if (pse.isBicycleNoThruTraffic()) {
                attrs.label += " bike";
                bits += 2;
            }
            if (pse.isMotorVehicleNoThruTraffic()) {
                attrs.label += " car";
                bits += 4;
            }
            attrs.color = new Color(colors[bits][0], colors[bits][1], colors[bits][2]);
            if (!attrs.label.equals("")) {
                attrs.label = "No" + attrs.label + " thru traffic";
            }
        }
        else {
            return false;
        }
        return true;
    }

    @Override
    public boolean renderVertex(Vertex v, VertexVisualAttributes attrs) {
        return false;
    }

    @Override
    public String getName() {
        return "No thru traffic";
    }
}
