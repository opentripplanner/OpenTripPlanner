package org.opentripplanner.framework.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFactory;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;

class SphericalDistanceLibraryTest {

  @Test
  void testLineStringLength() {
    Coordinate[] coordinates = new Coordinate[5];

    coordinates[0] = new Coordinate(0, 0);
    coordinates[1] = new Coordinate(0, 1.0 / 60);
    coordinates[2] = new Coordinate(0, 2.0 / 60);
    coordinates[3] = new Coordinate(0, 1.0 / 60);
    coordinates[4] = new Coordinate(0, 0);

    GeometryFactory geometryFactory = GeometryUtils.getGeometryFactory();
    CoordinateSequenceFactory coordinateSequenceFactory =
      geometryFactory.getCoordinateSequenceFactory();
    CoordinateSequence sequence = coordinateSequenceFactory.create(coordinates);
    LineString line = new LineString(sequence, geometryFactory);
    double length = SphericalDistanceLibrary.length(line);

    // A nautical mile is 1852 meters, and is defined as one 1/60th of a degree longitude on the
    // equator. As this is an approximate calculation, we expect the real value to be within a
    // 10 meters tolerance of this
    assertEquals(4 * 1852, length, 10);
  }

  @Test
  void testFastDistance_pointToSegment_perpendicularProjection() {
    // Horizontal segment at Oslo latitude
    Coordinate segmentStart = new Coordinate(10.70, 59.9);
    Coordinate segmentEnd = new Coordinate(10.80, 59.9);

    // Point directly north of segment midpoint
    // At 59.9°N, 1° latitude ≈ 111 km
    // 0.01° latitude ≈ 1.11 km ≈ 1110 meters
    Coordinate point = new Coordinate(10.75, 59.91);

    double distance = SphericalDistanceLibrary.fastDistance(point, segmentStart, segmentEnd);

    // Expected: ~1110 meters (perpendicular distance to midpoint)
    // Allow 50m tolerance for approximation
    assertEquals(1110, distance, 50);
  }

  @Test
  void testFastDistance_pointToSegment_closestPointIsStart() {
    // Segment from west to east
    Coordinate segmentStart = new Coordinate(10.70, 59.9);
    Coordinate segmentEnd = new Coordinate(10.80, 59.9);

    // Point west of segment start
    Coordinate point = new Coordinate(10.65, 59.9);

    double distance = SphericalDistanceLibrary.fastDistance(point, segmentStart, segmentEnd);
    double expectedDistance = SphericalDistanceLibrary.fastDistance(point, segmentStart);

    // Should be distance to start point (projection clamped to t=0)
    assertEquals(expectedDistance, distance, 1.0);
  }

  @Test
  void testFastDistance_pointToSegment_closestPointIsEnd() {
    // Segment from west to east
    Coordinate segmentStart = new Coordinate(10.70, 59.9);
    Coordinate segmentEnd = new Coordinate(10.80, 59.9);

    // Point east of segment end
    Coordinate point = new Coordinate(10.85, 59.9);

    double distance = SphericalDistanceLibrary.fastDistance(point, segmentStart, segmentEnd);
    double expectedDistance = SphericalDistanceLibrary.fastDistance(point, segmentEnd);

    // Should be distance to end point (projection clamped to t=1)
    assertEquals(expectedDistance, distance, 1.0);
  }

  @Test
  void testFastDistance_pointToSegment_pointOnSegment() {
    // Segment from Oslo Center to Oslo North
    Coordinate segmentStart = new Coordinate(10.7522, 59.9139);
    Coordinate segmentEnd = new Coordinate(10.7922, 59.9549);

    // Point exactly on the segment (midpoint)
    Coordinate point = new Coordinate(
      (segmentStart.x + segmentEnd.x) / 2,
      (segmentStart.y + segmentEnd.y) / 2
    );

    double distance = SphericalDistanceLibrary.fastDistance(point, segmentStart, segmentEnd);

    // Distance should be ~0 (within rounding error)
    assertEquals(0, distance, 1.0);
  }

  @Test
  void testFastDistance_pointToSegment_verticalSegment() {
    // Vertical segment (same longitude, different latitude)
    Coordinate segmentStart = new Coordinate(10.75, 59.9);
    Coordinate segmentEnd = new Coordinate(10.75, 60.0);

    // Point east of segment midpoint
    Coordinate point = new Coordinate(10.76, 59.95);

    double distance = SphericalDistanceLibrary.fastDistance(point, segmentStart, segmentEnd);

    // At 59.95°N, 0.01° longitude ≈ 560 meters
    // Expected: ~560 meters perpendicular distance
    assertEquals(560, distance, 50);
  }

  @Test
  void testFastDistance_pointToSegment_diagonalSegment() {
    // Diagonal segment (northeast direction)
    Coordinate segmentStart = new Coordinate(10.70, 59.90);
    Coordinate segmentEnd = new Coordinate(10.80, 60.00);

    // Point southeast of segment (below the line)
    Coordinate point = new Coordinate(10.75, 59.92);

    double distance = SphericalDistanceLibrary.fastDistance(point, segmentStart, segmentEnd);

    // Should be perpendicular distance to the line
    // Exact value depends on projection; verify it's reasonable
    assertTrue(distance > 0, "Distance should be positive");
    assertTrue(distance < 50000, "Distance should be less than 50km for this geometry");
  }

  @Test
  void testFastDistance_pointToSegment_degenerateSegment() {
    // Degenerate case: segment start equals segment end
    Coordinate segmentPoint = new Coordinate(10.75, 59.9);
    Coordinate point = new Coordinate(10.76, 59.91);

    double distance = SphericalDistanceLibrary.fastDistance(point, segmentPoint, segmentPoint);
    double expectedDistance = SphericalDistanceLibrary.fastDistance(point, segmentPoint);

    // Should fall back to point-to-point distance
    assertEquals(expectedDistance, distance, 0.1);
  }

  @Test
  void testFastDistance_pointToSegment_veryShortSegment() {
    // Very short segment (1 meter)
    Coordinate segmentStart = new Coordinate(10.75000, 59.90000);
    // ~1 meter east
    Coordinate segmentEnd = new Coordinate(10.75001, 59.90000);

    // Point 100 meters north
    Coordinate point = new Coordinate(10.75000, 59.90090);

    double distance = SphericalDistanceLibrary.fastDistance(point, segmentStart, segmentEnd);

    // Should be approximately 100 meters (perpendicular to short segment)
    assertEquals(100, distance, 10);
  }

  @Test
  void testFastDistance_pointToSegment_longSegment() {
    // Long segment (~70 km)
    Coordinate segmentStart = new Coordinate(10.70, 59.90);
    Coordinate segmentEnd = new Coordinate(10.70, 60.50);

    // Point 1 km east of midpoint
    Coordinate point = new Coordinate(10.71, 60.20);

    double distance = SphericalDistanceLibrary.fastDistance(point, segmentStart, segmentEnd);

    // At 60.2°N, 0.01° longitude ≈ 550 meters
    // Expected: ~550 meters perpendicular distance
    assertEquals(550, distance, 100);
  }
}
