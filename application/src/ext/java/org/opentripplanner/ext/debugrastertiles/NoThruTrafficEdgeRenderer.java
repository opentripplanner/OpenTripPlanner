package org.opentripplanner.ext.debugrastertiles;

import java.awt.Color;
import java.util.Optional;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.EdgeVertexRenderer;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.EdgeVisualAttributes;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.VertexVisualAttributes;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Render no thru traffic restrictions for each street edge, along with a label describing the
 * restriction.
 */
public class NoThruTrafficEdgeRenderer implements EdgeVertexRenderer {

  private static final Color[] colors = {
    new Color(200, 200, 200), // no limitations = light gray
    new Color(200, 200, 0), // no walk thru traffic = yellow
    new Color(0, 200, 200), // no bike thru = cyan
    new Color(0, 200, 0), // no walk & bike thru = green
    new Color(0, 0, 200), // no car thru = blue
    new Color(200, 100, 0), // no car & walk thru = orange
    new Color(200, 0, 200), // no car & bike thru = purple
    new Color(200, 0, 0), // no for all = red
  };

  public NoThruTrafficEdgeRenderer() {}

  @Override
  public Optional<EdgeVisualAttributes> renderEdge(Edge e) {
    if (e instanceof StreetEdge pse) {
      int colorIndex = 0;

      String label = "";

      if (pse.isWalkNoThruTraffic()) {
        label = " walk ";
        colorIndex += 1;
      }
      if (pse.isBicycleNoThruTraffic()) {
        label += " bike";
        colorIndex += 2;
      }
      if (pse.isMotorVehicleNoThruTraffic()) {
        label += " car";
        colorIndex += 4;
      }
      if (!label.isEmpty()) {
        label = "No" + label + " thru traffic";
      }

      return EdgeVisualAttributes.optional(colors[colorIndex], label);
    }
    return Optional.empty();
  }

  @Override
  public Optional<VertexVisualAttributes> renderVertex(Vertex v) {
    return Optional.empty();
  }
}
