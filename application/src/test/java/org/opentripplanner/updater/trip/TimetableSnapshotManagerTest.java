package org.opentripplanner.updater.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.updater.trip.TimetableSnapshotManagerTest.SameAssert.NotSame;
import static org.opentripplanner.updater.trip.TimetableSnapshotManagerTest.SameAssert.Same;

import java.time.LocalDate;
import java.time.Month;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.TimetableSnapshot;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.updater.TimetableSnapshotParameters;

class TimetableSnapshotManagerTest {

  private static final LocalDate TODAY = LocalDate.of(2024, Month.MAY, 30);
  private static final LocalDate TOMORROW = TODAY.plusDays(1);
  private static final LocalDate YESTERDAY = TODAY.minusDays(1);

  private static final TimetableRepositoryForTest TEST_MODEL = TimetableRepositoryForTest.of();

  // --- Shared objects for the purge test ---

  private static final TripPattern PATTERN = TimetableRepositoryForTest.tripPattern(
    "pattern",
    TimetableRepositoryForTest.route("r1").build()
  )
    .withStopPattern(
      TimetableRepositoryForTest.stopPattern(
        TEST_MODEL.stop("1").build(),
        TEST_MODEL.stop("2").build()
      )
    )
    .build();
  private static final TripTimes TRIP_TIMES = ScheduledTripTimes.of()
    .withArrivalTimes("00:00 00:01")
    .withTrip(TimetableRepositoryForTest.trip("trip").build())
    .build();

  // --- Shared objects for the three-phase updateBuffer tests ---

  private static final Route ROUTE = TimetableRepositoryForTest.route("route1").build();
  private static final Trip TRIP = TimetableRepositoryForTest.trip("trip1").build();
  private static final RegularStop STOP_1 = TEST_MODEL.stop("S1").build();
  private static final RegularStop STOP_2 = TEST_MODEL.stop("S2").build();
  private static final RegularStop STOP_3 = TEST_MODEL.stop("S3").build();

  private static final ScheduledTripTimes SCHEDULED_TRIP_TIMES = ScheduledTripTimes.of()
    .withArrivalTimes("00:00 00:01")
    .withTrip(TRIP)
    .build();

  /**
   * Scheduled pattern (stops S1, S2) with the trip in its scheduled timetable. Phase 2 of
   * updateBuffer() reads from the scheduled timetable to create a DELETED entry.
   */
  private static final TripPattern SCHEDULED_PATTERN = TimetableRepositoryForTest.tripPattern(
    "sched",
    ROUTE
  )
    .withStopPattern(TimetableRepositoryForTest.stopPattern(STOP_1, STOP_2))
    .withScheduledTimeTableBuilder(ttb -> ttb.addTripTimes(SCHEDULED_TRIP_TIMES))
    .build();

  /**
   * Modified pattern (stops S1, S3) flagged as real-time stop-pattern-modified. When an update
   * targets this pattern, TimetableSnapshot registers it in realTimeNewTripPatternsForModifiedTrips.
   */
  private static final TripPattern MODIFIED_PATTERN = TimetableRepositoryForTest.tripPattern(
    "modified",
    ROUTE
  )
    .withStopPattern(TimetableRepositoryForTest.stopPattern(STOP_1, STOP_3))
    .withRealTimeStopPatternModified()
    .build();

  /** A second modified pattern (stops S2, S3) for the all-three-phases test. */
  private static final TripPattern SECOND_MODIFIED_PATTERN = TimetableRepositoryForTest.tripPattern(
    "modified2",
    ROUTE
  )
    .withStopPattern(TimetableRepositoryForTest.stopPattern(STOP_2, STOP_3))
    .withRealTimeStopPatternModified()
    .build();

  enum SameAssert {
    Same {
      public void test(Object a, Object b) {
        assertSame(a, b);
      }
    },
    NotSame {
      public void test(Object a, Object b) {
        assertNotSame(a, b);
      }
    };

    abstract void test(Object a, Object b);

    SameAssert not() {
      return this == Same ? NotSame : Same;
    }
  }

  static Stream<Arguments> purgeExpiredDataTestCases() {
    return Stream.of(
      // purgeExpiredData   || snapshots PatternSnapshotA  PatternSnapshotB
      Arguments.of(Boolean.TRUE, NotSame, NotSame),
      Arguments.of(Boolean.FALSE, NotSame, Same)
    );
  }

