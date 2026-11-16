package org.opentripplanner.transit.model.timetable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.transit.model.timetable.TimetableValidationError.ErrorCode.NEGATIVE_DWELL_TIME;
import static org.opentripplanner.transit.model.timetable.TimetableValidationError.ErrorCode.NEGATIVE_HOP_TIME;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.core.model.accessibility.Accessibility;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.site.RegularStop;

class RealTimeTripTimesTest {

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();

  private static final String TRIP_ID = "testTripId";

  private static final List<FeedScopedId> STOP_IDS = List.of(
    id("A"),
    id("B"),
    id("C"),
    id("D"),
    id("E"),
    id("F"),
    id("G"),
    id("H")
  );

  static TripTimes createInitialTripTimes() {
    Trip trip = TransitRepositoryForTest.trip(TRIP_ID).build();

    List<StopTime> stopTimes = new ArrayList<>();

    for (int i = 0; i < STOP_IDS.size(); ++i) {
      StopTime stopTime = new StopTime();

      RegularStop stop = TEST_MODEL.stop(STOP_IDS.get(i).getId(), 0.0, 0.0).build();
      stopTime.setStop(stop);
      stopTime.setArrivalTime(i * 60);
      stopTime.setDepartureTime(i * 60);
      stopTime.setStopSequence(i * 10);
      stopTimes.add(stopTime);
    }

    return TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());
  }

  @Nested
  class Headsign {

    private static final NonLocalizedString STOP_TEST_DIRECTION = new NonLocalizedString(
      "STOP TEST DIRECTION"
    );
    private static final NonLocalizedString DIRECTION = new NonLocalizedString("DIRECTION");
    private static final StopTime EMPTY_STOPPOINT = new StopTime();

    @Test
    void shouldHandleBothNullScenario() {
      Trip trip = TransitRepositoryForTest.trip("TRIP").build();
      List<StopTime> stopTimes = List.of(EMPTY_STOPPOINT, EMPTY_STOPPOINT, EMPTY_STOPPOINT);

      TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());

      I18NString headsignFirstStop = tripTimes.getHeadsign(0);
      assertNull(headsignFirstStop);
    }

    @Test
    void shouldHandleTripOnlyHeadSignScenario() {
      Trip trip = TransitRepositoryForTest.trip("TRIP").withHeadsign(DIRECTION).build();
      List<StopTime> stopTimes = List.of(EMPTY_STOPPOINT, EMPTY_STOPPOINT, EMPTY_STOPPOINT);

      TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());

      I18NString headsignFirstStop = tripTimes.getHeadsign(0);
      assertEquals(DIRECTION, headsignFirstStop);
    }

    @Test
    void shouldHandleStopsOnlyHeadSignScenario() {
      Trip trip = TransitRepositoryForTest.trip("TRIP").build();
      StopTime stopWithHeadsign = new StopTime();
      stopWithHeadsign.setStopHeadsign(STOP_TEST_DIRECTION);
      List<StopTime> stopTimes = List.of(stopWithHeadsign, stopWithHeadsign, stopWithHeadsign);

      TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());

      I18NString headsignFirstStop = tripTimes.getHeadsign(0);
      assertEquals(STOP_TEST_DIRECTION, headsignFirstStop);
    }

    @Test
    void shouldHandleStopsEqualToTripHeadSignScenario() {
      Trip trip = TransitRepositoryForTest.trip("TRIP").withHeadsign(DIRECTION).build();
      StopTime stopWithHeadsign = new StopTime();
      stopWithHeadsign.setStopHeadsign(DIRECTION);
      List<StopTime> stopTimes = List.of(stopWithHeadsign, stopWithHeadsign, stopWithHeadsign);

      TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());

      I18NString headsignFirstStop = tripTimes.getHeadsign(0);
      assertEquals(DIRECTION, headsignFirstStop);
    }

    @Test
    void shouldHandleDifferingTripAndStopHeadSignScenario() {
      Trip trip = TransitRepositoryForTest.trip("TRIP").withHeadsign(DIRECTION).build();
      StopTime stopWithHeadsign = new StopTime();
      stopWithHeadsign.setStopHeadsign(STOP_TEST_DIRECTION);
      List<StopTime> stopTimes = List.of(stopWithHeadsign, EMPTY_STOPPOINT, EMPTY_STOPPOINT);

      TripTimes tripTimes = TripTimesFactory.tripTimes(trip, stopTimes, new Deduplicator());

      I18NString headsignFirstStop = tripTimes.getHeadsign(0);
      assertEquals(STOP_TEST_DIRECTION, headsignFirstStop);

      I18NString headsignSecondStop = tripTimes.getHeadsign(1);
      assertEquals(DIRECTION, headsignSecondStop);
    }
  }

  @Test
  public void testStopUpdate() {
    RealTimeTripTimesBuilder builder = createInitialTripTimes().createRealTimeFromScheduledTimes();

    builder.withArrivalTime(3, 190);
    builder.withDepartureTime(3, 190);
    builder.withArrivalTime(5, 311);
    builder.withDepartureTime(5, 312);

    var updatedTripTimesA = builder.build();

    assertEquals(190, updatedTripTimesA.getArrivalTime(3));
    assertEquals(190, updatedTripTimesA.getDepartureTime(3));
    assertEquals(311, updatedTripTimesA.getArrivalTime(5));
    assertEquals(312, updatedTripTimesA.getDepartureTime(5));
  }

  @Test
  public void testOnlyScheduledTimesAreCopied() {
    var initialTripTimes = createInitialTripTimes();
    var realTimeTripTimes = initialTripTimes
      .createRealTimeFromScheduledTimes()
      .withArrivalDelay(3, -1)
      .build();
    assertEquals(
      initialTripTimes.getArrivalTime(3),
      realTimeTripTimes.createRealTimeFromScheduledTimes().build().getArrivalTime(3)
    );
  }

  @Test
  public void testIncompleteTimes() {
    assertThrows(
      DataValidationException.class,
      createInitialTripTimes().createRealTimeWithoutScheduledTimes()::build
    );
  }

  @Test
  public void testCompleteTimes() {
    var builder = createInitialTripTimes().createRealTimeWithoutScheduledTimes();
    var delay = 30;
    for (var i = 0; i < builder.numberOfStops(); ++i) {
      builder.withArrivalDelay(i, delay).withDepartureDelay(i, delay);
    }
    var tripTimes = builder.build();
    for (var i = 0; i < tripTimes.getNumStops(); ++i) {
      assertEquals(delay, tripTimes.getArrivalDelay(i));
      assertEquals(delay, tripTimes.getDepartureDelay(i));
    }
  }

  @Test
  public void testPassedUpdate() {
    RealTimeTripTimesBuilder builder = createInitialTripTimes().createRealTimeFromScheduledTimes();

    builder.withDepartureTime(0, 30);
    var updatedTripTimesA = builder.build();

    assertEquals(30, updatedTripTimesA.getDepartureTime(0));
    assertEquals(60, updatedTripTimesA.getArrivalTime(1));
  }

  /**
   * Test hop time with stop cancellations when buses run late.
   * <pre>
   * Scheduled: 5 at 300, 6 at 360, 7 at 420
   * Test case: 5 at 421, 6 cancelled (with delay passing through skipped stop at 481), 7 at 420
   * Result: Error to be present at stop 7, due to negative hop time.
   * </pre>
   */
  @Test
  public void testNegativeHopTimeWithStopCancellations() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();

    builder.withDepartureTime(5, 421);
    builder.withArrivalTime(6, 481);
    builder.withDepartureTime(6, 481);
    builder.withCanceled(6);
    builder.withArrivalTime(7, 420);

    var error = assertThrows(DataValidationException.class, builder::build);
    assertEquals(
      "NEGATIVE_HOP_TIME for stop position 7 in trip Trip{F:testTripId RRtestTripId}.",
      error.error().message()
    );
  }

  /**
   * Test hop time with stop cancellations when buses run early.
   * <pre>
   * Scheduled: 5 at 300, 6 at 360, 7 at 420
   * Test case: 5 at 300, 6 cancelled (without delay from schedule), 7 at 320.
   * Result: Expect error at stop 7, due to negative hop time.
   * </pre>
   */
  @Test
  public void testPositiveHopTimeWithStopCancellationsEarly() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();

    builder.withDepartureTime(5, 300);
    builder.withCanceled(6);
    builder.withArrivalTime(7, 320);

    var error = assertThrows(DataValidationException.class, builder::build);
    assertEquals(
      "NEGATIVE_HOP_TIME for stop position 7 in trip Trip{F:testTripId RRtestTripId}.",
      error.error().message()
    );
  }

  /**
   * Test positive hop time with stop cancellations at the beginning of the trip.
   * <pre>
   * Scheduled: 0 at 0, 1 at 60, 2 at 120, 3 at 180, 4 at 240, 5 at 300, 6 at 360, 7 at 420
   * Test case: 0 and 1 cancelled, start trip at stop 2 at time 0.
   * Result: Expect error, since the arrival time at 2 is earlier than the departure time at 1.
   * </pre>
   */
  @Test
  public void testPositiveHopTimeWithTerminalCancellation() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();

    builder.withCanceled(0);
    builder.withCanceled(1);
    builder.withArrivalTime(2, 0);
    builder.withDepartureTime(2, 10);

    var error = assertThrows(DataValidationException.class, builder::build);
    assertEquals(
      "NEGATIVE_HOP_TIME for stop position 2 in trip Trip{F:testTripId RRtestTripId}.",
      error.error().message()
    );
  }

  /**
   * Test multiple cancelled stops together.
   * <pre>
   * Scheduled: 0 at 0, 1 at 60, 2 at 120, 3 at 180, 4 at 240, 5 at 300, 6 at 360, 7 at 420
   * Test case: 0 at 0, 1 to 6 are cancelled, 7 at time 350.
   * Result: Expect errors, since 7 is less than the scheduled time at 6.
   * </pre>
   */
  @Test
  public void testMultipleStopCancellations() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();

    builder.withCanceled(1);
    builder.withCanceled(2);
    builder.withCanceled(3);
    builder.withCanceled(4);
    builder.withCanceled(5);
    builder.withCanceled(6);
    builder.withArrivalTime(7, 350);
    builder.withDepartureTime(7, 350);

    var error = assertThrows(DataValidationException.class, builder::build);
    assertEquals(
      "NEGATIVE_HOP_TIME for stop position 7 in trip Trip{F:testTripId RRtestTripId}.",
      error.error().message()
    );
  }

  /**
   * Test multiple cancelled stops together.
   * <pre>
   * Scheduled: 0 at 0, 1 at 60, 2 at 120, 3 at 180, 4 at 240, 5 at 300, 6 at 360, 7 at 420
   * Test case: 0 at 0, 1 2 cancelled, 3 at 90, 4 5 6 cancelled, 7 at time 240.
   * Result: Expect errors, since 3 is less than scheduled time at 2.
   * </pre>
   */
  @Test
  public void testMultipleStopCancellations2() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();

    builder.withCanceled(1);
    builder.withCanceled(2);
    builder.withArrivalTime(3, 90);
    builder.withDepartureTime(3, 90);
    builder.withCanceled(4);
    builder.withCanceled(5);
    builder.withCanceled(6);
    builder.withArrivalTime(7, 240);
    builder.withDepartureTime(7, 240);

    var error = assertThrows(DataValidationException.class, builder::build);
    assertEquals(
      "NEGATIVE_HOP_TIME for stop position 3 in trip Trip{F:testTripId RRtestTripId}.",
      error.error().message()
    );
  }

  @Test
  public void testNonIncreasingUpdateCrossingMidnight() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();

    // "Yesterday"
    builder.withArrivalTime(0, -300);
    builder.withDepartureTime(0, 50);

    builder.build();
  }

  @Test
  public void testDelay() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();
    builder.withDepartureDelay(0, 10);
    builder.withArrivalDelay(6, 13);

    assertEquals(10, builder.getDepartureTime(0));
    assertEquals(6 * 60 + 13, builder.getArrivalTime(6));
  }

  @Test
  public void testCancel() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();
    builder.withCanceled();
    assertTrue(builder.build().isCanceled());
  }

  @Test
  public void testNoData() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();
    builder.withNoData(1);
    var updatedTripTimesA = builder.build();
    assertFalse(updatedTripTimesA.isNoDataStop(0));
    assertTrue(updatedTripTimesA.isNoDataStop(1));
    assertFalse(updatedTripTimesA.isNoDataStop(2));
  }

  @Test
  public void testRealTimeUpdated() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();
    assertFalse(builder.build().isRealTimeUpdated(1));
    builder.withArrivalTime(1, 70);
    builder.withDepartureTime(1, 70);
    assertTrue(builder.build().isRealTimeUpdated(1));
    builder.withNoData(1);
    var updatedTripTimesA = builder.build();
    assertTrue(updatedTripTimesA.isRealTimeUpdated(0));
    assertFalse(updatedTripTimesA.isRealTimeUpdated(1));
  }

  @Test
  public void stopPositions() {
    var builder = createInitialTripTimes().createRealTimeFromScheduledTimes();
    var list = builder.listStopPositions().boxed().toList();
    assertEquals(List.of(0, 1, 2, 3, 4, 5, 6, 7), list);
  }

  @Nested
  class GtfsStopSequence {

    @Test
    void gtfsSequence() {
      var stopIndex = createInitialTripTimes().gtfsSequenceOfStopIndex(2);
      assertEquals(20, stopIndex);
    }

    @Test
    void stopIndexOfGtfsSequence() {
      var stopIndex = createInitialTripTimes().stopPositionForGtfsSequence(40);
      assertTrue(stopIndex.isPresent());
      assertEquals(4, stopIndex.getAsInt());
    }

    @Test
    void unknownGtfsSequence() {
      var stopIndex = createInitialTripTimes().stopPositionForGtfsSequence(4);
      assertTrue(stopIndex.isEmpty());
    }
  }

  @Test
  public void validateNegativeDwellTime() {
    var expMsg = "NEGATIVE_DWELL_TIME for stop position 3 in trip Trip{F:testTripId RRtestTripId}.";
    var tt = createInitialTripTimes();
    var updatedTt = tt.createRealTimeFromScheduledTimes();

    updatedTt.withArrivalTime(3, 69);
    updatedTt.withDepartureTime(3, 68);

    var ex = assertThrows(DataValidationException.class, updatedTt::build);
    var error = (TimetableValidationError) ex.error();

    assertEquals(3, error.stopIndex());
    assertEquals(NEGATIVE_DWELL_TIME, error.code());
    assertEquals(expMsg, error.message());
    assertEquals(expMsg, ex.getMessage());
  }

  @Test
  public void vehicleIdIsStoredAndRetrieved() {
    var tripTimes = (RealTimeTripTimes) createInitialTripTimes()
      .createRealTimeFromScheduledTimes()
      .withVehicleId(id("BUS-42"))
      .build();

    assertTrue(tripTimes.getVehicleId().isPresent());
    assertEquals(id("BUS-42"), tripTimes.getVehicleId().get());
  }

  @Test
  public void vehicleIdIsEmptyByDefault() {
    var tripTimes = (RealTimeTripTimes) createInitialTripTimes()
      .createRealTimeFromScheduledTimes()
      .build();

    assertTrue(tripTimes.getVehicleId().isEmpty());
  }

  @Test
  public void validateNegativeHopTime() {
    var expMsg = "NEGATIVE_HOP_TIME for stop position 2 in trip Trip{F:testTripId RRtestTripId}.";
    var tt = createInitialTripTimes();
    var updatedTt = tt.createRealTimeFromScheduledTimes();

    updatedTt.withDepartureTime(1, 100);
    updatedTt.withArrivalTime(2, 99);

    var ex = assertThrows(DataValidationException.class, updatedTt::build);
    var error = (TimetableValidationError) ex.error();

    assertEquals(2, error.stopIndex());
    assertEquals(NEGATIVE_HOP_TIME, error.code());
    assertEquals(expMsg, error.message());
    assertEquals(expMsg, ex.getMessage());
  }

  @Test
  void testEqualsAndHashCode() {
    var scheduled = createInitialTripTimes();
    var subject = fullBuilder(scheduled).build();
    var same = fullBuilder(scheduled).build();

    assertEquals(subject, same);
    assertEquals(subject.hashCode(), same.hashCode());
  }

  private static Stream<Arguments> equalsAndHashCodeFieldCases() {
    return Stream.of(
      fieldCase("scheduledTripTimes", b -> b.withServiceCode(42)),
      fieldCase("arrivalTimes", b -> b.withArrivalTime(1, 65)),
      fieldCase("departureTimes", b -> b.withDepartureTime(1, 75)),
      fieldCase("stopRealTimeStates", b -> b.withCanceled(3)),
      fieldCase("extraCalls", b -> b.withExtraCall(2, true)),
      fieldCase("hasArrived", b -> b.withHasArrived(2, true)),
      fieldCase("hasDeparted", b -> b.withHasDeparted(2, true)),
      fieldCase("tripHeadsign", b -> b.withTripHeadsign(new NonLocalizedString("Other"))),
      fieldCase("stopHeadsigns", b -> b.withStopHeadsign(2, new NonLocalizedString("Other"))),
      fieldCase("occupancyStatus", b -> b.withOccupancyStatus(2, OccupancyStatus.FULL)),
      fieldCase("wheelchairAccessibility", b ->
        b.withWheelchairAccessibility(Accessibility.NOT_POSSIBLE)
      ),
      fieldCase("state", RealTimeTripTimesBuilder::withModifiedTripPattern),
      fieldCase("partialReplacedBys", b -> b.withPartialReplacedBy(2, 3, tripOnServiceDate())),
      fieldCase("vehicleId", b -> b.withVehicleId(id("otherVehicle")))
    );
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("equalsAndHashCodeFieldCases")
  void fieldIsPartOfEqualsAndHashCode(String field, Consumer<RealTimeTripTimesBuilder> change) {
    var scheduled = createInitialTripTimes();
    var subject = fullBuilder(scheduled).build();
    var builder = fullBuilder(scheduled);
    change.accept(builder);
    var other = builder.build();

    assertNotEquals(subject, other);
    assertNotEquals(subject.hashCode(), other.hashCode());
  }

  private static Arguments fieldCase(String field, Consumer<RealTimeTripTimesBuilder> change) {
    return Arguments.of(field, change);
  }

  private static RealTimeTripTimesBuilder fullBuilder(TripTimes scheduled) {
    return scheduled
      .createRealTimeFromScheduledTimes()
      .withArrivalTime(1, 70)
      .withDepartureTime(1, 70)
      .withRealTimeUpdated()
      .withNoData(2)
      .withExtraCall(1, true)
      .withHasArrived(1, true)
      .withHasDeparted(1, true)
      .withTripHeadsign(new NonLocalizedString("Headsign"))
      .withStopHeadsign(1, new NonLocalizedString("Stop headsign"))
      .withOccupancyStatus(1, OccupancyStatus.MANY_SEATS_AVAILABLE)
      .withWheelchairAccessibility(Accessibility.POSSIBLE)
      .withPartialReplacedBy(0, 1, tripOnServiceDate())
      .withVehicleId(id("vehicle"));
  }

  private static TripOnServiceDate tripOnServiceDate() {
    return TripOnServiceDate.of(id("replacement"))
      .withTrip(TransitRepositoryForTest.trip("replacementTrip").build())
      .withServiceDate(LocalDate.of(2026, 1, 1))
      .build();
  }
}
