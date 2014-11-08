package org.opentripplanner.inspector;

import org.opentripplanner.inspector.EdgeVertexTileRenderer.EdgeVertexRenderer;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitVertex;

import java.awt.Color;

/**
 * Render important information for debugging wheelchair access (street slopes and transit stop accessibility)
 *
 * @author hannesj
 */
public class WheelchairEdgeRenderer implements EdgeVertexRenderer {


    private ScalarColorPalette slopePalette = new DefaultScalarColorPalette(0.0, 0.08, 1.0);


    private static final Color NO_WHEELCHAIR_COLOR = Color.RED;
    private static final Color YES_WHEELCHAIR_COLOR = Color.GREEN;
    private static final Color NO_WHEELCHAIR_INFORMATION_COLOR = Color.ORANGE;


    public WheelchairEdgeRenderer() {
    }

    @Override
    public boolean renderEdge(Edge e, EdgeVertexTileRenderer.EdgeVisualAttributes attrs) {
        if (e instanceof StreetEdge) {
            StreetEdge pse = (StreetEdge) e;
            if (!pse.isWheelchairAccessible()) {
                attrs.color = NO_WHEELCHAIR_COLOR;
                attrs.label = "wheelchair=no";
            } else {
                attrs.color = slopePalette.getColor(pse.getMaxSlope());
                attrs.label = String.format("%.02f", pse.getMaxSlope());
            }
        } else {
            return false;
        }
        return true;
    }

    @Override
    public boolean renderVertex(Vertex v, EdgeVertexTileRenderer.VertexVisualAttributes attrs) {
        if (v instanceof TransitVertex) {
            if(((TransitVertex) v).getStop().getWheelchairBoarding() == 0)
                attrs.color = NO_WHEELCHAIR_INFORMATION_COLOR;
            if(((TransitVertex) v).getStop().getWheelchairBoarding() == 1)
                attrs.color = YES_WHEELCHAIR_COLOR;
            if(((TransitVertex) v).getStop().getWheelchairBoarding() == 2)
                attrs.color = NO_WHEELCHAIR_COLOR;
            attrs.label = v.getName();
        } else  {
            return false;
        }
        return true;
    }

    @Override
    public String getName() {
        return "Wheelchair access";
    }
}
