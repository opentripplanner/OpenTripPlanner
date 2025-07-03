package org.opentripplanner.transit.model.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.timetable.Trip;

class StopPatternTest {

  private final TimetableRepositoryForTest testModel = TimetableRepositoryForTest.of();

  @Test
  void boardingAlightingConditions() {
    // We have different types of stops, of which only regular stops should allow boarding/alighting
    var s1 = testModel.stop("1", 60.0, 11.0).build();
    var s2 = testModel.stop("2", 61.0, 11.0).build();
    var s3 = testModel.stop("3", 62.0, 11.0).build();
    var s4 = testModel.stop("4", 62.1, 11.0).build();

    var s34 = testModel.groupStop("3_4", s3, s4);

    var areaStop = testModel.areaStop("area").build();

    Trip t = TimetableRepositoryForTest.trip("trip").build();

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

    var pattern = TimetableRepositoryForTest.stopPattern(s1, s2, s3);

    assertEquals(List.of(s1, s2, s3), pattern.getStops());

    var updated = pattern.copyOf().replaceStop(s2.getId(), s4).build();
    assertEquals(List.of(s1, s4, s3), updated.getStops());
  }

  @Test
  void replaceStops() {
    var s1 = testModel.stop("1").build();
    var s2 = testModel.stop("2").build();
    var s3 = testModel.stop("3").build();
    var s4 = testModel.stop("4").build();

    var pattern = TimetableRepositoryForTest.stopPattern(s1, s2, s3);

    assertEquals(List.of(s1, s2, s3), pattern.getStops());

    var updated = pattern.copyOf().replaceStops(Map.of(0, s4, 2, s4)).build();
    assertEquals(List.of(s4, s2, s4), updated.getStops());
  }

  @Test
  void updatePickupDropoff() {
    var s1 = testModel.stop("1").build();
    var s2 = testModel.stop("2").build();
    var s3 = testModel.stop("3").build();

    var pattern = TimetableRepositoryForTest.stopPattern(s1, s2, s3);

    assertEquals(PickDrop.SCHEDULED, pattern.getPickup(0));
    assertEquals(PickDrop.SCHEDULED, pattern.getPickup(1));
    assertEquals(PickDrop.SCHEDULED, pattern.getPickup(2));
    assertEquals(PickDrop.SCHEDULED, pattern.getDropoff(0));
    assertEquals(PickDrop.SCHEDULED, pattern.getDropoff(1));
    assertEquals(PickDrop.SCHEDULED, pattern.getDropoff(2));

    var updated = pattern
      .copyOf()
      .updatePickup(Map.of(0, PickDrop.CALL_AGENCY, 1, PickDrop.COORDINATE_WITH_DRIVER))
      .updateDropoff(Map.of(2, PickDrop.CANCELLED))
      .build();
    assertEquals(PickDrop.CALL_AGENCY, updated.getPickup(0));
    assertEquals(PickDrop.COORDINATE_WITH_DRIVER, updated.getPickup(1));
    assertEquals(PickDrop.SCHEDULED, updated.getPickup(2));
    assertEquals(PickDrop.SCHEDULED, updated.getDropoff(0));
    assertEquals(PickDrop.SCHEDULED, updated.getDropoff(1));
    assertEquals(PickDrop.CANCELLED, updated.getDropoff(2));
  }
}
