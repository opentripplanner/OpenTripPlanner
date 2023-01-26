package org.opentripplanner.transit.model.trip.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.time.TimeUtils;

class TimetableBuilderTest {

  private static final int T00_00 = TimeUtils.time("00:00");
  private static final int T00_01 = TimeUtils.time("00:01");
  private static final int T00_02 = TimeUtils.time("00:02");
  private static final int T00_03 = TimeUtils.time("00:03");
  private static final int T00_05 = TimeUtils.time("00:05");
  private static final int T12_00 = TimeUtils.time("12:00");
  private static final int T12_30 = TimeUtils.time("12:30");
  private static final int T13_00 = TimeUtils.time("13:00");
  private static final int DWELL_TIME = DurationUtils.durationInSeconds("60s");
  private static final int TRIP_0 = 0;
  private static final int TRIP_1 = 1;
  private static final int STOP_0 = 0;
  private static final int STOP_1 = 1;
  private static final int STOP_2 = 2;

  @Test
  void oneSchedule() {
    var timetable = TimetableBuilder.of().schedule("0:01 0:03").build();
    assertEquals(1, timetable.numOfTrips());
    assertEquals(2, timetable.numOfStops());
    assertEquals(0, timetable.maxTripDurationInDays());
    assertEquals(T00_01, timetable.alightTime(TRIP_0, STOP_0));
    assertEquals(T00_01, timetable.boardTime(TRIP_0, STOP_0));
    assertEquals(T00_03, timetable.alightTime(TRIP_0, STOP_1));
    assertEquals(T00_03, timetable.boardTime(TRIP_0, STOP_1));
  }

  @Test
  void oneScheduleWithAlightAndBoardTimes() {
    var timetable = TimetableBuilder.of().schedule("0:01 0:03", "0:00 0:02").build();
    assertEquals(1, timetable.numOfTrips());
    assertEquals(2, timetable.numOfStops());
    assertEquals(0, timetable.maxTripDurationInDays());
    assertEquals(T00_00, timetable.alightTime(TRIP_0, STOP_0));
    assertEquals(T00_01, timetable.boardTime(TRIP_0, STOP_0));
    assertEquals(T00_02, timetable.alightTime(TRIP_0, STOP_1));
    assertEquals(T00_03, timetable.boardTime(TRIP_0, STOP_1));
  }

  @Test
  void twoSchedules() {
    var timetable = TimetableBuilder
      .of()
      .schedule("0:01 0:03 0:05", DWELL_TIME)
      .schedule("12:00 12:30 13:00", DWELL_TIME)
      .build();
    assertEquals(2, timetable.numOfTrips());
    assertEquals(3, timetable.numOfStops());
    assertEquals(0, timetable.maxTripDurationInDays());
    assertEquals(T00_01 - DWELL_TIME, timetable.alightTime(TRIP_0, STOP_0));
    assertEquals(T00_01, timetable.boardTime(TRIP_0, STOP_0));
    assertEquals(T00_03 - DWELL_TIME, timetable.alightTime(TRIP_0, STOP_1));
    assertEquals(T00_03, timetable.boardTime(TRIP_0, STOP_1));
    assertEquals(T00_05 - DWELL_TIME, timetable.alightTime(TRIP_0, STOP_2));
    assertEquals(T00_05, timetable.boardTime(TRIP_0, STOP_2));
    assertEquals(T12_00 - DWELL_TIME, timetable.alightTime(TRIP_1, STOP_0));
    assertEquals(T12_00, timetable.boardTime(TRIP_1, STOP_0));
    assertEquals(T12_30 - DWELL_TIME, timetable.alightTime(TRIP_1, STOP_1));
    assertEquals(T12_30, timetable.boardTime(TRIP_1, STOP_1));
    assertEquals(T13_00 - DWELL_TIME, timetable.alightTime(TRIP_1, STOP_2));
    assertEquals(T13_00, timetable.boardTime(TRIP_1, STOP_2));
  }

  @Test
  void veryLongSchedule() {
    var timetable = TimetableBuilder
      .of()
      .schedule("00:30 12:30 47:30")
      .build();
    assertEquals(1, timetable.numOfTrips());
    assertEquals(3, timetable.numOfStops());
    assertEquals(1, timetable.maxTripDurationInDays());
  }



    @Test
  void aTripScheduleNeedAtLeastTwoStops() {
    assertThrows(
      IllegalArgumentException.class,
      () -> TimetableBuilder.of().schedule("0:01").build()
    );
  }

  @Test
  void allTripSchedulesInATimetableMustHaveTheSameNumberOfStops() {
    assertThrows(
      IllegalArgumentException.class,
      () -> TimetableBuilder.of().schedule("0:01 0:03").schedule("0:01 0:03 0:05").build()
    );
  }

  @Test
  void boardAndAlightTimesMustContainTheSameNumberOfStops() {
    assertThrows(
      IllegalArgumentException.class,
      () -> TimetableBuilder.of().schedule("0:01 0:03", "0:01 0:03 0:05").build()
    );
  }
}
