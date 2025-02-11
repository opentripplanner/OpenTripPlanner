package org.opentripplanner.ext.flex.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.flex.trip.UnscheduledTrip.isUnscheduledTrip;
import static org.opentripplanner.ext.flex.trip.UnscheduledTripTest.TestCase.tc;
import static org.opentripplanner.model.PickDrop.NONE;
import static org.opentripplanner.model.StopTime.MISSING_VALUE;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.ext.flex.FlexStopTimesForTest;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.time.TimeUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

class UnscheduledTripTest {

  private static final int STOP_A = 0;
  private static final int STOP_B = 1;
  private static final int STOP_C = 2;
  private static final int T10_00 = TimeUtils.hm2time(10, 0);
  private static final int T11_00 = TimeUtils.hm2time(11, 0);
  private static final int T14_00 = TimeUtils.hm2time(14, 0);
  private static final int T15_00 = TimeUtils.hm2time(15, 0);

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  private static final RegularStop REGULAR_STOP = TEST_MODEL.stop("stop").build();

  private static final StopLocation AREA_STOP = TEST_MODEL.areaStop("area").build();

  @Nested
  class IsUnscheduledTrip {

    private static final StopTime SCHEDULED_STOP = FlexStopTimesForTest.regularStop("10:00");
    private static final StopTime UNSCHEDULED_STOP = FlexStopTimesForTest.area("10:10", "10:20");
    private static final StopTime CONTINUOUS_PICKUP_STOP = FlexStopTimesForTest.regularStopWithContinuousPickup(
      "10:30"
    );
    private static final StopTime CONTINUOUS_DROP_OFF_STOP = FlexStopTimesForTest.regularStopWithContinuousDropOff(
      "10:40"
    );

    // disallowed by the GTFS spec
    private static final StopTime FLEX_AND_CONTINUOUS_PICKUP_STOP = FlexStopTimesForTest.areaWithContinuousPickup(
      "10:50"
    );
    private static final StopTime FLEX_AND_CONTINUOUS_DROP_OFF_STOP = FlexStopTimesForTest.areaWithContinuousDropOff(
      "11:00"
    );

    static List<List<StopTime>> notUnscheduled() {
      return List.of(
        List.of(),
        List.of(SCHEDULED_STOP),
        List.of(SCHEDULED_STOP, SCHEDULED_STOP),
        List.of(SCHEDULED_STOP, SCHEDULED_STOP, SCHEDULED_STOP),
        List.of(UNSCHEDULED_STOP, SCHEDULED_STOP, UNSCHEDULED_STOP),
        List.of(UNSCHEDULED_STOP, FLEX_AND_CONTINUOUS_PICKUP_STOP),
        List.of(UNSCHEDULED_STOP, FLEX_AND_CONTINUOUS_DROP_OFF_STOP),
        List.of(FLEX_AND_CONTINUOUS_PICKUP_STOP, FLEX_AND_CONTINUOUS_DROP_OFF_STOP)
      );
    }

    @ParameterizedTest
    @MethodSource("notUnscheduled")
    void isNotUnscheduled(List<StopTime> stopTimes) {
      assertFalse(isUnscheduledTrip(stopTimes));
    }

    static List<List<StopTime>> unscheduled() {
      return List.of(
        List.of(UNSCHEDULED_STOP),
        List.of(UNSCHEDULED_STOP, SCHEDULED_STOP),
        List.of(SCHEDULED_STOP, UNSCHEDULED_STOP),
        List.of(UNSCHEDULED_STOP, UNSCHEDULED_STOP),
        List.of(UNSCHEDULED_STOP, UNSCHEDULED_STOP, UNSCHEDULED_STOP),
        Collections.nCopies(10, UNSCHEDULED_STOP),
        List.of(UNSCHEDULED_STOP, CONTINUOUS_PICKUP_STOP),
        List.of(CONTINUOUS_PICKUP_STOP, UNSCHEDULED_STOP),
        List.of(UNSCHEDULED_STOP, CONTINUOUS_DROP_OFF_STOP),
        List.of(CONTINUOUS_DROP_OFF_STOP, UNSCHEDULED_STOP)
      );
    }

