package org.opentripplanner.ext.debugrastertiles;

import java.awt.Color;
import java.util.Optional;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.EdgeVertexRenderer;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.EdgeVisualAttributes;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.VertexVisualAttributes;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;

/**
 * Render walk safety for each edge using a color palette. Display the walk safety factor as label.
 */
public class WalkSafetyEdgeRenderer implements EdgeVertexRenderer {

  private final ScalarColorPalette palette = new DefaultScalarColorPalette(1.0, 3.0, 10.0);

  public WalkSafetyEdgeRenderer() {}

  @Override
  public Optional<EdgeVisualAttributes> renderEdge(Edge e) {
    if (e instanceof StreetEdge pse) {
      if (pse.getPermission().allows(TraverseMode.WALK)) {
        double walkSafety = pse.getWalkSafetyFactor();
        return EdgeVisualAttributes.optional(
          palette.getColor(walkSafety),
          "%.02f".formatted(walkSafety)
        );
      } else {
        return EdgeVisualAttributes.optional(Color.LIGHT_GRAY, "no walking");
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<VertexVisualAttributes> renderVertex(Vertex v) {
    if (v instanceof IntersectionVertex) {
      return VertexVisualAttributes.optional(Color.DARK_GRAY, null);
    }
    return Optional.empty();
  }

  @Override
  public String getName() {
    return "Walk safety";
  }
}
