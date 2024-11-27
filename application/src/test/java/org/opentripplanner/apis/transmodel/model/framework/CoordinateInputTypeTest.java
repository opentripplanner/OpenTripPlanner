package org.opentripplanner.apis.transmodel.model.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.geometry.WgsCoordinate;

class CoordinateInputTypeTest {

  private static final double LATITUDE_VALUE = 64.5;
  private static final double LONGITUDE_VALUE = 11.0;
  private static final WgsCoordinate COORDINATE = new WgsCoordinate(
    LATITUDE_VALUE,
    LONGITUDE_VALUE
  );

  @Test
  void mapToWsgCoordinate() {
    assertEquals(
      new WgsCoordinate(LATITUDE_VALUE, LONGITUDE_VALUE),
      CoordinateInputType
        .mapToWsgCoordinate("c", Map.of("c", CoordinateInputType.mapForTest(COORDINATE)))
        .get()
    );
  }

  @Test
  void mapToWsgCoordinateWithMissingLongitude() {
    var ex = assertThrows(
      IllegalArgumentException.class,
      () ->
        CoordinateInputType.mapToWsgCoordinate(
          "c",
          Map.of("c", Map.ofEntries(Map.entry(CoordinateInputType.LONGITUDE, LONGITUDE_VALUE)))
        )
    );
    assertEquals("The 'latitude' parameter is required.", ex.getMessage());
  }

  @Test
  void mapToWsgCoordinateWithMissingLatitude() {
    var ex = assertThrows(
      IllegalArgumentException.class,
      () ->
        CoordinateInputType.mapToWsgCoordinate(
          "c",
          Map.of("c", Map.ofEntries(Map.entry(CoordinateInputType.LATITUDE, LATITUDE_VALUE)))
        )
    );
    assertEquals("The 'longitude' parameter is required.", ex.getMessage());
  }
}
