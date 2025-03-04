package org.opentripplanner.framework.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
