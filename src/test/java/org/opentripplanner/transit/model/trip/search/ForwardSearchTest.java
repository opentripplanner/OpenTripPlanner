package org.opentripplanner.transit.model.trip.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.framework.time.TimeUtils;
import org.opentripplanner.test.support.VariableSource;
import org.opentripplanner.transit.model.trip.Timetable;
import org.opentripplanner.transit.model.trip.timetable.TimetableBuilder;

class ForwardSearchTest {

  private static final int STOP_0 = 0;
  private static final int STOP_1 = 1;
  private static final int STOP_2 = 2;

  private static final int TRIP_0 = 0;
  private static final int TRIP_1 = 1;

  private static final Timetable timetableBasic = TimetableBuilder
    .of()
    .schedule("12:00 13:00 14:00")
    .build();
  private static final Timetable timetableTwoTrips = TimetableBuilder
    .of()
    .schedule("12:30 13:30 14:30")
    .schedule("12:40 13:40 14:40")
    .build();

  static Stream<Arguments> boardingSearchTestCases = Stream.of(
    Arguments.of(timetableBasic, "11:37", STOP_0, TRIP_0, "12:00"),
    Arguments.of(timetableBasic, "12:00", STOP_0, TRIP_0, "12:00")
    // Arguments.of(timetableBasic, "14:01", STOP_1, TRIP_0, "13:00")
    //Arguments.of(timetableBasic, "14:00", STOP_2, TRIP_0, "14:00"),
    //Arguments.of(timetableTwoTrips, "12:01", STOP_0, TRIP_1, "12:30"),
    //Arguments.of(timetableTwoTrips, "13:00", TRIP_1, TRIP_1, "13:00"),
    //Arguments.of(timetableTwoTrips, "13:01", TRIP_1, TRIP_1, "13:30")
  );

  @ParameterizedTest(
    name = "Earliest board time {1}, stop pos: {2}, exp. trip index: {3},  exp. board time: {4}"
  )
  @VariableSource("boardingSearchTestCases")
  void boardingSearchTest(
    Timetable timetable,
    String ebt,
    int stopPos,
    int expTripIndex,
    String expTime
  ) {
    int earliestBoardTime = TimeUtils.time(ebt);
    var bae = new ForwardSearch(timetable);

    bae.search(earliestBoardTime, stopPos);

    // Verify correct input stored in event
    assertEquals(ebt, TimeUtils.timeToStrCompact(bae.earliestBoardTime()));
    assertEquals(stopPos, bae.stopPositionInPattern());

    // Correct trip found
    assertEquals(expTripIndex, bae.tripIndex());
    assertEquals(expTime, TimeUtils.timeToStrCompact(bae.time()));
    assertFalse(bae.empty());
  }
}
