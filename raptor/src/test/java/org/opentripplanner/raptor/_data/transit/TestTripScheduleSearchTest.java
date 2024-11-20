package org.opentripplanner.raptor._data.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor.api.model.RaptorConstants.NOT_FOUND;
import static org.opentripplanner.raptor.api.model.SearchDirection.FORWARD;
import static org.opentripplanner.raptor.api.model.SearchDirection.REVERSE;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor._data.RaptorTestConstants;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.utils.time.TimeUtils;

/**
 * This tests another test class - we do it to make sure the
 * {@link TestTripScheduleSearch} is following the Raptor SPI contract.
 */
class TestTripScheduleSearchTest implements RaptorTestConstants {

  private static final int NOT_DEFINED = -999_999;
  private static final int T09_59_59 = TimeUtils.time("09:59:59");
  private static final int T10_00_00 = TimeUtils.time("10:00:00");
  private static final int T10_00_01 = TimeUtils.time("10:00:01");
  private static final int T10_09_59 = TimeUtils.time("10:09:59");
  private static final int T10_10_00 = TimeUtils.time("10:10:00");
  private static final int T10_10_01 = TimeUtils.time("10:10:01");
  private static final int T10_19_59 = TimeUtils.time("10:19:59");
  private static final int T10_20_00 = TimeUtils.time("10:20:00");
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

  static List<Arguments> tripSearchWithOneTripTestCases() {
    return List.of(
      Arguments.of(FORWARD, STOP_POS_0, T09_59_59, T10_00_00, Status.OK),
      Arguments.of(FORWARD, STOP_POS_0, T10_00_00, T10_00_00, Status.OK),
      Arguments.of(FORWARD, STOP_POS_0, T10_00_01, NOT_DEFINED, Status.EMPTY),
      Arguments.of(FORWARD, STOP_POS_1, T10_09_59, T10_10_00, Status.OK),
      Arguments.of(FORWARD, STOP_POS_1, T10_10_00, T10_10_00, Status.OK),
      Arguments.of(FORWARD, STOP_POS_1, T10_10_01, NOT_DEFINED, Status.EMPTY),
      Arguments.of(REVERSE, STOP_POS_0, T09_59_59, NOT_DEFINED, Status.EMPTY),
      Arguments.of(REVERSE, STOP_POS_0, T10_00_00, T10_00_00, Status.OK),
      Arguments.of(REVERSE, STOP_POS_0, T10_00_01, T10_00_00, Status.OK),
      Arguments.of(REVERSE, STOP_POS_1, T10_09_59, NOT_DEFINED, Status.EMPTY),
      Arguments.of(REVERSE, STOP_POS_1, T10_10_00, T10_10_00, Status.OK),
      Arguments.of(REVERSE, STOP_POS_1, T10_10_01, T10_10_00, Status.OK)
    );
  }

  @ParameterizedTest
  @MethodSource("tripSearchWithOneTripTestCases")
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

  static List<Arguments> tripSearchWithTwoTripsTestCases() {
    return List.of(
      Arguments.of(FORWARD, STOP_POS_0, T09_59_59, T10_00_00, TRIP_ONE),
      Arguments.of(FORWARD, STOP_POS_0, T10_00_00, T10_00_00, TRIP_ONE),
      Arguments.of(FORWARD, STOP_POS_0, T10_00_01, T10_10_00, TRIP_TWO),
      Arguments.of(FORWARD, STOP_POS_0, T10_09_59, T10_10_00, TRIP_TWO),
      Arguments.of(FORWARD, STOP_POS_0, T10_10_00, T10_10_00, TRIP_TWO),
      Arguments.of(FORWARD, STOP_POS_0, T10_10_01, NOT_DEFINED, NOT_FOUND),
      Arguments.of(FORWARD, STOP_POS_1, T10_09_59, T10_10_00, TRIP_ONE),
      Arguments.of(FORWARD, STOP_POS_1, T10_10_00, T10_10_00, TRIP_ONE),
      Arguments.of(FORWARD, STOP_POS_1, T10_10_01, T10_20_00, TRIP_TWO),
      Arguments.of(FORWARD, STOP_POS_1, T10_19_59, T10_20_00, TRIP_TWO),
      Arguments.of(FORWARD, STOP_POS_1, T10_20_00, T10_20_00, TRIP_TWO),
      Arguments.of(FORWARD, STOP_POS_1, T10_20_01, NOT_DEFINED, NOT_FOUND),
      Arguments.of(REVERSE, STOP_POS_0, T09_59_59, NOT_DEFINED, NOT_FOUND),
      Arguments.of(REVERSE, STOP_POS_0, T10_00_00, T10_00_00, TRIP_ONE),
      Arguments.of(REVERSE, STOP_POS_0, T10_00_01, T10_00_00, TRIP_ONE),
      Arguments.of(REVERSE, STOP_POS_0, T10_09_59, T10_00_00, TRIP_ONE),
      Arguments.of(REVERSE, STOP_POS_0, T10_10_00, T10_10_00, TRIP_TWO),
      Arguments.of(REVERSE, STOP_POS_0, T10_10_01, T10_10_00, TRIP_TWO),
      Arguments.of(REVERSE, STOP_POS_1, T10_09_59, NOT_DEFINED, NOT_FOUND),
      Arguments.of(REVERSE, STOP_POS_1, T10_10_00, T10_10_00, TRIP_ONE),
      Arguments.of(REVERSE, STOP_POS_1, T10_10_01, T10_10_00, TRIP_ONE),
      Arguments.of(REVERSE, STOP_POS_1, T10_19_59, T10_10_00, TRIP_ONE),
      Arguments.of(REVERSE, STOP_POS_1, T10_20_00, T10_20_00, TRIP_TWO),
      Arguments.of(REVERSE, STOP_POS_1, T10_20_01, T10_20_00, TRIP_TWO)
    );
  }

  @ParameterizedTest
  @MethodSource("tripSearchWithTwoTripsTestCases")
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
