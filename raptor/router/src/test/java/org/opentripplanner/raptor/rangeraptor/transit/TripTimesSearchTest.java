package org.opentripplanner.raptor.rangeraptor.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.raptor._data.stoparrival.TestArrivals.access;
import static org.opentripplanner.raptor._data.stoparrival.TestArrivals.bus;
import static org.opentripplanner.raptor._data.stoparrival.TestArrivals.busReverseSearch;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.free;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor.rangeraptor.transit.TripTimesSearch.findTripForwardSearch;
import static org.opentripplanner.raptor.rangeraptor.transit.TripTimesSearch.findTripReverseSearch;
import static org.opentripplanner.utils.time.TimeUtils.timeToStrLong;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.spi.BoardAndAlightTime;

public class TripTimesSearchTest implements RaptorTestConstants {

  private static final int A_BOARD_TIME = 110;
  private static final int C_ALIGHT_TIME = 300;
  private static final int A_BOARD_EARLY = A_BOARD_TIME - 10;
  private static final int C_ALIGHT_LATE = C_ALIGHT_TIME + 10;

  // Given a trip-schedule with board-times [110, 210, -] and alight-times [-, 200, 300].
  private TestTripSchedule schedule = TestTripSchedule.schedule(
    pattern("P1", STOP_A, STOP_B, STOP_C)
  )
    .departures(A_BOARD_TIME, 210, 310)
    .arrivals(100, 200, C_ALIGHT_TIME)
    .build();

  @Test
  public void findTripWithPlentySlack() {
    BoardAndAlightTime r;

    // Search AFTER EDT
    r = findTripForwardSearch(busFwd(STOP_A, STOP_C, A_BOARD_EARLY, schedule));

    assertTimes(r, A_BOARD_TIME, C_ALIGHT_TIME);

    // Search BEFORE LAT
    r = findTripReverseSearch(busRev(STOP_C, STOP_A, C_ALIGHT_LATE, schedule));

    assertTimes(r, A_BOARD_TIME, C_ALIGHT_TIME);
  }

  @Test
  public void findTripWithoutSlack() {
    BoardAndAlightTime r;

    // Search AFTER EDT
    r = findTripForwardSearch(busFwd(STOP_A, STOP_C, A_BOARD_TIME, schedule));

    assertTimes(r, A_BOARD_TIME, C_ALIGHT_TIME);

    // Search BEFORE LAT
    r = findTripReverseSearch(busRev(STOP_C, STOP_A, C_ALIGHT_TIME, schedule));

    assertTimes(r, A_BOARD_TIME, C_ALIGHT_TIME);
  }

  @Test
  public void findInLoop() {
    // Stops A - (B - C){2 times} - D
    var schedule = TestTripSchedule.schedule(
      pattern("P1", STOP_A, STOP_B, STOP_C, STOP_B, STOP_C, STOP_D)
    )
      .times("10:01 10:02 10:03 10:04 10:05 10:06")
      .build();
    // Time at stop
    int a = schedule.departure(0);
    int b1 = schedule.departure(1);
    int c1 = schedule.departure(2);
    int b2 = schedule.departure(3);
    int c2 = schedule.departure(4);
    int d = schedule.departure(5);

    // When a stop appears only once, the position is unambiguous
    assertForwardSearch(STOP_A, a, STOP_D, d, schedule);
    assertReverseSearch(STOP_A, a, STOP_D, d, schedule);

    // Boarding at the first occurrence of B, alighting at the first occurrence of C
    assertForwardSearch(STOP_B, b1, STOP_C, c1, schedule);
    assertReverseSearch(STOP_B, b1, STOP_C, c1, schedule);

    // Boarding at the second occurrence of B, alighting at the second occurrence of C
    assertForwardSearch(STOP_B, b2, STOP_C, c2, schedule);
    assertReverseSearch(STOP_B, b2, STOP_C, c2, schedule);

    // Boarding at the second occurrence of B, alighting at D (path: B~C~D, not B~C~B~C~D)
    assertForwardSearch(STOP_B, b2, STOP_D, d, schedule);
    assertReverseSearch(STOP_B, b2, STOP_D, d, schedule);

    // Boarding at A, alighting at the first occurrence of C (path: A~B~C, not A~B~C~B~C)
    assertForwardSearch(STOP_A, a, STOP_C, c1, schedule);
    assertReverseSearch(STOP_A, a, STOP_C, c1, schedule);
  }

  @Test
  public void noTripFoundWhenFromStopIsMissing() {
    assertThrows(
      IllegalStateException.class,
      () -> findTripForwardSearch(busFwd(STOP_A, STOP_A, A_BOARD_TIME, schedule)),
      "No stops matching 'fromStop'."
    );
  }

