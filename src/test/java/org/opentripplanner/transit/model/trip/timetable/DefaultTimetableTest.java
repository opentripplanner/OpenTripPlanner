package org.opentripplanner.transit.model.trip.timetable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.Deduplicator;

class DefaultTimetableTest {

  public static final int[] STOP_TIMES = { 60, 180, 300 };
  public static final int[] STOP_TIMES_MORE_THAN_ONE_DAY = { 60, 180, 86401 };

  @Test
  void createDefaultTimetableLessThanOneDay() {
    DefaultTimetable timetable = DefaultTimetable.create(
      new int[][] { STOP_TIMES },
      new int[][] { STOP_TIMES },
      new Deduplicator()
    );
    Assertions.assertEquals(0, timetable.maxTripDurationInDays());
  }

  @Test
  void createDefaultTimetableLessThanOneDayTwoTrips() {
    DefaultTimetable timetable = DefaultTimetable.create(
      new int[][] { STOP_TIMES, STOP_TIMES },
      new int[][] { STOP_TIMES, STOP_TIMES },
      new Deduplicator()
    );
    Assertions.assertEquals(0, timetable.maxTripDurationInDays());
  }

  @Test
  void createDefaultTimetableMoreThanOneDay() {
    DefaultTimetable timetable = DefaultTimetable.create(
      new int[][] { STOP_TIMES_MORE_THAN_ONE_DAY },
      new int[][] { STOP_TIMES_MORE_THAN_ONE_DAY },
      new Deduplicator()
    );
    Assertions.assertEquals(1, timetable.maxTripDurationInDays());
  }

  @Test
  void createDefaultTimetableMoreThanOneDayTwoTrips() {
    DefaultTimetable timetable = DefaultTimetable.create(
      new int[][] { STOP_TIMES, STOP_TIMES_MORE_THAN_ONE_DAY },
      new int[][] { STOP_TIMES, STOP_TIMES_MORE_THAN_ONE_DAY },
      new Deduplicator()
    );
    Assertions.assertEquals(1, timetable.maxTripDurationInDays());
  }
}
