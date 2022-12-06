package org.opentripplanner.inspector.raster;

import java.awt.Color;
import java.util.Optional;
import org.opentripplanner.inspector.raster.EdgeVertexTileRenderer.EdgeVertexRenderer;
import org.opentripplanner.inspector.raster.EdgeVertexTileRenderer.EdgeVisualAttributes;
import org.opentripplanner.inspector.raster.EdgeVertexTileRenderer.VertexVisualAttributes;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.PathwayEdge;
import org.opentripplanner.street.model.vertex.StationElementVertex;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StationElement;

public class PathwayEdgeRenderer implements EdgeVertexRenderer {

  @Override
  public Optional<EdgeVisualAttributes> renderEdge(Edge e) {
    if (!(e instanceof PathwayEdge pwe)) {
      return Optional.empty();
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

    var color =
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
      color = color.darker();
    }

    return EdgeVisualAttributes.optional(color, sb.toString());
  }

  @Override
  public Optional<VertexVisualAttributes> renderVertex(Vertex v) {
    if (!(v instanceof StationElementVertex stationElementVertex)) {
      return Optional.empty();
    }
    StationElement<?, ?> stationElement = stationElementVertex.getStationElement();

    StringBuilder sb = new StringBuilder();

    sb.append(stationElement.getName());

    if (stationElement instanceof RegularStop stop && stop.getPlatformCode() != null) {
      sb.append(" [").append(stop.getPlatformCode()).append("]");
    }

    if (stationElement.getCode() != null) {
      sb.append(" (").append(stationElement.getCode()).append(")");
    }

    Color color =
      switch (stationElement.getClass().getSimpleName()) {
        case "Stop" -> Color.ORANGE;
        case "PathwayNode" -> new Color(217, 38, 145);
        case "Entrance" -> new Color(38, 184, 217);
        case "BoardingArea" -> Color.PINK;
        default -> Color.LIGHT_GRAY;
      };

    return VertexVisualAttributes.optional(color, sb.toString());
  }

  @Override
  public String getName() {
    return "Pathways";
  }
}
