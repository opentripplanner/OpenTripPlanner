package org.opentripplanner.apis.transmodel.model.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
  void mapToWgsCoordinate() {
    assertEquals(
      new WgsCoordinate(LATITUDE_VALUE, LONGITUDE_VALUE),
      CoordinateInputType.mapToWgsCoordinate(
        "c",
        Map.of("c", CoordinateInputType.mapForTest(COORDINATE))
      ).get()
    );
  }

  @Test
  void mapEmptyCoordinateToNull() {
    assertTrue(CoordinateInputType.mapToWgsCoordinate("c", Map.of()).isEmpty());
  }
}
