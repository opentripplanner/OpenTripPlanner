package org.opentripplanner.transit.model.trip.timetable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.transit.model.trip.Timetable;

class TimetableTest {

  private static final Timetable SUBJECT = TimetableBuilder
    .of()
    .schedule("0:01 0:03 0:06")
    .schedule("0:03 0:05 0:08")
    .build();

  private static final int EARLIER_TIME_THAN_FIRST_TRIP = 0;
  private static final int LATER_TIME_THAN_SECOND_TRIP = 3000;
  private static final int T00_06 = TimeUtils.time("0:06");
  private static final int TIME_BETWEEN_FIRST_AND_SECOND_TRIP = T00_06 + 1;
  private static final int FIRST_TRIP_INDEX = 0;
  private static final int SECOND_TRIP_INDEX = 1;
  private static final int LAST_STOP_INDEX = 2;

  @Test
  void testFindTripIndexAlightingBeforeInCurrentTimetable() {
    Assertions.assertEquals(
      SECOND_TRIP_INDEX,
      SUBJECT.findTripIndexAlightingBefore(LAST_STOP_INDEX, LATER_TIME_THAN_SECOND_TRIP)
    );
  }

  @Test
  void testFindFirstTripIndexAlightingBeforeInCurrentTimetable() {
    Assertions.assertEquals(
      FIRST_TRIP_INDEX,
      SUBJECT.findTripIndexAlightingBefore(LAST_STOP_INDEX, TIME_BETWEEN_FIRST_AND_SECOND_TRIP)
    );
  }

  @Test
  void testFindTripIndexAlightingBeforeInPreviousTimetable() {
    Assertions.assertEquals(
      Timetable.PREV_TIME_TABLE_INDEX,
      SUBJECT.findTripIndexAlightingBefore(LAST_STOP_INDEX, EARLIER_TIME_THAN_FIRST_TRIP)
    );
  }

  @Test
  void testFindTripIndexBoardingAfterInCurrentTimetable() {
    Assertions.assertEquals(
      FIRST_TRIP_INDEX,
      SUBJECT.findTripIndexBoardingAfter(LAST_STOP_INDEX, EARLIER_TIME_THAN_FIRST_TRIP)
    );
  }

  @Test
  void testFindSecondTripIndexBoardingAfterInCurrentTimetable() {
    Assertions.assertEquals(
      SECOND_TRIP_INDEX,
      SUBJECT.findTripIndexBoardingAfter(LAST_STOP_INDEX, TIME_BETWEEN_FIRST_AND_SECOND_TRIP)
    );
  }

  @Test
  void testFindTripIndexBoardingAfterInNextTimetable() {
    Assertions.assertEquals(
      Timetable.NEXT_TIME_TABLE_INDEX,
      SUBJECT.findTripIndexBoardingAfter(LAST_STOP_INDEX, LATER_TIME_THAN_SECOND_TRIP)
    );
  }
}