    @ParameterizedTest
    @MethodSource("unscheduled")
    void isUnscheduled(List<StopTime> stopTimes) {
      assertTrue(isUnscheduledTrip(stopTimes));
    }
  }

  @Test
  void testUnscheduledTrip() {
    var fromStopTime = new StopTime();
    fromStopTime.setStop(AREA_STOP);
    fromStopTime.setFlexWindowStart(T10_00);
    fromStopTime.setFlexWindowEnd(T14_00);

    var toStopTime = new StopTime();
    toStopTime.setStop(AREA_STOP);
    toStopTime.setFlexWindowStart(T11_00);
    toStopTime.setFlexWindowEnd(T15_00);

    var trip = UnscheduledTrip
      .of(id("UNSCHEDULED"))
      .withStopTimes(List.of(fromStopTime, toStopTime))
      .build();

    assertEquals(T10_00, trip.earliestDepartureTime(STOP_A));
    assertEquals(T14_00, trip.latestArrivalTime(STOP_A));
    assertEquals(T11_00, trip.earliestDepartureTime(STOP_B));
    assertEquals(T15_00, trip.latestArrivalTime(STOP_B));

    assertEquals(PickDrop.SCHEDULED, trip.getBoardRule(STOP_A));
    assertEquals(PickDrop.SCHEDULED, trip.getAlightRule(STOP_B));
  }

  @Test
  void testUnscheduledFeederTripFromScheduledStop() {
    var fromStopTime = new StopTime();
    fromStopTime.setStop(TEST_MODEL.stop("stop").build());
    fromStopTime.setDepartureTime(T10_00);

    var toStopTime = new StopTime();
    toStopTime.setStop(AREA_STOP);
    toStopTime.setFlexWindowStart(T10_00);
    toStopTime.setFlexWindowEnd(T14_00);

    var trip = UnscheduledTrip
      .of(id("UNSCHEDULED"))
      .withStopTimes(List.of(fromStopTime, toStopTime))
      .build();

    assertEquals(T10_00, trip.earliestDepartureTime(STOP_A));
    assertEquals(T10_00, trip.latestArrivalTime(STOP_A));
    assertEquals(T10_00, trip.earliestDepartureTime(STOP_B));
    assertEquals(T14_00, trip.latestArrivalTime(STOP_B));

    assertEquals(PickDrop.SCHEDULED, trip.getBoardRule(STOP_A));
    assertEquals(PickDrop.SCHEDULED, trip.getAlightRule(STOP_B));
  }

  @Test
  void testUnscheduledFeederTripToScheduledStop() {
    var fromStopTime = new StopTime();
    fromStopTime.setStop(AREA_STOP);
    fromStopTime.setFlexWindowStart(T10_00);
    fromStopTime.setFlexWindowEnd(T14_00);

    var toStopTime = new StopTime();
    toStopTime.setStop(REGULAR_STOP);
    toStopTime.setArrivalTime(T14_00);

    var trip = UnscheduledTrip
      .of(id("UNSCHEDULED"))
      .withStopTimes(List.of(fromStopTime, toStopTime))
      .build();

    assertEquals(PickDrop.SCHEDULED, trip.getBoardRule(STOP_A));
    assertEquals(PickDrop.SCHEDULED, trip.getAlightRule(STOP_B));
  }

