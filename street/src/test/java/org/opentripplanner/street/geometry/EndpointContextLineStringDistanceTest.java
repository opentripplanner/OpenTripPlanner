package org.opentripplanner.street.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;

/**
 * Verifies that {@link EndpointContextLineString#squaredEquirectangularDistanceToPoint} (the
 * allocation-free walk used by the vertex linker) returns the same projected distance as the old JTS
 * path it replaced: {@code equirectangularProject(uncompact(...)).distance(point)}. The method
 * returns the squared distance, so the test compares its {@code sqrt} to the JTS distance.
 */
class EndpointContextLineStringDistanceTest {

  private static final GeometryFactory GF = new GeometryFactory();

  /** Reference value: faithful copy of the old {@code VertexLinker.distance()} computation. */
  private static double jtsReferenceDistance(
    double ax,
    double ay,
    double bx,
    double by,
    byte[] packed,
    boolean reverse,
    double px,
    double py,
    double xscale
  ) {
    LineString geom = EndpointContextLineString.uncompact(ax, ay, bx, by, packed, reverse);
    Coordinate[] coords = new Coordinate[geom.getNumPoints()];
    for (int i = 0; i < coords.length; i++) {
      Coordinate c = (Coordinate) geom.getCoordinateN(i).clone();
      c.x *= xscale;
      coords[i] = c;
    }
    LineString transformed = GF.createLineString(coords);
    Point p = GF.createPoint(new Coordinate(px * xscale, py));
    return transformed.distance(p);
  }

  private static byte[] pack(double ax, double ay, double bx, double by, double... intermediate) {
    Coordinate[] full = new Coordinate[intermediate.length / 2 + 2];
    full[0] = new Coordinate(ax, ay);
    for (int i = 0; i < intermediate.length / 2; i++) {
      full[i + 1] = new Coordinate(intermediate[i * 2], intermediate[i * 2 + 1]);
    }
    full[full.length - 1] = new Coordinate(bx, by);
    LineString ls = GF.createLineString(full);
    return EndpointContextLineString.compact(ax, ay, bx, by, ls, false);
  }

  @Test
  void straightLine() {
    byte[] packed = pack(0.0, 0.0, 1.0, 1.0);
    double xscale = 0.5;
    // Point off the segment.
    assertParity(0.0, 0.0, 1.0, 1.0, packed, false, 0.4, 0.6, xscale);
    // Point exactly on the segment.
    assertParity(0.0, 0.0, 1.0, 1.0, packed, false, 0.5, 0.5, xscale);
    // Point beyond an endpoint (clamps to the end).
    assertParity(0.0, 0.0, 1.0, 1.0, packed, false, 2.0, 2.0, xscale);
    // Point at a vertex.
    assertParity(0.0, 0.0, 1.0, 1.0, packed, false, 0.0, 0.0, xscale);
  }

  @Test
  void singleIntermediatePoint() {
    byte[] packed = pack(0.0, 0.0, 2.0, 0.0, 1.0, 0.5);
    double xscale = Math.cos(Math.toRadians(59.9));
    assertParity(0.0, 0.0, 2.0, 0.0, packed, false, 1.0, 0.0, xscale);
    assertParity(0.0, 0.0, 2.0, 0.0, packed, false, 1.0, 0.5, xscale);
    assertParity(0.0, 0.0, 2.0, 0.0, packed, false, -1.0, -1.0, xscale);
  }

  @Test
  void multiSegment() {
    byte[] packed = pack(
      10.0,
      59.0,
      10.01,
      59.01,
      10.002,
      59.001,
      10.004,
      59.003,
      10.006,
      59.002,
      10.008,
      59.008
    );
    double xscale = Math.cos(Math.toRadians(59.0));
    assertParity(10.0, 59.0, 10.01, 59.01, packed, false, 10.005, 59.0045, xscale);
    assertParity(10.0, 59.0, 10.01, 59.01, packed, false, 10.0, 59.01, xscale);
  }

  @Test
  void reverseSeedsFromCorrectEndpoint() {
    // Encode with reverse=true: endpoints (B,A), as a back edge would.
    Coordinate[] full = {
      new Coordinate(2.0, 0.0),
      new Coordinate(1.5, 0.4),
      new Coordinate(1.0, 0.1),
      new Coordinate(0.0, 0.0),
    };
    LineString ls = GF.createLineString(full);
    byte[] packed = EndpointContextLineString.compact(0.0, 0.0, 2.0, 0.0, ls, true);
    double xscale = 0.7;
    assertParity(0.0, 0.0, 2.0, 0.0, packed, true, 1.2, 0.3, xscale);
    assertParity(0.0, 0.0, 2.0, 0.0, packed, true, 0.5, 0.5, xscale);
  }

  @Test
  void degenerateRepeatedPoint() {
    // Two consecutive intermediate points rounding to the same fixed-point cell.
    byte[] packed = pack(0.0, 0.0, 1.0, 0.0, 0.5, 0.0000001, 0.5, 0.0000002);
    double xscale = 0.9;
    assertParity(0.0, 0.0, 1.0, 0.0, packed, false, 0.5, 0.001, xscale);
  }

  private static void assertParity(
    double ax,
    double ay,
    double bx,
    double by,
    byte[] packed,
    boolean reverse,
    double px,
    double py,
    double xscale
  ) {
    double expected = jtsReferenceDistance(ax, ay, bx, by, packed, reverse, px, py, xscale);
    double got = Math.sqrt(
      EndpointContextLineString.squaredEquirectangularDistanceToPoint(
        ax,
        ay,
        bx,
        by,
        packed,
        reverse,
        px,
        py,
        xscale
      )
    );
    assertEquals(expected, got, 1e-12);
  }
}
