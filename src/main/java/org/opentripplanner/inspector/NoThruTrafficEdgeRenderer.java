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

    private static final Color THRU_TRAFFIC_COLOR = Color.LIGHT_GRAY;

    private static final Color NO_THRU_TRAFFIC_COLOR = Color.RED;

    private static final Color NO_BICYCLE_THRU_TRAFFIC_COLOR = Color.BLUE;

    private static final Color NO_CAR_THRU_TRAFFIC_COLOR = Color.ORANGE;

    public NoThruTrafficEdgeRenderer() {
    }

    @Override
    public boolean renderEdge(Edge e, EdgeVisualAttributes attrs) {
        if (e instanceof StreetEdge) {
            StreetEdge pse = (StreetEdge) e;
            if (pse.isBicycleNoThruTraffic() && pse.isMotorVehicleNoThruTraffic()) {
                attrs.color = NO_THRU_TRAFFIC_COLOR;
                attrs.label = "no thru traffic";
            }
            else if (pse.isBicycleNoThruTraffic()) {
                attrs.color = NO_BICYCLE_THRU_TRAFFIC_COLOR;
                attrs.label = "no bicycle thru traffic";
            }
            else if (pse.isMotorVehicleNoThruTraffic()) {
                attrs.color = NO_CAR_THRU_TRAFFIC_COLOR;
                attrs.label = "no car thru traffic";
            }
            else {
                attrs.color = THRU_TRAFFIC_COLOR;
                attrs.label = "";
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