  static Stream<TestCase> testRegularStopToAreaEarliestDepartureTimeTestCases() {
    // REGULAR-STOP to AREA - (10:00-14:00) => (14:00)
    var tc = tc(regularDeparture("10:00"), area("10:00", "14:00"));
    return Stream.of(
      tc
        .expected("Requested departure time is before flex service departure time", "10:00")
        .request("09:00", "1h")
        .build(),
      tc
        .expected(
          "Requested departure time is before flex service departure time, max duration",
          "10:00"
        )
        .request("09:00", "4h")
        .build(),
      tc
        .expectedNotFound(
          "Requested departure time is before flex service departure time, duration too long"
        )
        .request("09:00", "4h1s")
        .build(),
      tc
        .expected("Requested departure time match flex service departure time", "10:00")
        .request("10:00", "1h")
        .build(),
      tc
        .expectedNotFound(
          "Requested departure time match flex service departure time, duration too long"
        )
        .request("10:00", "4h1s")
        .build(),
      tc
        .expectedNotFound("Requested departure time is after flex service departure time")
        .request("10:01", "0s")
        .build()
    );
  }

  @ParameterizedTest
  @MethodSource("testRegularStopToAreaEarliestDepartureTimeTestCases")
  void testRegularStopToAreaEarliestDepartureTime(TestCase tc) {
    assertEquals(
      timeToString(tc.expectedTime),
      timeToString(
        tc.trip().earliestDepartureTime(tc.requestedTime, STOP_A, STOP_B, tc.tripDuration)
      )
    );
  }

  static Stream<TestCase> testAreaToRegularStopEarliestDepartureTestCases() {
    // AREA TO REGULAR-STOP - (10:00-14:00) => (14:00)
    var tc = tc(area("10:00", "14:00"), regularArrival("14:00"));
    return Stream.of(
      tc
        .expected(
          "Requested departure time is before flex service departure window start, no duration",
          "14:00"
        )
        .request("09:59", "0s")
        .build(),
      tc
        .expected(
          "Requested departure time is before flex service departure window start, duration 1h",
          "13:00"
        )
        .request("09:59", "1h")
        .build(),
      tc
        .expected(
          "Requested departure time is before flex service departure window start, max duration",
          "10:00"
        )
        .request("09:59", "4h")
        .build(),
      tc
        .expectedNotFound(
          "Requested departure time is before flex service departure window start, duration to long"
        )
        .request("09:59", "4h1s")
        .build(),
      tc
        .expected(
          "Requested departure time is inside flex service departure window - time-shift to match fixed arrival time",
          "13:00"
        )
        .request("11:00", "1h")
        .build(),
      tc
        .expected("Requested departure time is match flex service departure end", "14:00")
        .request("14:00", "0s")
        .build(),
      tc
        .expectedNotFound(
          "Requested departure time is match flex service departure end, duration too long"
        )
        .request("14:00", "1s")
        .build()
    );
  }

  @ParameterizedTest
  @MethodSource("testAreaToRegularStopEarliestDepartureTestCases")
  void testAreaToRegularStopEarliestDepartureTime(TestCase tc) {
    assertEquals(
      timeToString(tc.expectedTime),
      timeToString(
        tc.trip().earliestDepartureTime(tc.requestedTime, STOP_A, STOP_B, tc.tripDuration)
      )
    );
  }

  static Stream<TestCase> testAreaToAreaEarliestDepartureTimeTestCases() {
    // AREA TO AREA - (10:00-14:00) => (11:00-15:00)
    var tc = tc(area("10:00", "14:00"), area("11:00", "15:00"));
    return Stream.of(
      tc
        .expected(
          "Requested departure time is before flex service departure window start, duration 1h",
          "10:00"
        )
        .request("09:23", "1h")
        .build(),
      tc
        .expected(
          "Requested departure time is before flex service departure window start, duration 5m, expect time-shift to match service arrival window",
          "10:55"
        )
        .request("09:23", "5m")
        .build(),
      tc
        .expected(
          "Requested departure time is inside flex service departure window, duration is just enough",
          "10:00"
        )
        .request("10:00", "1h")
        .build(),
      tc
        .expected(
          "Requested departure time is inside flex service departure window, duration is to small and requires time-shifting to match arrival time window",
          "10:30"
        )
        .request("10:00", "30m")
        .build(),
      tc
        .expected(
          "Requested departure time match latest flex service departure time, duration ok",
          "14:00"
        )
        .request("14:00", "1h")
        .build(),
      tc
        .expectedNotFound(
          "Requested departure time match latest flex service departure time, duration to long"
        )
        .request("14:00", "1h1s")
        .build()
    );
  }

