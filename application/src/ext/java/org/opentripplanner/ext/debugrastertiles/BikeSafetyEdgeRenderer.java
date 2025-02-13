package org.opentripplanner.ext.debugrastertiles;

import java.awt.Color;
import java.util.Optional;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.EdgeVertexRenderer;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.EdgeVisualAttributes;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.VertexVisualAttributes;
import org.opentripplanner.service.vehiclerental.street.StreetVehicleRentalLink;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
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
  public Optional<EdgeVisualAttributes> renderEdge(Edge e) {
    if (e instanceof StreetEdge pse) {
      if (pse.getPermission().allows(TraverseMode.BICYCLE)) {
        double bikeSafety = pse.getBicycleSafetyFactor();
        return EdgeVisualAttributes.optional(
          palette.getColor(bikeSafety),
          String.format("%.02f", bikeSafety)
        );
      } else {
        return EdgeVisualAttributes.optional(Color.LIGHT_GRAY, "no bikes");
      }
    } else if (e instanceof StreetVehicleRentalLink) {
      return EdgeVisualAttributes.optional(palette.getColor(1.0f), "link");
    }
    return Optional.empty();
  }

  @Override
  public Optional<VertexVisualAttributes> renderVertex(Vertex v) {
    if (v instanceof VehicleRentalPlaceVertex) {
      return VertexVisualAttributes.optional(VEHICLE_RENTAL_COLOR_VERTEX, v.getDefaultName());
    } else if (v instanceof IntersectionVertex) {
      return VertexVisualAttributes.optional(Color.DARK_GRAY, null);
    }
    return Optional.empty();
  }

  @Override
  public String getName() {
    return "Bike safety";
  }
}
