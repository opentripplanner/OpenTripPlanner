package org.opentripplanner.inspector;

import java.awt.Color;
import java.util.Optional;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.EdgeVertexRenderer;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.EdgeVisualAttributes;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.VertexVisualAttributes;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.TransitEntranceVertex;
import org.opentripplanner.routing.vertextype.TransitPathwayNodeVertex;
import org.opentripplanner.util.I18NString;

public class PathwayEdgeRenderer implements EdgeVertexRenderer {

  @Override
  public boolean renderEdge(Edge e, EdgeVisualAttributes attrs) {
    if (e instanceof PathwayEdge pwe) {
      attrs.label =
        "distance=%s,time=%s".formatted(
            Math.round(pwe.getDistanceMeters()),
            pwe.getDistanceIndependentTime()
          );
      attrs.color = new Color(145, 217, 38);
      return true;
    }
    return false;
  }

  @Override
  public boolean renderVertex(Vertex v, VertexVisualAttributes attrs) {
    if (v instanceof TransitPathwayNodeVertex) {
      attrs.color = new Color(217, 38, 145);
      return true;
    } else if (v instanceof TransitEntranceVertex tev) {
      attrs.label = Optional.ofNullable(tev.getName()).map(I18NString::toString).orElse(null);
      attrs.color = new Color(38, 184, 217);
      return true;
    }
    return false;
  }

  @Override
  public String getName() {
    return "Pathways";
  }
}