  @ParameterizedTest(name = "purgeExpired: {0} ||  {1}  {2}")
  @MethodSource("purgeExpiredDataTestCases")
  public void testPurgeExpiredData(
    boolean purgeExpiredData,
    SameAssert expSnapshots,
    SameAssert expPatternAeqB
  ) {
    // We will simulate the clock turning midnight into tomorrow, data on
    // yesterday is candidate to expire
    final AtomicReference<LocalDate> clock = new AtomicReference<>(YESTERDAY);

    var snapshotManager = new TimetableSnapshotManager(
      null,
      TimetableSnapshotParameters.DEFAULT.withPurgeExpiredData(purgeExpiredData),
      clock::get
    );

    var res1 = snapshotManager.updateBuffer(
      RealTimeTripUpdate.of(PATTERN, TRIP_TIMES, YESTERDAY).build()
    );
    assertTrue(res1.isSuccess());

    snapshotManager.commitTimetableSnapshot(true);
    final TimetableSnapshot snapshotA = snapshotManager.getTimetableSnapshot();

    // Turn the clock to tomorrow
    clock.set(TOMORROW);

    var res2 = snapshotManager.updateBuffer(
      RealTimeTripUpdate.of(PATTERN, TRIP_TIMES, TODAY).build()
    );
    assertTrue(res2.isSuccess());

    snapshotManager.purgeAndCommit();

    final TimetableSnapshot snapshotB = snapshotManager.getTimetableSnapshot();

    expSnapshots.test(snapshotA, snapshotB);
    expPatternAeqB.test(
      snapshotA.resolve(PATTERN, YESTERDAY),
      snapshotB.resolve(PATTERN, YESTERDAY)
    );
    expPatternAeqB
      .not()
      .test(snapshotB.resolve(PATTERN, null), snapshotB.resolve(PATTERN, YESTERDAY));

    // Expect the same results regardless of the config for these
    assertNotSame(snapshotA.resolve(PATTERN, null), snapshotA.resolve(PATTERN, YESTERDAY));
    assertSame(snapshotA.resolve(PATTERN, null), snapshotB.resolve(PATTERN, null));
  }

  /**
   * Phase 3 only: a simple update that applies trip times to the scheduled pattern without
   * reverting or deleting anything.
   */
  @Test
  void updateBufferPhase3Only() {
    var manager = createManager();
    var rtTripTimes = SCHEDULED_TRIP_TIMES.createRealTimeFromScheduledTimes()
      .withArrivalDelay(0, 60)
      .withDepartureDelay(0, 60)
      .withArrivalDelay(1, 60)
      .withDepartureDelay(1, 60)
      .build();

    var result = manager.updateBuffer(
      RealTimeTripUpdate.of(SCHEDULED_PATTERN, rtTripTimes, TODAY).build()
    );

    assertTrue(result.isSuccess());
    var timetable = manager.resolve(SCHEDULED_PATTERN, TODAY);
    var tripTimes = timetable.getTripTimes(TRIP);
    assertNotNull(tripTimes);
    assertEquals(RealTimeState.UPDATED, tripTimes.getRealTimeState());
  }

  /**
   * Phase 2 + Phase 3: move a trip to a modified pattern by deleting it from the scheduled pattern
   * and applying the update on the modified pattern.
   */
  @Test
  void updateBufferWithDeleteFromScheduled() {
    var manager = createManager();
    var rtTripTimes = SCHEDULED_TRIP_TIMES.createRealTimeFromScheduledTimes()
      .withArrivalDelay(0, 60)
      .withDepartureDelay(0, 60)
      .withArrivalDelay(1, 60)
      .withDepartureDelay(1, 60)
      .build();

    var result = manager.updateBuffer(
      RealTimeTripUpdate.of(MODIFIED_PATTERN, rtTripTimes, TODAY)
        .withHideTripInScheduledPattern(SCHEDULED_PATTERN)
        .build()
    );

    assertTrue(result.isSuccess());

    // Trip should be DELETED in the scheduled pattern
    var scheduledTimetable = manager.resolve(SCHEDULED_PATTERN, TODAY);
    assertEquals(RealTimeState.DELETED, scheduledTimetable.getTripTimes(TRIP).getRealTimeState());

    // Trip should be UPDATED in the modified pattern
    var modifiedTimetable = manager.resolve(MODIFIED_PATTERN, TODAY);
    assertEquals(RealTimeState.UPDATED, modifiedTimetable.getTripTimes(TRIP).getRealTimeState());

    // Modified pattern should be registered for this trip
    assertEquals(MODIFIED_PATTERN, manager.getNewTripPatternForModifiedTrip(TRIP.getId(), TODAY));
  }

