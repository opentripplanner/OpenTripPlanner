package org.opentripplanner.inspector.vector.edge;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorAlightEdge;
import org.opentripplanner.street.model.edge.ElevatorBoardEdge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.vertex.Vertex;

class EdgeGeometryMapper {

  static Geometry map(Edge edge) {
    return switch (edge) {
      case ElevatorHopEdge e -> makeElevatorLineString(e);
      case ElevatorBoardEdge e -> makeElevatorLineString(e);
      case ElevatorAlightEdge e -> makeElevatorLineString(e);
      default -> edge.getGeometry();
    };
  }

  private static LineString makeElevatorLineString(Edge e) {
    // If coordinates are equal, move the other one slightly to make the edge visible.
    final Vertex toVertex = e.getToVertex();
    var toCoord = toVertex.getCoordinate();
    var fromV = e.getFromVertex();
    if (fromV.getCoordinate().equals(toCoord)) {
      Coordinate newTo = toCoord.copy();
      newTo.setX(toCoord.getX() + 0.000001);
      return GeometryUtils.makeLineString(fromV.getCoordinate(), newTo);
    } else {
      return GeometryUtils.makeLineString(fromV.getCoordinate(), toVertex.getCoordinate());
    }
  }
}
