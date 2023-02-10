package org.opentripplanner.transit.model.trip.timetable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.trip.Timetable;

class TimetableTest {

  private static final Timetable SUBJECT = TimetableBuilder.of().schedule("0:01 0:03 0:06").build();

  @Test
  void testFindTripIndexAlightingBeforeInCurrentTimetable() {
    Assertions.assertEquals(0, SUBJECT.findTripIndexAlightingBefore(2, 3000));
  }

  @Test
  void testFindTripIndexAlightingBeforeInPreviousTimetable() {
    Assertions.assertEquals(
      Timetable.PREV_TIME_TABLE_INDEX,
      SUBJECT.findTripIndexAlightingBefore(2, 0)
    );
  }
}
