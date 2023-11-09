package org.opentripplanner.transit.model.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.timetable.Trip;

class StopPatternTest {

  private final TransitModelForTest testModel = TransitModelForTest.of();

  @Test
  void boardingAlightingConditions() {
    // We have different types of stops, of which only regular stops should allow boarding/alighting
    var s1 = testModel.stop("1", 60.0, 11.0).build();
    var s2 = testModel.stop("2", 61.0, 11.0).build();
    var s3 = testModel.stop("3", 62.0, 11.0).build();
    var s4 = testModel.stop("4", 62.1, 11.0).build();

    var s34 = testModel.groupStopForTest("3_4", List.of(s3, s4));

    var areaStop = testModel.areaStopForTest(
      "area",
      GeometryUtils
        .getGeometryFactory()
        .createPolygon(
          new Coordinate[] {
            new Coordinate(11.0, 63.0),
            new Coordinate(11.5, 63.0),
            new Coordinate(11.5, 63.5),
            new Coordinate(11.0, 63.5),
            new Coordinate(11.0, 63.0),
          }
        )
    );

    Trip t = TransitModelForTest.trip("trip").build();

    StopPattern stopPattern = new StopPattern(
      List.of(
        testModel.stopTime(t, 0, s1),
        testModel.stopTime(t, 1, s2),
        testModel.stopTime(t, 2, s34),
        testModel.stopTime(t, 3, areaStop)
      )
    );

    assertTrue(stopPattern.canAlight(0), "Allowed at RegularStop");
    assertTrue(stopPattern.canAlight(1), "Allowed at RegularStop");
    assertFalse(stopPattern.canAlight(2), "Forbidden at GroupStop");
    assertFalse(stopPattern.canAlight(3), "Forbidden at AreaStop");

    assertTrue(stopPattern.canBoard(0), "Allowed at RegularStop");
    assertTrue(stopPattern.canBoard(1), "Allowed at RegularStop");
    assertFalse(stopPattern.canBoard(2), "Forbidden at GroupStop");
    assertFalse(stopPattern.canBoard(3), "Forbidden at AreaStop");
  }

  @Test
  void replaceStop() {
    var s1 = testModel.stop("1").build();
    var s2 = testModel.stop("2").build();
    var s3 = testModel.stop("3").build();
    var s4 = testModel.stop("4").build();

    var pattern = TransitModelForTest.stopPattern(s1, s2, s3);

    assertEquals(List.of(s1, s2, s3), pattern.getStops());

    var updated = pattern.mutate().replaceStop(s2.getId(), s4).build();
    assertEquals(List.of(s1, s4, s3), updated.getStops());
  }
}
