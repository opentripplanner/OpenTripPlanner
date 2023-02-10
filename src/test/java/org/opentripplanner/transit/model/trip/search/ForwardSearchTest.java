package org.opentripplanner.transit.model.trip.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
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
    .schedule("12:00 13:00 14:00")
    .schedule("12:30 13:30 14:30")
    .build();

  static Stream<Arguments> boardingSearchTestCases = Stream.of(
    // Basic timetable with just one trip
    Arguments.of(timetableBasic, "11:37", STOP_0, TRIP_0, "12:00"),
    Arguments.of(timetableBasic, "12:00", STOP_0, TRIP_0, "12:00"),
    Arguments.of(timetableBasic, "12:12", STOP_1, TRIP_0, "13:00"),
    Arguments.of(timetableBasic, "13:00", STOP_1, TRIP_0, "13:00"),
    Arguments.of(timetableBasic, "13:59", STOP_2, TRIP_0, "14:00"),
    Arguments.of(timetableBasic, "14:00", STOP_2, TRIP_0, "14:00"),
    // Timetable with more than one trip
    // First stop pos: 0
    Arguments.of(timetableTwoTrips, "11:59", STOP_0, TRIP_0, "12:00"),
    Arguments.of(timetableTwoTrips, "12:00", STOP_0, TRIP_0, "12:00"),
    Arguments.of(timetableTwoTrips, "12:01", STOP_0, TRIP_1, "12:30"),
    Arguments.of(timetableTwoTrips, "12:30", STOP_0, TRIP_1, "12:30"),
    // Middle stop pos: 1
    Arguments.of(timetableTwoTrips, "12:59", STOP_1, TRIP_0, "13:00"),
    Arguments.of(timetableTwoTrips, "13:00", STOP_1, TRIP_0, "13:00"),
    Arguments.of(timetableTwoTrips, "13:01", STOP_1, TRIP_1, "13:30"),
    Arguments.of(timetableTwoTrips, "13:30", STOP_1, TRIP_1, "13:30"),
    // Last stop pos: 2
    Arguments.of(timetableTwoTrips, "13:59", STOP_2, TRIP_0, "14:00"),
    Arguments.of(timetableTwoTrips, "14:00", STOP_2, TRIP_0, "14:00"),
    Arguments.of(timetableTwoTrips, "14:01", STOP_2, TRIP_1, "14:30"),
    Arguments.of(timetableTwoTrips, "14:30", STOP_2, TRIP_1, "14:30")
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
    var forwardSearch = new ForwardSearch(timetable);

    var boardEvent = forwardSearch.search(earliestBoardTime, stopPos);

    // Verify correct input stored in event
    assertEquals(ebt, TimeUtils.timeToStrCompact(boardEvent.earliestBoardTime()));
    assertEquals(stopPos, boardEvent.stopPositionInPattern());

    // Correct trip found
    assertEquals(expTripIndex, boardEvent.tripIndex());
    assertEquals(expTime, TimeUtils.timeToStrCompact(boardEvent.time()));
    assertFalse(boardEvent.empty());
  }

  // TODO RTM - Add none-hapy day tests

  @Test
  void testToString() {
    int earliestBoardTime = TimeUtils.time("12:00");
    var forwardSearch = new ForwardSearch(timetableBasic);

    assertEquals(
      "ForwardSearch{timetable: DefaultTimetable{nTrips: 1, nStops: 3, times: {trip 0: [12:00 13:00 14:00]}}}",
      forwardSearch.toString()
    );

    var boardEvent = forwardSearch.search(earliestBoardTime, 0);

    assertEquals(
      "ForwardSearch{tripIndex: 0, stopPosition: 0, earliestBoardTime: 12:00, timetable: DefaultTimetable{nTrips: 1, nStops: 3, times: {trip 0: [12:00 13:00 14:00]}}}",
      boardEvent.toString()
    );
  }
}
