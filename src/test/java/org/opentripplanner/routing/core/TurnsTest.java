package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model._data.StreetModelForTest;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.model.edge.StreetEdgeBuilder;
import org.opentripplanner.street.model.vertex.IntersectionVertex;

public class TurnsTest {

  @Test
  public void testIntersectionVertex() {
    GeometryFactory gf = new GeometryFactory();

    LineString geometry = gf.createLineString(
      new Coordinate[] { new Coordinate(-0.10, 0), new Coordinate(0, 0) }
    );

    IntersectionVertex v1 = StreetModelForTest.intersectionVertex("v1", -0.10, 0);
    IntersectionVertex v2 = StreetModelForTest.intersectionVertex("v2", 0, 0);

    StreetEdge leftEdge = new StreetEdgeBuilder<>()
      .withFromVertex(v1)
      .withToVertex(v2)
      .withGeometry(geometry)
      .withName("morx")
      .withMeterLength(10.0)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(true)
      .buildAndConnect();

    LineString geometry2 = gf.createLineString(
      new Coordinate[] { new Coordinate(0, 0), new Coordinate(-0.10, 0) }
    );

    StreetEdge rightEdge = new StreetEdgeBuilder<>()
      .withFromVertex(v1)
      .withToVertex(v2)
      .withGeometry(geometry2)
      .withName("fleem")
      .withMeterLength(10.0)
      .withPermission(StreetTraversalPermission.ALL)
      .withBack(false)
      .buildAndConnect();

    assertEquals(180, Math.abs(leftEdge.getOutAngle() - rightEdge.getOutAngle()));
  }
}
