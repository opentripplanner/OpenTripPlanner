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

    private static final Color [] colors = {
        new Color(200, 200, 200), // no limitations = light gray
        new Color(200, 200, 0),   // no walk thru traffic = yellow
        new Color(0, 200, 200),   // no bike thru = cyan
        new Color(0, 200, 0),     // no walk & bike thru = green
        new Color(0, 0, 200),     // no car thru = blue
        new Color(200, 100, 0),   // no car & walk thru = orange
        new Color(200, 0, 200),   // no car & bike thru = purple
        new Color(200, 0, 0)      // no for all = red
    };

    @Override
    public boolean renderEdge(Edge e, EdgeVisualAttributes attrs) {
        if (e instanceof StreetEdge) {
            StreetEdge pse = (StreetEdge) e;
            int colorIndex = 0;

            attrs.label = "";

            if (pse.isWalkNoThruTraffic()) {
                attrs.label = " walk ";
                colorIndex += 1;
            }
            if (pse.isBicycleNoThruTraffic()) {
                attrs.label += " bike";
                colorIndex += 2;
            }
            if (pse.isMotorVehicleNoThruTraffic()) {
                attrs.label += " car";
                colorIndex += 4;
            }
            attrs.color = colors[colorIndex];
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
