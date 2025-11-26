package org.opentripplanner.inspector.vector.edge;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.ElevatorHopEdge;
import org.opentripplanner.street.model.vertex.Vertex;

class EdgeGeometryMapper {

  static Geometry map(Edge edge) {
    return switch (edge) {
      case ElevatorHopEdge e -> {
        // If coordinates are equal, move the other one slightly to make the edge visible
        final Vertex toVertex = e.getToVertex();
        var toCoord = toVertex.getCoordinate();
        var fromV = e.getFromVertex();
        if (fromV.getCoordinate().equals(toCoord)) {
          Coordinate newTo = toCoord.copy();
          newTo.setX(toCoord.getX() + 0.000001);
          yield GeometryUtils.makeLineString(fromV.getCoordinate(), newTo);
        } else {
          yield GeometryUtils.makeLineString(fromV.getCoordinate(), toVertex.getCoordinate());
        }
      }
      default -> edge.getGeometry();
    };
  }
}
