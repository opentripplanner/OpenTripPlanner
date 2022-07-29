package org.opentripplanner.inspector;

import java.awt.Color;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.EdgeVertexRenderer;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.EdgeVisualAttributes;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.VertexVisualAttributes;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;

/**
 * Render walk safety for each edge using a color palette. Display the walk safety factor as label.
 */
public class WalkSafetyEdgeRenderer implements EdgeVertexRenderer {

  private final ScalarColorPalette palette = new DefaultScalarColorPalette(1.0, 3.0, 10.0);

  public WalkSafetyEdgeRenderer() {}

  @Override
  public boolean renderEdge(Edge e, EdgeVisualAttributes attrs) {
    if (e instanceof StreetEdge pse) {
      if (pse.getPermission().allows(TraverseMode.WALK)) {
        double walkSafety = pse.getWalkSafetyFactor();
        attrs.color = palette.getColor(walkSafety);
        attrs.label = String.format("%.02f", walkSafety);
      } else {
        attrs.color = Color.LIGHT_GRAY;
        attrs.label = "no walking";
      }
    } else {
      return false;
    }
    return true;
  }

  @Override
  public boolean renderVertex(Vertex v, VertexVisualAttributes attrs) {
    if (v instanceof IntersectionVertex) {
      attrs.color = Color.DARK_GRAY;
    } else {
      return false;
    }
    return true;
  }

  @Override
  public String getName() {
    return "Walk safety";
  }
}