  @Test
  public void noTripFoundWhenToStopIsMissingInReverseSearch() {
    assertThrows(
      IllegalStateException.class,
      () -> findTripReverseSearch(busRev(STOP_C, STOP_C, C_ALIGHT_TIME, schedule)),
      "No stops matching 'toStop'"
    );
  }

  /**
   * The trip-schedule may visit the same stop many times. For example in the case of a
   * subway-loop.
   */
  @Test
  public void findTripWhenScheduleLoops() {
    // Create a trip schedule that run in a 2 loops with a stop before and after the loop
    // stops: Start at 1, loop twice: 111, 122, 133, 144, 155, and end at 1155
    // alight times:    [  -, 100, 200, 300, 400, .., 1100] and
    // departure times: [ 10, 110, 210, 310, 410, .., 1110].
    schedule = TestTripSchedule.schedule(
      pattern(1, 111, 122, 133, 144, 155, 111, 122, 133, 144, 155, 1155)
    )
      .departures(10, 110, 210, 310, 410, 510, 610, 710, 810, 910, 1010, 1110)
      .arrivals(0, 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1100)
      .build();

    BoardAndAlightTime r;

    // TEST FORWARD SEARCH
    {
      // Board in the 2nd loop at stop 2 and get off at stop 3
      r = findTripForwardSearch(busFwd(122, 133, 710, schedule));
      assertEquals(710, r.boardTime());
      assertEquals(800, r.alightTime());

      // Board in the 1st loop at stop 4 and get off at stop 3
      r = findTripForwardSearch(busFwd(144, 133, 410, schedule));
      assertEquals(410, r.boardTime());
      assertEquals(800, r.alightTime());

      // Board in the 1st stop, ride the loop twice, alight at the last stop
      r = findTripForwardSearch(busFwd(1, 1155, 10, schedule));
      assertEquals(10, r.boardTime());
      assertEquals(1100, r.alightTime());
    }

    // TEST REVERSE SEARCH
    {
      // Board in the 2nd loop at stop 2 and get off at stop 3
      r = findTripReverseSearch(busRev(133, 122, 800, schedule));
      assertEquals(710, r.boardTime());
      assertEquals(800, r.alightTime());

      // Board in the 1st loop at stop 4 and get off at stop 3
      r = findTripReverseSearch(busRev(133, 144, 800, schedule));
      assertEquals(410, r.boardTime());
      assertEquals(800, r.alightTime());

      // Board in the 1st stop, ride the loop twice, alight at the last stop
      r = findTripReverseSearch(busRev(1155, 1, 1100, schedule));
      assertEquals(10, r.boardTime());
      assertEquals(1100, r.alightTime());
    }
  }

  /**
   * Forward search: the access arrives at the board stop at {@code boardTime}. Uses
   * {@code findDepartureStopPosition} to identify which stop position was boarded.
   */
  private static ArrivalView<TestTripSchedule> busFwd(
    int boardStop,
    int alightStop,
    int boardTime,
    TestTripSchedule trip
  ) {
    var access = access(boardStop, boardTime, free(boardStop));
    return bus(1, alightStop, -9999, -9999, -9999, trip, access);
  }

  /**
   * Reverse search: the access arrives at the alight stop at {@code alightTime}. Uses
   * {@code findArrivalStopPosition} to identify which stop position was alighted, which correctly
   * handles the last stop in a pattern.
   */
  private static ArrivalView<TestTripSchedule> busRev(
    int alightStop,
    int boardStop,
    int alightTime,
    TestTripSchedule trip
  ) {
    var access = access(alightStop, alightTime, free(alightStop));
    return busReverseSearch(1, boardStop, -9999, -9999, -9999, trip, access);
  }

  private void assertForwardSearch(
    int boardStop,
    int boardTime,
    int alightStop,
    int expAlightTime,
    TestTripSchedule schedule
  ) {
    var r = findTripForwardSearch(busFwd(boardStop, alightStop, boardTime, schedule));
    assertTimes(r, boardTime, expAlightTime);
  }

  private void assertReverseSearch(
    int boardStop,
    int expBoardTime,
    int alightStop,
    int alightTime,
    TestTripSchedule schedule
  ) {
    var r = findTripReverseSearch(busRev(alightStop, boardStop, alightTime, schedule));
    assertTimes(r, expBoardTime, alightTime);
  }

  private void assertTimes(BoardAndAlightTime r, int expBoardTime, int expAlightTime) {
    assertEquals(timeToStrLong(expBoardTime), timeToStrLong(r.boardTime()));
    assertEquals(timeToStrLong(expAlightTime), timeToStrLong(r.alightTime()));
  }
}
