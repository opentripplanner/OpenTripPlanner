package org.opentripplanner.street.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.street.geometry.GeometryUtils.makeLineString;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

public class LineStringConcatenationTest {

  @Test
  void simpleConcatenate() {
    var line = GeometryUtils.concatenateLineStrings(
      List.of(makeLineString(0, 0, 1, 1), makeLineString(2, 2, 3, 3))
    );

    assertEquals("LINESTRING (0 0, 1 1, 2 2, 3 3)", line.toString());
  }

  @Test
  void deduplicate() {
    var line = GeometryUtils.concatenateLineStrings(
      List.of(makeLineString(0, 0, 1, 1), makeLineString(1, 1, 2, 2, 3, 3))
    );

    assertEquals("LINESTRING (0 0, 1 1, 2 2, 3 3)", line.toString());
  }

  @Test
  void manyLineStrings() {
    var line = GeometryUtils.concatenateLineStrings(
      List.of(makeLineString(0, 0, 1, 1), makeLineString(2, 2, 3, 3), makeLineString(4, 4, 5, 5))
    );

    assertEquals("LINESTRING (0 0, 1 1, 2 2, 3 3, 4 4, 5 5)", line.toString());
  }

  @Test
  void singleCoordinate() {
    var line = GeometryUtils.concatenateLineStrings(List.of(makeLineString(0, 0, 0, 0)));

    assertEquals("LINESTRING (0 0, 0 0)", line.toString());
  }

  @Test
  void severalDuplicates() {
    var line = GeometryUtils.concatenateLineStrings(
      List.of(makeLineString(0, 0, 0, 0, 1, 1, 1, 1), makeLineString(1, 1, 2, 2, 3, 3, 3, 3))
    );

    assertEquals("LINESTRING (0 0, 0 0, 1 1, 1 1, 2 2, 3 3, 3 3)", line.toString());
  }

  @Test
  void nullsAreSkipped() {
    var line = GeometryUtils.concatenateLineStrings(
      Arrays.asList(null, makeLineString(0, 0, 1, 1), null)
    );
    assertEquals("LINESTRING (0 0, 1 1)", line.toString());
  }

  @Test
  void emptyLineStrings() {
    var line = GeometryUtils.concatenateLineStrings(List.of());
    assertEquals("LINESTRING EMPTY", line.toString());
  }

  @Test
  void concatenateLineStringsWithSameFromToTest() {
    Coordinate[] coordinates = new Coordinate[4];

    coordinates[0] = new Coordinate(0, 0);
    coordinates[1] = new Coordinate(0, 1);
    coordinates[2] = new Coordinate(0, 1);
    coordinates[3] = new Coordinate(0, 2);

    LineString line = GeometryUtils.concatenateLineStrings(
      List.of(
        makeLineString(coordinates[0], coordinates[1]),
        makeLineString(coordinates[2], coordinates[3])
      )
    );

    assertEquals(3, line.getCoordinates().length);
  }

  @Test
  void concatenateLineStringsWithDifferentFromToTest() {
    Coordinate[] coordinates = new Coordinate[4];

    coordinates[0] = new Coordinate(0, 0);
    coordinates[1] = new Coordinate(0, 1);
    coordinates[2] = new Coordinate(1, 1);
    coordinates[3] = new Coordinate(1, 2);

    LineString line = GeometryUtils.concatenateLineStrings(
      List.of(
        makeLineString(coordinates[0], coordinates[1]),
        makeLineString(coordinates[2], coordinates[3])
      )
    );

    assertEquals(4, line.getCoordinates().length);
  }
}
