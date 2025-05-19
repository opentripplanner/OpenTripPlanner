package org.opentripplanner.ext.debugrastertiles;

import java.awt.Color;
import java.util.Optional;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.EdgeVertexRenderer;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.EdgeVisualAttributes;
import org.opentripplanner.ext.debugrastertiles.EdgeVertexTileRenderer.VertexVisualAttributes;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.edge.EscalatorEdge;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.IntersectionVertex;
import org.opentripplanner.street.model.vertex.OsmBoardingLocationVertex;
import org.opentripplanner.street.model.vertex.TransitBoardingAreaVertex;
import org.opentripplanner.street.model.vertex.TransitEntranceVertex;
import org.opentripplanner.street.model.vertex.TransitPathwayNodeVertex;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.street.model.vertex.Vertex;

/**
 * Render traversal permissions for each edge by color and label (walk, bicycle, car, stairs).
 *
 * @author laurent
 */
public class TraversalPermissionsEdgeRenderer implements EdgeVertexRenderer {

  private static final Color LINK_COLOR_EDGE = Color.ORANGE;

  private static final Color ELEVATOR_COLOR_EDGE = Color.YELLOW;

  private static final Color STAIRS_COLOR_EDGE = Color.PINK;

  private static final Color STREET_COLOR_VERTEX = Color.DARK_GRAY;

  private static final Color TRANSIT_STOP_COLOR_VERTEX = new Color(0.0f, 0.0f, 0.8f);

  private static final Color VEHICLE_RENTAL_COLOR_VERTEX = new Color(0.0f, 0.7f, 0.0f);

  private static final Color PARK_AND_RIDE_COLOR_VERTEX = Color.RED;

  private static final Color OSM_BOARDING_LOCATION_VERTEX_COLOR = new Color(23, 160, 234);

  private static final Color BARRIER_COLOR_VERTEX = new Color(0.5803922f, 0.21568628f, 0.24313726f);

  @Override
  public Optional<EdgeVisualAttributes> renderEdge(Edge e) {
    Color color;
    String label;

    if (e instanceof StreetEdge pse) {
      if (pse.isStairs()) {
        color = STAIRS_COLOR_EDGE;
        label = "stairs";
      } else {
        color = getColor(pse.getPermission());
        label = getLabel(pse.getPermission());
      }
      if (pse.isMotorVehicleNoThruTraffic()) {
        label += " car NTT";
      }
      if (pse.isBicycleNoThruTraffic()) {
        label += " bike NTT";
      }
      if (pse.isWalkNoThruTraffic()) {
        label += " walk NTT";
      }
    } else if (e instanceof ElevatorHopEdge ehe) {
      color = ELEVATOR_COLOR_EDGE;
      label = "elevator";
      if (ehe.isWheelchairAccessible()) {
        label += " wheelchair";
      }
      if (ehe.getPermission().allows(StreetTraversalPermission.BICYCLE)) {
        label += " bike";
      }
      if (ehe.getPermission().allows(StreetTraversalPermission.CAR)) {
        label += " car";
      }
    } else if (e instanceof EscalatorEdge) {
      color = STAIRS_COLOR_EDGE;
      label = "escalator";
    } else {
      color = LINK_COLOR_EDGE;
      label = "link";
    }
    return EdgeVisualAttributes.optional(color, label);
  }

  @Override
  public Optional<VertexVisualAttributes> renderVertex(Vertex v) {
    if (v instanceof OsmBoardingLocationVertex osmV) {
      return VertexVisualAttributes.optional(
        OSM_BOARDING_LOCATION_VERTEX_COLOR,
        "OSM refs" + osmV.references
      );
    } else if (v instanceof IntersectionVertex) {
      if (v instanceof BarrierVertex) {
        return VertexVisualAttributes.optional(BARRIER_COLOR_VERTEX, null);
      }
      return VertexVisualAttributes.optional(STREET_COLOR_VERTEX, null);
    } else if (
      v instanceof TransitStopVertex ||
      v instanceof TransitEntranceVertex ||
      v instanceof TransitPathwayNodeVertex ||
      v instanceof TransitBoardingAreaVertex
    ) {
      return VertexVisualAttributes.optional(TRANSIT_STOP_COLOR_VERTEX, v.getDefaultName());
    } else if (v instanceof VehicleRentalPlaceVertex) {
      return VertexVisualAttributes.optional(VEHICLE_RENTAL_COLOR_VERTEX, v.getDefaultName());
    } else if (v instanceof VehicleParkingEntranceVertex) {
      return VertexVisualAttributes.optional(PARK_AND_RIDE_COLOR_VERTEX, v.getDefaultName());
    }
    return Optional.empty();
  }

  private Color getColor(StreetTraversalPermission permissions) {
    /*
     * We use the trick that there are 3 main traversal modes (WALK, BIKE and CAR) and 3 color
     * channels (R, G, B).
     */
    float r = 0.2f;
    float g = 0.2f;
    float b = 0.2f;
    if (permissions.allows(StreetTraversalPermission.PEDESTRIAN)) {
      g += 0.5f;
    }
    if (permissions.allows(StreetTraversalPermission.BICYCLE)) {
      b += 0.5f;
    }
    if (permissions.allows(StreetTraversalPermission.CAR)) {
      r += 0.5f;
    }
    // TODO CUSTOM_VEHICLE (?)
    return new Color(r, g, b);
  }

  private String getLabel(StreetTraversalPermission permissions) {
    StringBuilder sb = new StringBuilder();
    if (permissions.allows(StreetTraversalPermission.PEDESTRIAN)) sb.append("walk,");
    if (permissions.allows(StreetTraversalPermission.BICYCLE)) sb.append("bike,");
    if (permissions.allows(StreetTraversalPermission.CAR)) sb.append("car,");
    if (sb.length() > 0) {
      sb.setLength(sb.length() - 1); // Remove last comma
    } else {
      sb.append("none");
    }
    return sb.toString();
  }
}
