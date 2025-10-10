package org.opentripplanner.raptor._data.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor.api.model.RaptorConstants.NOT_FOUND;

import io.github.nchaugen.tabletest.junit.FactorySources;
import io.github.nchaugen.tabletest.junit.TableTest;
import java.util.List;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor._support.TableTestValueParser;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * This tests another test class - we do it to make sure the
 * {@link TestTripScheduleSearch} is following the Raptor SPI contract.
 */
@FactorySources(TableTestValueParser.class)
public class TestTripScheduleSearchTest implements RaptorTestConstants {

  private static final int T09_59_59 = TimeUtils.time("09:59:59");
  private static final int T10_20_01 = TimeUtils.time("10:20:01");

  private static final int TRIP_ONE = 0;
  private static final int TRIP_TWO = 1;

  private static final TestTripPattern PATTERN = TestTripPattern.pattern("R1", STOP_A, STOP_B);

  private static final List<TestTripSchedule> ONE_TRIP = List.of(
    TestTripSchedule.schedule(PATTERN).times("10:00 10:10").build()
  );

  private static final List<TestTripSchedule> TWO_TRIPS = List.of(
    TestTripSchedule.schedule(PATTERN).times("10:00 10:10").build(),
    TestTripSchedule.schedule(PATTERN).times("10:10 10:20").build()
  );

  @TableTest(
    """
    Direction | Stop Pos | Search Time | Exp Time  | Exp Status
    FORWARD   | 0        | 09:59:59    | 10:00:00  | OK
    FORWARD   | 0        | 10:00:00    | 10:00:00  | OK
    FORWARD   | 0        | 10:00:01    | UNDEFINED | EMPTY
    FORWARD   | 1        | 10:09:59    | 10:10:00  | OK
    FORWARD   | 1        | 10:10:00    | 10:10:00  | OK
    FORWARD   | 1        | 10:10:01    | UNDEFINED | EMPTY
    REVERSE   | 0        | 09:59:59    | UNDEFINED | EMPTY
    REVERSE   | 0        | 10:00:00    | 10:00:00  | OK
    REVERSE   | 0        | 10:00:01    | 10:00:00  | OK
    REVERSE   | 1        | 10:09:59    | UNDEFINED | EMPTY
    REVERSE   | 1        | 10:10:00    | 10:10:00  | OK
    REVERSE   | 1        | 10:10:01    | 10:10:00  | OK
    """
  )
  void tripSearchWithOneTrip(
    SearchDirection direction,
    int stopPos,
    int searchTime,
    int expTime,
    Status expStatus
  ) {
    var search = new TestTripScheduleSearch(direction, ONE_TRIP);
    var result = search.search(searchTime, stopPos);

    if (expStatus == Status.EMPTY) {
      assertTrue(result.empty());
      assertTime(searchTime, result.earliestBoardTime());
      assertEquals(NOT_FOUND, result.tripIndex());
    } else if (expStatus == Status.OK) {
      assertFalse(result.empty());
      assertEquals(stopPos, result.stopPositionInPattern());
      assertTime(expTime, result.time());
      assertTime(searchTime, result.earliestBoardTime());
      assertEquals(0, result.tripIndex());
    }

    if (!result.empty()) {
      // A second search fails if the first search succeeded
      int tripIndex = result.tripIndex();
      var e = search.search(direction.isForward() ? T09_59_59 : T10_20_01, stopPos, tripIndex);
      assertTrue(e.empty());
    }
  }

  @TableTest(
    """
    Direction | Stop Pos | Search Time | Exp Time  | Exp Trip Index
    FORWARD   | 0        | 09:59:59    | 10:00:00  | 0
    FORWARD   | 0        | 10:00:00    | 10:00:00  | 0
    FORWARD   | 0        | 09:59:59    | 10:00:00  | 0
    FORWARD   | 0        | 10:00:00    | 10:00:00  | 0
    FORWARD   | 0        | 10:00:01    | 10:10:00  | 1
    FORWARD   | 0        | 10:09:59    | 10:10:00  | 1
    FORWARD   | 0        | 10:10:00    | 10:10:00  | 1
    FORWARD   | 0        | 10:10:01    | UNDEFINED | NOT_FOUND
    FORWARD   | 1        | 10:09:59    | 10:10:00  | 0
    FORWARD   | 1        | 10:10:00    | 10:10:00  | 0
    FORWARD   | 1        | 10:10:01    | 10:20:00  | 1
    FORWARD   | 1        | 10:19:59    | 10:20:00  | 1
    FORWARD   | 1        | 10:20:00    | 10:20:00  | 1
    FORWARD   | 1        | 10:20:01    | UNDEFINED | NOT_FOUND
    REVERSE   | 0        | 09:59:59    | UNDEFINED | NOT_FOUND
    REVERSE   | 0        | 10:00:00    | 10:00:00  | 0
    REVERSE   | 0        | 10:00:01    | 10:00:00  | 0
    REVERSE   | 0        | 10:09:59    | 10:00:00  | 0
    REVERSE   | 0        | 10:10:00    | 10:10:00  | 1
    REVERSE   | 0        | 10:10:01    | 10:10:00  | 1
    REVERSE   | 1        | 10:09:59    | UNDEFINED | NOT_FOUND
    REVERSE   | 1        | 10:10:00    | 10:10:00  | 0
    REVERSE   | 1        | 10:10:01    | 10:10:00  | 0
    REVERSE   | 1        | 10:19:59    | 10:10:00  | 0
    REVERSE   | 1        | 10:20:00    | 10:20:00  | 1
    REVERSE   | 1        | 10:20:01    | 10:20:00  | 1
    """
  )
  void tripSearchWithTwoTrips(
    SearchDirection direction,
    int stopPos,
    int searchTime,
    int expTime,
    int expTripIndex
  ) {
    var search = new TestTripScheduleSearch(direction, TWO_TRIPS);
    var result = search.search(searchTime, stopPos);

    if (expTripIndex == NOT_FOUND) {
      assertTrue(result.empty());
      assertTime(searchTime, result.earliestBoardTime());
      assertEquals(NOT_FOUND, result.tripIndex());
    } else {
      assertFalse(result.empty());
      assertEquals(stopPos, result.stopPositionInPattern());
      assertTime(expTime, result.time());
      assertTime(searchTime, result.earliestBoardTime());
      assertEquals(expTripIndex, result.tripIndex());
    }

    if (!result.empty()) {
      int secondTrip = direction.isForward() ? TRIP_TWO : TRIP_ONE;
      int firstTrip = direction.isForward() ? TRIP_ONE : TRIP_TWO;
      int tripIndex = result.tripIndex();
      var e = search.search(direction.isForward() ? T09_59_59 : T10_20_01, stopPos, tripIndex);

      if (tripIndex == secondTrip) {
        assertFalse(e.empty());
        assertEquals(firstTrip, result.tripIndex());
      } else {
        assertTrue(e.empty());
        assertEquals(NOT_FOUND, result.tripIndex());
      }
    }
  }

  private static void assertTime(int expected, int actual) {
    assertEquals(TimeUtils.timeToStrLong(expected), TimeUtils.timeToStrLong(actual));
  }

  private enum Status {
    OK,
    EMPTY,
  }
}
