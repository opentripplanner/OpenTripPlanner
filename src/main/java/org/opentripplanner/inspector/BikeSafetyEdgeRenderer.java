package org.opentripplanner.inspector;

import java.awt.Color;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.EdgeVertexRenderer;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.EdgeVisualAttributes;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.VertexVisualAttributes;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetVehicleRentalLink;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.TraverseMode;

/**
 * Render bike safety for each edge using a color palette. Display the bike safety factor as label.
 *
 * @author laurent
 */
public class BikeSafetyEdgeRenderer implements EdgeVertexRenderer {

  private static final Color VEHICLE_RENTAL_COLOR_VERTEX = new Color(0.0f, 0.7f, 0.0f);
  private final ScalarColorPalette palette = new DefaultScalarColorPalette(1.0, 3.0, 10.0);

  public BikeSafetyEdgeRenderer() {}

  @Override
  public boolean renderEdge(Edge e, EdgeVisualAttributes attrs) {
    if (e instanceof StreetEdge) {
      StreetEdge pse = (StreetEdge) e;
      if (pse.getPermission().allows(TraverseMode.BICYCLE)) {
        double bikeSafety = pse.getBicycleSafetyFactor();
        attrs.color = palette.getColor(bikeSafety);
        attrs.label = String.format("%.02f", bikeSafety);
      } else {
        attrs.color = Color.LIGHT_GRAY;
        attrs.label = "no bikes";
      }
    } else if (e instanceof StreetVehicleRentalLink) {
      attrs.color = palette.getColor(1.0f);
      attrs.label = "link";
    } else {
      return false;
    }
    return true;
  }

  @Override
  public boolean renderVertex(Vertex v, VertexVisualAttributes attrs) {
    if (v instanceof VehicleRentalPlaceVertex) {
      attrs.color = VEHICLE_RENTAL_COLOR_VERTEX;
      attrs.label = v.getDefaultName();
    } else if (v instanceof IntersectionVertex) {
      attrs.color = Color.DARK_GRAY;
    } else {
      return false;
    }
    return true;
  }

  @Override
  public String getName() {
    return "Bike safety";
  }
}
