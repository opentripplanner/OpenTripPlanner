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
            StreetEdge pse = (StreetEdge) e;
	    int r = 200, g = 0, b = 0;

	    attrs.label = "";

	    if (pse.isWalkNoThruTraffic()) {
                attrs.label = " walk ";
	    } else {
	        g = 200;
	    }
	    if (pse.isBicycleNoThruTraffic()) {
                attrs.label += " bike";
            } else {
	        b = 200;
	    }
            if (pse.isMotorVehicleNoThruTraffic()) {
                attrs.label += " car";
		r = 255;
            }
            attrs.color = new Color(r, g, b);
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