  @ParameterizedTest
  @MethodSource("testAreaToAreaEarliestDepartureTimeTestCases")
  void testAreaToAreaEarliestDepartureTime(TestCase tc) {
    assertEquals(
      timeToString(tc.expectedTime),
      timeToString(
        tc.trip().earliestDepartureTime(tc.requestedTime, STOP_A, STOP_B, tc.tripDuration)
      )
    );
  }

  static Stream<TestCase> testRegularStopToAreaLatestArrivalTimeTestCases() {
    // REGULAR-STOP to AREA - (10:00-14:00) => (14:00)
    var tc = tc(regularDeparture("10:00"), area("10:00", "14:00"));
    return Stream.of(
      tc
        .expectedNotFound("Requested arrival time is before flex service arrival window start")
        .request("09:59", "0s")
        .build(),
      tc
        .expected("Match flex service arrival window start", "10:00")
        .request("10:00", "0s")
        .build(),
      tc
        .expectedNotFound("Match flex service arrival window start, but duration is to long")
        .request("10:00", "1s")
        .build(),
      tc.expected("Match flex service departure time", "11:00").request("11:00", "1h").build(),
      tc
        .expectedNotFound("Match flex service departure time, duration too long")
        .request("11:00", "1h1s")
        .build(),
      tc
        .expected("Match flex service arrival window end with matching duration", "14:00")
        .request("14:00", "4h")
        .build(),
      tc
        .expected("Match flex service arrival window end, duration too short", "13:00")
        .request("14:00", "3h")
        .build(),
      tc
        .expectedNotFound("Match flex service arrival window end, with duration too long")
        .request("14:00", "4h1s")
        .build(),
      tc
        .expected("Request arrival after flex service arrival window end", "12:00")
        .request("14:30", "2h")
        .build(),
      tc
        .expectedNotFound(
          "Request arrival after flex service arrival window end, duration too long"
        )
        .request("14:01", "4h1s")
        .build()
    );
  }

  @ParameterizedTest
  @MethodSource("testRegularStopToAreaLatestArrivalTimeTestCases")
  void testRegularStopToAreaLatestArrivalTime(TestCase tc) {
    assertEquals(
      timeToString(tc.expectedTime),
      timeToString(tc.trip().latestArrivalTime(tc.requestedTime, STOP_A, STOP_B, tc.tripDuration))
    );
  }

  static Stream<TestCase> testAreaToRegularStopLatestArrivalTimeTestCases() {
    // AREA TO REGULAR-STOP - (10:00-14:00) => (14:00)
    var tc = tc(area("10:00", "14:00"), regularArrival("14:00"));
    return Stream.of(
      tc
        .expectedNotFound("Requested arrival time is before flex service arrival window start")
        .request("13:59", "0s")
        .build(),
      tc
        .expected("Match flex service arrival time, no duration", "14:00")
        .request("14:00", "0s")
        .build(),
      tc
        .expected("Match flex service arrival time, 1h duration", "14:00")
        .request("14:00", "1h")
        .build(),
      tc
        .expected("Match flex service arrival time, max duration", "14:00")
        .request("14:00", "4h")
        .build(),
      tc
        .expectedNotFound("Match flex service arrival time, duration too long")
        .request("14:00", "4h1s")
        .build(),
      tc
        .expected("Requested arrival time is after flex service arrival time", "14:00")
        .request("14:01", "1h")
        .build(),
      tc
        .expected(
          "Requested arrival time is after flex service arrival time, max duration",
          "14:00"
        )
        .request("14:30", "4h")
        .build(),
      tc
        .expectedNotFound(
          "Requested arrival time is after flex service arrival time, duration to long"
        )
        .request("14:30", "4h1s")
        .build()
    );
  }

