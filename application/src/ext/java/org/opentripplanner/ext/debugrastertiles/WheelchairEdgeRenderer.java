package org.opentripplanner.ext.debugrastertiles;

import java.awt.Color;
import java.util.Optional;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.EdgeVertexRenderer;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.EdgeVisualAttributes;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.VertexVisualAttributes;
import org.opentripplanner.routing.api.request.preference.WheelchairPreferences;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Render important information for debugging wheelchair access (street slopes and transit stop
 * accessibility)
 *
 * @author hannesj
 */
public class WheelchairEdgeRenderer implements EdgeVertexRenderer {

  private static final Color NO_WHEELCHAIR_COLOR = Color.RED;
  private static final Color YES_WHEELCHAIR_COLOR = Color.GREEN;
  private static final Color NO_WHEELCHAIR_INFORMATION_COLOR = Color.ORANGE;
  private final ScalarColorPalette slopePalette;

  public WheelchairEdgeRenderer(WheelchairPreferences wheelchairPreferences) {
    this.slopePalette = new DefaultScalarColorPalette(0.0, wheelchairPreferences.maxSlope(), 1.0);
  }

  @Override
  public Optional<EdgeVisualAttributes> renderEdge(Edge e) {
    if (e instanceof StreetEdge pse) {
      if (!pse.isWheelchairAccessible()) {
        return EdgeVisualAttributes.optional(NO_WHEELCHAIR_COLOR, "wheelchair=no");
      } else {
        return EdgeVisualAttributes.optional(
          slopePalette.getColor(pse.getMaxSlope()),
          String.format("%.02f", pse.getMaxSlope())
        );
      }
    } else if (e instanceof ElevatorHopEdge ehe) {
      if (!ehe.isWheelchairAccessible()) {
        return EdgeVisualAttributes.optional(NO_WHEELCHAIR_COLOR, "wheelchair=no");
      } else {
        return EdgeVisualAttributes.optional(Color.GREEN, "elevator");
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<VertexVisualAttributes> renderVertex(Vertex v) {
    if (v instanceof TransitStopVertex) {
      var accessibility = ((TransitStopVertex) v).getStop().getWheelchairAccessibility();
      var color =
        switch (accessibility) {
          case NO_INFORMATION -> NO_WHEELCHAIR_INFORMATION_COLOR;
          case POSSIBLE -> YES_WHEELCHAIR_COLOR;
          case NOT_POSSIBLE -> NO_WHEELCHAIR_COLOR;
        };
      return VertexVisualAttributes.optional(color, v.getDefaultName());
    }
    return Optional.empty();
  }
}