  /**
   * Phase 1 + Phase 3: revert a previous pattern modification and update the trip on the
   * scheduled pattern.
   */
  @Test
  void updateBufferWithRevertThenUpdate() {
    var manager = createManager();
    var rtTripTimes = SCHEDULED_TRIP_TIMES.createRealTimeFromScheduledTimes()
      .withArrivalDelay(0, 60)
      .withDepartureDelay(0, 60)
      .withArrivalDelay(1, 60)
      .withDepartureDelay(1, 60)
      .build();

    // Setup: move the trip to the modified pattern
    manager.updateBuffer(
      RealTimeTripUpdate.of(MODIFIED_PATTERN, rtTripTimes, TODAY)
        .withHideTripInScheduledPattern(SCHEDULED_PATTERN)
        .build()
    );

    // Precondition: trip is on modified pattern
    assertEquals(MODIFIED_PATTERN, manager.getNewTripPatternForModifiedTrip(TRIP.getId(), TODAY));

    // Now revert and update on the scheduled pattern
    var result = manager.updateBuffer(
      RealTimeTripUpdate.of(SCHEDULED_PATTERN, rtTripTimes, TODAY)
        .withRevertPreviousRealTimeUpdates(true)
        .build()
    );

    assertTrue(result.isSuccess());

    // Trip should no longer be registered on the modified pattern
    assertNull(manager.getNewTripPatternForModifiedTrip(TRIP.getId(), TODAY));

    // Trip should be UPDATED in the scheduled pattern
    var timetable = manager.resolve(SCHEDULED_PATTERN, TODAY);
    assertEquals(RealTimeState.UPDATED, timetable.getTripTimes(TRIP).getRealTimeState());
  }

  /**
   * Phase 1 + Phase 2 + Phase 3: revert a previous pattern modification, delete the trip from the
   * scheduled pattern, and move it to a different modified pattern.
   */
  @Test
  void updateBufferAllThreePhases() {
    var manager = createManager();
    var rtTripTimes = SCHEDULED_TRIP_TIMES.createRealTimeFromScheduledTimes()
      .withArrivalDelay(0, 60)
      .withDepartureDelay(0, 60)
      .withArrivalDelay(1, 60)
      .withDepartureDelay(1, 60)
      .build();

    // Setup: move the trip to the first modified pattern
    manager.updateBuffer(
      RealTimeTripUpdate.of(MODIFIED_PATTERN, rtTripTimes, TODAY)
        .withHideTripInScheduledPattern(SCHEDULED_PATTERN)
        .build()
    );

    // Precondition: trip is on the first modified pattern
    assertEquals(MODIFIED_PATTERN, manager.getNewTripPatternForModifiedTrip(TRIP.getId(), TODAY));

    // Now revert from the first modified pattern, delete from scheduled, and update on the
    // second modified pattern
    var result = manager.updateBuffer(
      RealTimeTripUpdate.of(SECOND_MODIFIED_PATTERN, rtTripTimes, TODAY)
        .withRevertPreviousRealTimeUpdates(true)
        .withHideTripInScheduledPattern(SCHEDULED_PATTERN)
        .build()
    );

    assertTrue(result.isSuccess());

    // Trip should be DELETED in the scheduled pattern
    var scheduledTimetable = manager.resolve(SCHEDULED_PATTERN, TODAY);
    assertEquals(RealTimeState.DELETED, scheduledTimetable.getTripTimes(TRIP).getRealTimeState());

    // Trip should be UPDATED in the second modified pattern
    var secondModifiedTimetable = manager.resolve(SECOND_MODIFIED_PATTERN, TODAY);
    assertEquals(
      RealTimeState.UPDATED,
      secondModifiedTimetable.getTripTimes(TRIP).getRealTimeState()
    );

    // Modified pattern registration should point to the second modified pattern
    assertEquals(
      SECOND_MODIFIED_PATTERN,
      manager.getNewTripPatternForModifiedTrip(TRIP.getId(), TODAY)
    );
  }