  @ParameterizedTest
  @MethodSource("testAreaToRegularStopLatestArrivalTimeTestCases")
  void testAreaToRegularStopLatestArrivalTime(TestCase tc) {
    assertEquals(
      timeToString(tc.expectedTime),
      timeToString(tc.trip().latestArrivalTime(tc.requestedTime, STOP_A, STOP_B, tc.tripDuration))
    );
  }

  static Stream<TestCase> testAreaToAreaLatestArrivalTimeTestCases() {
    // AREA TO AREA - (10:00-14:00) => (11:00-15:00)
    var tc = tc(area("10:00", "14:00"), area("11:00", "15:00"));
    return Stream.of(
      tc
        .expectedNotFound("Requested arrival time is before flex service arrival window start")
        .request("10:59", "0s")
        .build(),
      tc.expected("Match flex service start of window", "11:00").request("11:00", "1h").build(),
      tc
        .expectedNotFound("Match flex service start of window, but duration is too long")
        .request("11:00", "1h1s")
        .build(),
      tc.expected("Match flex service end of window", "15:00").request("15:00", "1h").build(),
      tc
        .expected("Match flex service end of window, but duration is 1 minute too short", "14:59")
        .request("15:00", "59m")
        .build(),
      tc
        .expected("Requested arrival time is after flex service end of window", "15:00")
        .request("15:01", "3h")
        .build(),
      tc.expected("Max duration", "15:00").request("16:00", "5h").build(),
      tc.expectedNotFound("Duration is too long").request("16:00", "5h1s").build()
    );
  }

  @ParameterizedTest
  @MethodSource("testAreaToAreaLatestArrivalTimeTestCases")
  void testAreaToAreaLatestArrivalTime(TestCase tc) {
    assertEquals(
      timeToString(tc.expectedTime),
      timeToString(tc.trip().latestArrivalTime(tc.requestedTime, STOP_A, STOP_B, tc.tripDuration))
    );
  }

  static Stream<TestCase> multipleAreasEarliestDepartureTimeTestCases() {
    var from = area("10:00", "10:05");
    var middle = area("10:10", "10:15");
    var to = area("10:20", "10:25");

    var tc = new TestCase.Builder(from, to).withStopTimes(List.of(from, middle, to));

    return Stream.of(
      tc
        .expected(
          "Requested departure time is after flex service departure window start, duration 21m",
          "10:01"
        )
        .request("10:01", "21m")
        .build(),
      tc
        .expected(
          "Requested departure time is before flex service departure window start, duration 1h",
          "10:00"
        )
        .request("09:50", "24m")
        .build()
    );
  }

  @ParameterizedTest
  @MethodSource("multipleAreasEarliestDepartureTimeTestCases")
  void testMultipleAreasEarliestDepartureTime(TestCase tc) {
    assertEquals(
      timeToString(tc.expectedTime),
      timeToString(
        tc.trip().earliestDepartureTime(tc.requestedTime, STOP_A, STOP_C, tc.tripDuration)
      )
    );
  }

  @Test
  void boardingAlighting() {
    var AREA_STOP1 = TEST_MODEL.areaStop("area-1").build();
    var AREA_STOP2 = TEST_MODEL.areaStop("area-2").build();
    var AREA_STOP3 = TEST_MODEL.areaStop("area-3").build();

    var first = area(AREA_STOP1, "10:00", "10:05");
    first.setDropOffType(NONE);
    var second = area(AREA_STOP2, "10:10", "10:15");
    second.setPickupType(NONE);
    var third = area(AREA_STOP3, "10:20", "10:25");

    var trip = TestCase
      .tc(first, third)
      .withStopTimes(List.of(first, second, third))
      .build()
      .trip();

    assertTrue(trip.isBoardingPossible(AREA_STOP1));
    assertFalse(trip.isAlightingPossible(AREA_STOP1));

    assertFalse(trip.isBoardingPossible(AREA_STOP2));
    assertTrue(trip.isAlightingPossible(AREA_STOP2));
  }

