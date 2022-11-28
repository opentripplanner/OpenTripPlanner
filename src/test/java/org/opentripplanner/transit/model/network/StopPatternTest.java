package org.opentripplanner.transit.model.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.framework.geometry.GeometryUtils;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.timetable.Trip;

class StopPatternTest {

  @Test
  void boardingAlightingConditions() {
    // We have different types of stops, of which only regular stops should allow boarding/alighting
    var s1 = TransitModelForTest.stopForTest("1", 60.0, 11.0);
    var s2 = TransitModelForTest.stopForTest("2", 61.0, 11.0);
    var s3 = TransitModelForTest.stopForTest("3", 62.0, 11.0);
    var s4 = TransitModelForTest.stopForTest("4", 62.1, 11.0);

    var s34 = TransitModelForTest.groupStopForTest("3_4", List.of(s3, s4));

    var areaStop = TransitModelForTest.areaStopForTest(
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
        TransitModelForTest.stopTime(t, 0, s1),
        TransitModelForTest.stopTime(t, 1, s2),
        TransitModelForTest.stopTime(t, 2, s34),
        TransitModelForTest.stopTime(t, 3, areaStop)
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
}