  /**
   * Cancel a scheduled trip: build canceled trip times before calling updateBuffer, as the
   * adapter does.
   */
  @Test
  void updateBufferCancelScheduledTrip() {
    var manager = createManager();
    var canceledTripTimes = SCHEDULED_TRIP_TIMES.createRealTimeFromScheduledTimes()
      .cancelTrip()
      .build();

    var result = manager.updateBuffer(
      RealTimeTripUpdate.of(SCHEDULED_PATTERN, canceledTripTimes, TODAY)
        .withRevertPreviousRealTimeUpdates(true)
        .build()
    );

    assertTrue(result.isSuccess());
    var timetable = manager.resolve(SCHEDULED_PATTERN, TODAY);
    var tripTimes = timetable.getTripTimes(TRIP);
    assertNotNull(tripTimes);
    assertEquals(RealTimeState.CANCELED, tripTimes.getRealTimeState());
  }

  /**
   * Delete a scheduled trip: build deleted trip times before calling updateBuffer, as the
   * adapter does.
   */
  @Test
  void updateBufferDeleteScheduledTrip() {
    var manager = createManager();
    var deletedTripTimes = SCHEDULED_TRIP_TIMES.createRealTimeFromScheduledTimes()
      .deleteTrip()
      .build();

    var result = manager.updateBuffer(
      RealTimeTripUpdate.of(SCHEDULED_PATTERN, deletedTripTimes, TODAY)
        .withRevertPreviousRealTimeUpdates(true)
        .build()
    );

    assertTrue(result.isSuccess());
    var timetable = manager.resolve(SCHEDULED_PATTERN, TODAY);
    var tripTimes = timetable.getTripTimes(TRIP);
    assertNotNull(tripTimes);
    assertEquals(RealTimeState.DELETED, tripTimes.getRealTimeState());
  }

  /**
   * Set up a trip on a modified pattern, then cancel it with revert. Verify the trip is reverted
   * back to the scheduled pattern and is canceled there.
   */
  @Test
  void updateBufferCancelWithRevert() {
    var manager = createManager();
    var rtTripTimes = SCHEDULED_TRIP_TIMES.createRealTimeFromScheduledTimes()
      .withArrivalDelay(0, 60)
      .withDepartureDelay(0, 60)
      .withArrivalDelay(1, 60)
      .withDepartureDelay(1, 60)
      .build();

    // Setup: move the trip to the modified pattern
    manager.updateBuffer(
      RealTimeTripUpdate.of(MODIFIED_PATTERN, rtTripTimes, TODAY)
        .withHideTripInScheduledPattern(SCHEDULED_PATTERN)
        .build()
    );

    // Precondition: trip is on modified pattern
    assertEquals(MODIFIED_PATTERN, manager.getNewTripPatternForModifiedTrip(TRIP.getId(), TODAY));

    // Now revert and cancel on the scheduled pattern
    var canceledTripTimes = SCHEDULED_TRIP_TIMES.createRealTimeFromScheduledTimes()
      .cancelTrip()
      .build();
    var result = manager.updateBuffer(
      RealTimeTripUpdate.of(SCHEDULED_PATTERN, canceledTripTimes, TODAY)
        .withRevertPreviousRealTimeUpdates(true)
        .build()
    );

    assertTrue(result.isSuccess());

    // Trip should no longer be registered on the modified pattern
    assertNull(manager.getNewTripPatternForModifiedTrip(TRIP.getId(), TODAY));

    // Trip should be CANCELED in the scheduled pattern
    var timetable = manager.resolve(SCHEDULED_PATTERN, TODAY);
    assertEquals(RealTimeState.CANCELED, timetable.getTripTimes(TRIP).getRealTimeState());
  }

  private static TimetableSnapshotManager createManager() {
    return new TimetableSnapshotManager(null, TimetableSnapshotParameters.DEFAULT, () -> TODAY);
  }
}