  private static String timeToString(int time) {
    return TimeUtils.timeToStrCompact(time, MISSING_VALUE, "MISSING_VALUE");
  }

  private static StopTime area(String startTime, String endTime) {
    return area(AREA_STOP, endTime, startTime);
  }

  private static StopTime area(StopLocation areaStop, String endTime, String startTime) {
    var stopTime = new StopTime();
    stopTime.setStop(areaStop);
    stopTime.setFlexWindowStart(TimeUtils.time(startTime));
    stopTime.setFlexWindowEnd(TimeUtils.time(endTime));
    return stopTime;
  }

  private static StopTime regularDeparture(String departureTime) {
    return regularStopTime(MISSING_VALUE, TimeUtils.time(departureTime));
  }

  private static StopTime regularArrival(String arrivalTime) {
    return regularStopTime(TimeUtils.time(arrivalTime), MISSING_VALUE);
  }

  private static StopTime regularStopTime(int arrivalTime, int departureTime) {
    var stopTime = new StopTime();
    stopTime.setStop(REGULAR_STOP);
    stopTime.setArrivalTime(arrivalTime);
    stopTime.setDepartureTime(departureTime);
    return stopTime;
  }

  record TestCase(
    StopTime from,
    StopTime to,
    List<StopTime> stopTimes,
    String expectedDescription,
    int expectedTime,
    int requestedTime,
    int tripDuration
  ) {
    static Builder tc(StopTime start, StopTime end) {
      return new TestCase.Builder(start, end);
    }

    UnscheduledTrip trip() {
      return UnscheduledTrip.of(id("UNSCHEDULED")).withStopTimes(stopTimes).build();
    }

    @Override
    public String toString() {
      return ToStringBuilder
        .of(TestCase.class)
        .addObj(
          "expected",
          expectedDescription +
          ": " +
          TimeUtils.timeToStrCompact(expectedTime, MISSING_VALUE, "MISSING")
        )
        .addServiceTime("requested", requestedTime)
        .addDurationSec("duration", tripDuration)
        .addObj("departure", departureToStr(from))
        .addObj("arrival", arrivalToStr(to))
        .toString();
    }

    private static String departureToStr(StopTime st) {
      return timeToStr(st.getDepartureTime(), st.getFlexWindowStart(), st.getFlexWindowEnd());
    }

    private static String arrivalToStr(StopTime st) {
      return timeToStr(st.getArrivalTime(), st.getFlexWindowStart(), st.getFlexWindowEnd());
    }

    private static String timeToStr(int time, int start, int end) {
      return time == MISSING_VALUE
        ? (TimeUtils.timeToStrCompact(start) + " - " + TimeUtils.timeToStrCompact(end))
        : TimeUtils.timeToStrCompact(time);
    }

    private static class Builder {

      private final StopTime from;
      private final StopTime to;
      private String expectedDescription;
      private List<StopTime> stopTimes;
      private int expectedTime;
      private int requestedTime;
      private int tripDuration;

      public Builder(StopTime from, StopTime to) {
        this.from = from;
        this.to = to;
      }

      public Builder withStopTimes(List<StopTime> stopTimes) {
        this.stopTimes = stopTimes;
        return this;
      }

      Builder expected(String expectedDescription, String expectedTime) {
        this.expectedDescription = expectedDescription;
        this.expectedTime = TimeUtils.time(expectedTime);
        return this;
      }

      Builder expectedNotFound(String expectedDescription) {
        this.expectedDescription = expectedDescription;
        this.expectedTime = MISSING_VALUE;
        return this;
      }

      Builder request(String requestedTime, String tripDuration) {
        this.requestedTime = TimeUtils.time(requestedTime);
        this.tripDuration = DurationUtils.durationInSeconds(tripDuration);
        return this;
      }

      TestCase build() {
        return new TestCase(
          from,
          to,
          Objects.requireNonNullElse(stopTimes, List.of(from, to)),
          expectedDescription,
          expectedTime,
          requestedTime,
          tripDuration
        );
      }
    }
  }
}
