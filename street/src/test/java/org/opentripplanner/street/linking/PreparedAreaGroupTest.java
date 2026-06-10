package org.opentripplanner.street.linking;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.edge.AreaGroup;

class PreparedAreaGroupTest {

  private static final GeometryFactory GF = GeometryUtils.getGeometryFactory();

  // A 10x10 square with a 2x2 hole in the centre (x,y in (4,6)).
  private static final Polygon SQUARE_WITH_HOLE = squareWithHole();

  private final PreparedAreaGroup area = new PreparedAreaGroup(new AreaGroup(SQUARE_WITH_HOLE));

  @Test
  void containsInteriorSegment() {
    assertTrue(area.containsSegment(new Coordinate(1, 1), new Coordinate(1, 9)));
  }

  @Test
  void containsBoundaryToBoundarySegmentThroughInterior() {
    // Visibility vertices sit on the area boundary; a connection between two of them that stays
    // inside the area must be accepted. Here x=2 runs from the bottom edge to the top edge, left of
    // the hole.
    assertTrue(area.containsSegment(new Coordinate(2, 0), new Coordinate(2, 10)));
  }

  @Test
  void doesNotContainHoleCrossingSegment() {
    // A horizontal segment at y=5 from the left edge to the right edge passes through the hole.
    assertFalse(area.containsSegment(new Coordinate(1, 5), new Coordinate(9, 5)));
  }

  @Test
  void doesNotContainExteriorSegment() {
    assertFalse(area.containsSegment(new Coordinate(11, 11), new Coordinate(12, 12)));
  }

  @Test
  void exposesWrappedAreaGroup() {
    var ag = new AreaGroup(SQUARE_WITH_HOLE);
    assertSame(ag, new PreparedAreaGroup(ag).areaGroup());
  }

  private static Polygon squareWithHole() {
    LinearRing shell = GF.createLinearRing(
      new Coordinate[] {
        new Coordinate(0, 0),
        new Coordinate(10, 0),
        new Coordinate(10, 10),
        new Coordinate(0, 10),
        new Coordinate(0, 0),
      }
    );
    LinearRing hole = GF.createLinearRing(
      new Coordinate[] {
        new Coordinate(4, 4),
        new Coordinate(6, 4),
        new Coordinate(6, 6),
        new Coordinate(4, 6),
        new Coordinate(4, 4),
      }
    );
    return GF.createPolygon(shell, new LinearRing[] { hole });
  }
}
