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

class ReverseSearchTest {

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
    Arguments.of(timetableBasic, "12:01", STOP_0, TRIP_0, "12:00")
    // TODO RTM - Add tests cases

    // Timetable with more than one trip
    // First stop pos: 0
    // Middle stop pos: 1
    // Last stop pos: 2
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
    var reverseSearch = new ReverseSearch(timetable);

    var alightEvent = reverseSearch.search(earliestBoardTime, stopPos);

    // Verify correct input stored in event
    assertEquals(ebt, TimeUtils.timeToStrCompact(alightEvent.earliestBoardTime()));
    assertEquals(stopPos, alightEvent.stopPositionInPattern());

    // Correct trip found
    assertEquals(expTripIndex, alightEvent.tripIndex());
    assertEquals(expTime, TimeUtils.timeToStrCompact(alightEvent.time()));
    assertFalse(alightEvent.empty());
  }

  // TODO RTM - Add none-hapy day tests

  @Test
  void testToString() {
    int earliestBoardTime = TimeUtils.time("12:00");
    var reverseSearch = new ReverseSearch(timetableBasic);

    assertEquals(
      "ReverseSearch{timetable: DefaultTimetable{nTrips: 1, nStops: 3, times: {trip 0: [12:00 13:00 14:00]}}}",
      reverseSearch.toString()
    );

    var boardEvent = reverseSearch.search(earliestBoardTime, 0);

    assertEquals(
      "ReverseSearch{tripIndex: 0, stopPosition: 0, earliestBoardTime: 12:00, timetable: DefaultTimetable{nTrips: 1, nStops: 3, times: {trip 0: [12:00 13:00 14:00]}}}",
      boardEvent.toString()
    );
  }
}
