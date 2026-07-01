package org.opentripplanner.street.linking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.street.geometry.GeometryUtils;
import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.edge.Area;
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
  void containsInteriorPoint() {
    assertTrue(area.containsPoint(new Coordinate(1, 1)));
  }

  @Test
  void doesNotContainPointInHole() {
    // The 2x2 hole is centred at x,y in (4,6); a point in its middle is not inside the area.
    assertFalse(area.containsPoint(new Coordinate(5, 5)));
  }

  @Test
  void doesNotContainExteriorPoint() {
    assertFalse(area.containsPoint(new Coordinate(11, 11)));
  }

  @Test
  void exposesWrappedAreaGroup() {
    var ag = new AreaGroup(SQUARE_WITH_HOLE);
    assertSame(ag, new PreparedAreaGroup(ag).areaGroup());
  }

  @Test
  void singleAreaGroupReturnsItsOnlyArea() {
    var only = area(square(0, 10));
    var g = new AreaGroup(square(0, 10));
    g.addArea(only);
    assertEquals(
      List.of(only),
      new PreparedAreaGroup(g).areasCrossedBy(GeometryUtils.makeLineString(1, 5, 4, 5))
    );
  }

  @Test
  void edgeWithinOneSubAreaReturnsThatAreaOnly() {
    // Two overlapping squares: friendly x in [0,10], steps x in [6,16]. x in [1,4] crosses only the
    // first.
    var friendly = area(square(0, 10));
    var steps = area(square(6, 16));
    var g = group(friendly, steps);
    assertEquals(
      List.of(friendly),
      new PreparedAreaGroup(g).areasCrossedBy(GeometryUtils.makeLineString(1, 5, 4, 5))
    );
  }

  @Test
  void edgeCrossingTwoSubAreasReturnsBoth() {
    // x in [2,12] crosses both squares, in list order.
    var friendly = area(square(0, 10));
    var steps = area(square(6, 16));
    var g = group(friendly, steps);
    assertEquals(
      List.of(friendly, steps),
      new PreparedAreaGroup(g).areasCrossedBy(GeometryUtils.makeLineString(2, 5, 12, 5))
    );
  }

  @Test
  void edgeTouchingNeighbourOnlyAtEndpointExcludesIt() {
    // Tile A x in [0,10] and tile B x in [10,20] share only the boundary line x=10 (the squares abut,
    // they do not overlap). The edge (5,5)->(10,5) lies inside A and merely touches B at its endpoint
    // (10,5). An un-shrunk intersects would wrongly count B; the shrink must drop that endpoint touch
    // so only A is returned.
    var a = area(square(0, 10));
    var b = area(square(10, 20));
    var g = new AreaGroup(square(0, 20));
    g.addArea(a);
    g.addArea(b);
    assertEquals(
      List.of(a),
      new PreparedAreaGroup(g).areasCrossedBy(GeometryUtils.makeLineString(5, 5, 10, 5))
    );
  }

  private static AreaGroup group(Area... areas) {
    var g = new AreaGroup(square(0, 16));
    for (Area a : areas) {
      g.addArea(a);
    }
    return g;
  }

  private static Area area(Polygon geometry) {
    var a = new Area();
    a.setGeometry(geometry);
    a.setName(I18NString.of("area"));
    a.setPermission(StreetTraversalPermission.PEDESTRIAN);
    return a;
  }

  private static Polygon square(double minX, double maxX) {
    return GF.createPolygon(
      new Coordinate[] {
        new Coordinate(minX, 0),
        new Coordinate(maxX, 0),
        new Coordinate(maxX, 10),
        new Coordinate(minX, 10),
        new Coordinate(minX, 0),
      }
    );
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
