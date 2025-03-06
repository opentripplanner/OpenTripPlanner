package org.opentripplanner.raptor.rangeraptor.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.raptor._data.stoparrival.TestArrivals.access;
import static org.opentripplanner.raptor._data.stoparrival.TestArrivals.bus;
import static org.opentripplanner.raptor._data.transit.TestAccessEgress.free;
import static org.opentripplanner.raptor._data.transit.TestTripPattern.pattern;
import static org.opentripplanner.raptor.rangeraptor.transit.TripTimesSearch.findTripForwardSearch;
import static org.opentripplanner.raptor.rangeraptor.transit.TripTimesSearch.findTripForwardSearchApproximateTime;
import static org.opentripplanner.raptor.rangeraptor.transit.TripTimesSearch.findTripReverseSearch;
import static org.opentripplanner.raptor.rangeraptor.transit.TripTimesSearch.findTripReverseSearchApproximateTime;
import static org.opentripplanner.utils.time.TimeUtils.timeToStrLong;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._data.transit.TestTripSchedule;
import org.opentripplanner.raptor.api.view.ArrivalView;
import org.opentripplanner.raptor.spi.BoardAndAlightTime;
import org.opentripplanner.utils.time.TimeUtils;

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
    r = findTripForwardSearch(busFwd(STOP_A, STOP_C, C_ALIGHT_LATE, schedule));

    assertTimes(r, A_BOARD_TIME, C_ALIGHT_TIME);

    // Search BEFORE LAT
    r = findTripReverseSearch(busRev(STOP_C, STOP_A, A_BOARD_EARLY, schedule));

    assertTimes(r, A_BOARD_TIME, C_ALIGHT_TIME);
  }

  @Test
  public void findTripWithApproximateTimes() {
    BoardAndAlightTime r;

    // Search AFTER EDT
    r = findTripForwardSearchApproximateTime(busFwd(STOP_A, STOP_C, C_ALIGHT_LATE, schedule));

    assertTimes(r, A_BOARD_TIME, C_ALIGHT_TIME);

    // Search BEFORE LAT
    r = findTripReverseSearchApproximateTime(busRev(STOP_C, STOP_A, A_BOARD_EARLY, schedule));

    assertTimes(r, A_BOARD_TIME, C_ALIGHT_TIME);
  }

  @Test
  public void findTripWithoutSlack() {
    BoardAndAlightTime r;

    // Search AFTER EDT
    r = findTripForwardSearch(busFwd(STOP_A, STOP_C, C_ALIGHT_TIME, schedule));

    assertTimes(r, A_BOARD_TIME, C_ALIGHT_TIME);

    // Search BEFORE LAT
    r = findTripReverseSearch(busRev(STOP_C, STOP_A, A_BOARD_TIME, schedule));

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

    // With one option the approximate time does not matter, early 09:50 or late 10:30
    assertForwardAppxTime("09:50", STOP_A, a, STOP_D, d, schedule);
    assertForwardAppxTime("10:30", STOP_A, a, STOP_D, d, schedule);
    assertReverseAppxTime("09:50", STOP_D, a, STOP_A, d, schedule);
    assertReverseAppxTime("10:30", STOP_D, a, STOP_A, d, schedule);

    // Picking the closest trip from B to C, for 10:03:30 it is the first loop
    assertForwardAppxTime("10:03:30", STOP_B, b1, STOP_C, c1, schedule);
    assertReverseAppxTime("10:03:30", STOP_C, b1, STOP_B, c1, schedule);
    assertForwardAppxTime("10:03:31", STOP_B, b2, STOP_C, c2, schedule);
    assertReverseAppxTime("10:03:31", STOP_C, b2, STOP_B, c2, schedule);

    // Avoid boarding early, riding the loop and then get off. Avoid: B~C~B~C~D, expect: B~C~D
    assertForwardAppxTime("10:00", STOP_B, b2, STOP_D, d, schedule);
    assertReverseAppxTime("10:00", STOP_D, b2, STOP_B, d, schedule);

    // Avoid late alighting, ride the loop before getting off. Avoid: A~B~C~B~C, expect: A~B~C
    assertForwardAppxTime("10:50", STOP_A, a, STOP_C, c1, schedule);
    assertReverseAppxTime("10:50", STOP_C, a, STOP_A, c1, schedule);
  }

  @Test
  public void noTripFoundWhenArrivalIsToEarly() {
    assertThrows(
      IllegalStateException.class,
      () -> findTripForwardSearch(busFwd(STOP_A, STOP_C, C_ALIGHT_TIME - 1, schedule)),
      "No stops matching 'toStop'."
    );
  }

  @Test
  public void noTripFoundWhenReverseArrivalIsToLate() {
    assertThrows(
      IllegalStateException.class,
      () -> findTripReverseSearch(busRev(STOP_C, STOP_A, A_BOARD_TIME + 1, schedule)),
      "No stops matching 'fromStop'."
    );
  }

  @Test
  public void noTripFoundWhenArrivalIsWayTooEarly() {
    assertThrows(
      IllegalStateException.class,
      () -> findTripForwardSearch(busFwd(STOP_A, STOP_C, 0, schedule)),
      "No stops matching 'toStop'."
    );
  }

  @Test
  public void noTripFoundWhenReverseArrivalIsWayTooEarly() {
    assertThrows(
      IllegalStateException.class,
      () -> findTripReverseSearch(busRev(STOP_C, STOP_A, 10_000, schedule)),
      "No stops matching 'fromStop'."
    );
  }

  @Test
  public void noTripFoundWhenFromStopIsMissing() {
    assertThrows(
      IllegalStateException.class,
      () -> findTripForwardSearch(busFwd(STOP_A, STOP_A, C_ALIGHT_LATE, schedule)),
      "No stops matching 'fromStop'."
    );
  }

  @Test
  public void noTripFoundWhenToStopIsMissingInReverseSearch() {
    assertThrows(
      IllegalStateException.class,
      () -> findTripReverseSearch(busRev(STOP_C, STOP_C, A_BOARD_EARLY, schedule)),
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
      r = findTripForwardSearch(busFwd(122, 133, 800, schedule));
      assertEquals(710, r.boardTime());
      assertEquals(800, r.alightTime());

      // Board in the 1st loop at stop 4 and get off at stop 3
      r = findTripForwardSearch(busFwd(144, 133, 800, schedule));
      assertEquals(410, r.boardTime());
      assertEquals(800, r.alightTime());

      // Board in the 1st stop, ride the loop twice, alight at the last stop
      r = findTripForwardSearch(busFwd(1, 1155, 1100, schedule));
      assertEquals(10, r.boardTime());
      assertEquals(1100, r.alightTime());
    }

    // TEST REVERSE SEARCH
    {
      // Board in the 2nd loop at stop 2 and get off at stop 3
      r = findTripReverseSearch(busRev(133, 122, 710, schedule));
      assertEquals(710, r.boardTime());
      assertEquals(800, r.alightTime());

      // Board in the 1st loop at stop 4 and get off at stop 3
      r = findTripReverseSearch(busRev(133, 144, 410, schedule));
      assertEquals(410, r.boardTime());
      assertEquals(800, r.alightTime());

      // Board in the 1st stop, ride the loop twice, alight at the last stop
      r = findTripReverseSearch(busRev(1155, 1, 10, schedule));
      assertEquals(10, r.boardTime());
      assertEquals(1100, r.alightTime());
    }
  }

  private static ArrivalView<TestTripSchedule> busFwd(
    int accessStop,
    int transitToStop,
    int arrivalTime,
    TestTripSchedule trip
  ) {
    var access = access(accessStop, -9999, free(accessStop));
    return bus(1, transitToStop, arrivalTime, -9999, -9999, trip, access);
  }

  private static ArrivalView<TestTripSchedule> busRev(
    int accessStop,
    int transitToStop,
    int arrivalTime,
    TestTripSchedule trip
  ) {
    var access = access(accessStop, -9999, free(accessStop));
    return bus(1, transitToStop, arrivalTime, -9999, -9999, trip, access);
  }

  private void assertForwardAppxTime(
    String approximateArrivalTime,
    int boardStop,
    int expBoardTime,
    int alightStop,
    int expAlightTime,
    TestTripSchedule schedule
  ) {
    int arrivalTime = TimeUtils.time(approximateArrivalTime);
    var bus = busFwd(boardStop, alightStop, arrivalTime, schedule);

    var r = findTripForwardSearchApproximateTime(bus);

    assertTimes(r, expBoardTime, expAlightTime);
  }

  private void assertReverseAppxTime(
    String approximateArrivalTime,
    int boardStop,
    int expBoardTime,
    int alightStop,
    int expAlightTime,
    TestTripSchedule schedule
  ) {
    int arrivalTime = TimeUtils.time(approximateArrivalTime);
    var bus = busRev(boardStop, alightStop, arrivalTime, schedule);

    var r = findTripReverseSearchApproximateTime(bus);

    assertTimes(r, expBoardTime, expAlightTime);
  }

  private void assertTimes(BoardAndAlightTime r, int expBoardTime, int expAlightTime) {
    assertEquals(timeToStrLong(expBoardTime), timeToStrLong(r.boardTime()));
    assertEquals(timeToStrLong(expAlightTime), timeToStrLong(r.alightTime()));
  }
}
