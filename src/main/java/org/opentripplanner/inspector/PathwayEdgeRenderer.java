package org.opentripplanner.inspector;

import java.awt.Color;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.EdgeVertexRenderer;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.EdgeVisualAttributes;
import org.opentripplanner.inspector.EdgeVertexTileRenderer.VertexVisualAttributes;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StationElement;

public class PathwayEdgeRenderer implements EdgeVertexRenderer {

  @Override
  public boolean renderEdge(Edge e, EdgeVisualAttributes attrs) {
    if (!(e instanceof PathwayEdge pwe)) {
      return false;
    }

    StringBuilder sb = new StringBuilder();

    if (!pwe.hasBogusName()) {
      sb.append("name=").append(pwe.getName()).append(", ");
    }

    if (pwe.getDistanceMeters() != 0) {
      sb.append("distance=").append(Math.round(pwe.getDistanceMeters())).append(", ");
    }

    if (pwe.getDistanceIndependentTime() != 0) {
      sb.append("time=").append(pwe.getDistanceIndependentTime()).append(", ");
    }

    if (pwe.getSteps() != 0) {
      sb.append("steps=").append(pwe.getSteps()).append(", ");
    }

    // remove last comma
    if (!sb.isEmpty()) {
      sb.setLength(sb.length() - 2);
    }

    attrs.label = sb.toString();

    attrs.color =
      switch (pwe.getMode()) {
        case WALKWAY -> new Color(145, 217, 38);
        case STAIRS -> Color.CYAN;
        case MOVING_SIDEWALK -> Color.GREEN;
        case ESCALATOR -> Color.BLUE;
        case ELEVATOR -> Color.PINK;
        case FARE_GATE -> Color.RED;
        case EXIT_GATE -> Color.MAGENTA;
        case UNKNOWN -> Color.GRAY;
      };

    if (!pwe.isWheelchairAccessible()) {
      attrs.color = attrs.color.darker();
    }

    return true;
  }

  @Override
  public boolean renderVertex(Vertex v, VertexVisualAttributes attrs) {
    StationElement stationElement = v.getStationElement();

    if (stationElement == null) {
      return false;
    }

    StringBuilder sb = new StringBuilder();

    sb.append(stationElement.getName());

    if (stationElement instanceof RegularStop stop && stop.getPlatformCode() != null) {
      sb.append(" [").append(stop.getPlatformCode()).append("]");
    }

    if (stationElement.getCode() != null) {
      sb.append(" (").append(stationElement.getCode()).append(")");
    }

    attrs.label = sb.toString();
    attrs.color =
      switch (stationElement.getClass().getSimpleName()) {
        case "Stop" -> Color.ORANGE;
        case "PathwayNode" -> new Color(217, 38, 145);
        case "Entrance" -> new Color(38, 184, 217);
        case "BoardingArea" -> Color.PINK;
        default -> Color.LIGHT_GRAY;
      };

    return true;
  }

  @Override
  public String getName() {
    return "Pathways";
  }
}
