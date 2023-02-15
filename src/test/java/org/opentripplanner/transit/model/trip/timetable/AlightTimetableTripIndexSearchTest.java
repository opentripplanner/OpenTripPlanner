package org.opentripplanner.transit.model.trip.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.transit.model.trip.Timetable.NEXT_TIME_TABLE_INDEX;
import static org.opentripplanner.transit.model.trip.Timetable.PREV_TIME_TABLE_INDEX;
import static org.opentripplanner.transit.model.trip.timetable.AlightTripIndexSearch.findAlightTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlightTimetableTripIndexSearchTest {

  private final int[] ONE = { 10 };
  private final int[] FOUR = { 10, 11, 11, 12 };
  private final int[] MANY = new int[200];

  @BeforeEach
  public void setup() {
    MANY[0] = 10;
    MANY[1] = 11;
    MANY[2] = 11;
    MANY[3] = 12;
    for (int i = 4; i < MANY.length; i++) {
      MANY[i] = MANY[i - 1] + 1;
    }
  }

  @Test
  void findDepartureSingleElementTimetable() {
    assertEquals(NEXT_TIME_TABLE_INDEX, findAlightTime(ONE, -1, 0, 11));
    assertEquals(0, findAlightTime(ONE, -1, 0, 10));
    assertEquals(PREV_TIME_TABLE_INDEX, findAlightTime(ONE, -1, 0, 9));
  }

  @Test
  void findDepartureInTimetableWithFourElements() {
    final int start = -1;
    final int end = FOUR.length - 1;

    assertEquals(NEXT_TIME_TABLE_INDEX, findAlightTime(FOUR, start, end, 13));
    assertEquals(3, findAlightTime(FOUR, start, end, 12));
    assertEquals(2, findAlightTime(FOUR, start, end, 11));
    assertEquals(0, findAlightTime(FOUR, start, end, 10));
    assertEquals(PREV_TIME_TABLE_INDEX, findAlightTime(FOUR, start, end, 9));
  }

  @Test
  void findDepartureInTimetableWith200Elements() {
    testSearch(AlightTripIndexSearch::findAlightTime);
    testSearch(AlightTripIndexSearch::findAlightTimeLinearApproximation);
    testSearch(AlightTripIndexSearch::findAlightTimeBinarySearch);
  }

  void testSearch(TimetableTripIndexSearch search) {
    // Search the first half of timetable
    int start = -1;
    int end = start + 100;

    assertEquals(PREV_TIME_TABLE_INDEX, search.searchForTripIndex(MANY, start, end, 9));
    assertEquals(0, search.searchForTripIndex(MANY, start, end, 10));
    assertEquals(2, search.searchForTripIndex(MANY, start, end, 11));
    assertEquals(3, search.searchForTripIndex(MANY, start, end, 12));
    // Search time at end
    assertEquals(99, search.searchForTripIndex(MANY, start, end, 108));
    assertEquals(NEXT_TIME_TABLE_INDEX, search.searchForTripIndex(MANY, start, end, 109));

    // Search for all times to see that thresholds are done right
    for (int i = 4; i < end; ++i) {
      assertEquals(i, search.searchForTripIndex(MANY, start, end, i + 9));
    }

    // Search the last half of timetable
    start = end;
    end = start + 100;

    for (int i = start + 1; i < end; ++i) {
      assertEquals(i, search.searchForTripIndex(MANY, start, end, i + 9));
    }

    assertEquals(NEXT_TIME_TABLE_INDEX, search.searchForTripIndex(MANY, start, end, 209));
  }
}
