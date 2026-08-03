package org.opentripplanner.updater.trip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Test;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.RaptorTransitDataTestFactory;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.calendar.DefaultTripCalendars;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.transit.model.timetable.ScheduledTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.repository.DefaultTimetableRepository;

/**
 * Tests for {@link TripUpdateApplier}, covering the three-phase update logic that was
 * previously in {@code TimetableSnapshotManager.updateBuffer()}.
 */
class TripUpdateApplierTest {

  private static final LocalDate TODAY = LocalDate.of(2024, Month.MAY, 30);

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();

  private static final Route ROUTE = TransitRepositoryForTest.route("route1").build();
  private static final Trip TRIP = TransitRepositoryForTest.trip("trip1").build();
  private static final RegularStop STOP_1 = TEST_MODEL.stop("S1").build();
  private static final RegularStop STOP_2 = TEST_MODEL.stop("S2").build();
  private static final RegularStop STOP_3 = TEST_MODEL.stop("S3").build();

  private static final ScheduledTripTimes SCHEDULED_TRIP_TIMES = ScheduledTripTimes.of()
    .withArrivalTimes("00:00 00:01")
    .withTrip(TRIP)
    .build();

  /**
   * Scheduled pattern (stops S1, S2) with the trip in its scheduled timetable. Phase 2 of
   * apply() reads from the scheduled timetable to create a DELETED entry.
   */
  private static final TripPattern SCHEDULED_PATTERN = TransitRepositoryForTest.tripPattern(
    "sched",
    ROUTE
  )
    .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_1, STOP_2))
    .withScheduledTimeTableBuilder(ttb -> ttb.addTripTimes(SCHEDULED_TRIP_TIMES))
    .build();

  /**
   * Modified pattern (stops S1, S3) flagged as real-time stop-pattern-modified.
   */
  private static final TripPattern MODIFIED_PATTERN = TransitRepositoryForTest.tripPattern(
    "modified",
    ROUTE
  )
    .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_1, STOP_3))
    .withRealTimeStopPatternModified()
    .build();

  /** A second modified pattern (stops S2, S3) for the all-three-phases test. */
  private static final TripPattern SECOND_MODIFIED_PATTERN = TransitRepositoryForTest.tripPattern(
    "modified2",
    ROUTE
  )
    .withStopPattern(TransitRepositoryForTest.stopPattern(STOP_2, STOP_3))
    .withRealTimeStopPatternModified()
    .build();

  private static DefaultTimetableRepository createBuffer() {
    return new DefaultTimetableRepository(
      RaptorTransitDataTestFactory.empty(),
      new DefaultTripCalendars()
    );
  }

  /**
   * Phase 3 only: a simple update that applies trip times to the scheduled pattern without
   * reverting or deleting anything.
   */
  @Test
  void phase3Only() {
    var buffer = createBuffer();
    var rtTripTimes = SCHEDULED_TRIP_TIMES.createRealTimeFromScheduledTimes()
      .withArrivalDelay(0, 60)
      .withDepartureDelay(0, 60)
      .withArrivalDelay(1, 60)
      .withDepartureDelay(1, 60)
      .build();

    TripUpdateApplier.apply(
      buffer,
      RealTimeTripUpdate.of(SCHEDULED_PATTERN, rtTripTimes, TODAY).build()
    );

    var timetable = buffer.resolve(SCHEDULED_PATTERN, TODAY);
    var tripTimes = timetable.getTripTimes(TRIP);
    assertNotNull(tripTimes);
    assertTrue(tripTimes.hasAnyUpdates());
  }

  /**
   * Phase 2 + Phase 3: move a trip to a modified pattern by deleting it from the scheduled pattern
   * and applying the update on the modified pattern.
   */
  @Test
  void phase2And3() {
    var buffer = createBuffer();
    var rtTripTimes = SCHEDULED_TRIP_TIMES.createRealTimeFromScheduledTimes()
      .withArrivalDelay(0, 60)
      .withDepartureDelay(0, 60)
      .withArrivalDelay(1, 60)
      .withDepartureDelay(1, 60)
      .build();

    TripUpdateApplier.apply(
      buffer,
      RealTimeTripUpdate.of(MODIFIED_PATTERN, rtTripTimes, TODAY)
        .withHideTripInScheduledPattern(SCHEDULED_PATTERN)
        .build()
    );

    // Trip should be DELETED in the scheduled pattern
    var scheduledTimetable = buffer.resolve(SCHEDULED_PATTERN, TODAY);
    assertTrue(scheduledTimetable.getTripTimes(TRIP).isDeleted());

    // Trip should be UPDATED in the modified pattern
    var modifiedTimetable = buffer.resolve(MODIFIED_PATTERN, TODAY);
    assertTrue(modifiedTimetable.getTripTimes(TRIP).hasAnyUpdates());

    // Modified pattern should be registered for this trip
    assertEquals(MODIFIED_PATTERN, buffer.getNewTripPatternForModifiedTrip(TRIP.getId(), TODAY));
  }

  /**
   * Phase 1 + Phase 3: revert a previous pattern modification and update the trip on the
   * scheduled pattern.
   */
  @Test
  void phase1And3() {
    var buffer = createBuffer();
    var rtTripTimes = SCHEDULED_TRIP_TIMES.createRealTimeFromScheduledTimes()
      .withArrivalDelay(0, 60)
      .withDepartureDelay(0, 60)
      .withArrivalDelay(1, 60)
      .withDepartureDelay(1, 60)
      .build();

    // Setup: move the trip to the modified pattern
    TripUpdateApplier.apply(
      buffer,
      RealTimeTripUpdate.of(MODIFIED_PATTERN, rtTripTimes, TODAY)
        .withHideTripInScheduledPattern(SCHEDULED_PATTERN)
        .build()
    );

    // Precondition: trip is on modified pattern
    assertEquals(MODIFIED_PATTERN, buffer.getNewTripPatternForModifiedTrip(TRIP.getId(), TODAY));

    // Now revert and update on the scheduled pattern
    TripUpdateApplier.apply(
      buffer,
      RealTimeTripUpdate.of(SCHEDULED_PATTERN, rtTripTimes, TODAY)
        .withRevertPreviousRealTimeUpdates(true)
        .build()
    );

    // Trip should no longer be registered on the modified pattern
    assertNull(buffer.getNewTripPatternForModifiedTrip(TRIP.getId(), TODAY));

    // Trip should be UPDATED in the scheduled pattern
    var timetable = buffer.resolve(SCHEDULED_PATTERN, TODAY);
    assertTrue(timetable.getTripTimes(TRIP).hasAnyUpdates());
  }

  /**
   * Phase 1 + Phase 2 + Phase 3: revert a previous pattern modification, delete the trip from the
   * scheduled pattern, and move it to a different modified pattern.
   */
  @Test
  void allThreePhases() {
    var buffer = createBuffer();
    var rtTripTimes = SCHEDULED_TRIP_TIMES.createRealTimeFromScheduledTimes()
      .withArrivalDelay(0, 60)
      .withDepartureDelay(0, 60)
      .withArrivalDelay(1, 60)
      .withDepartureDelay(1, 60)
      .build();

    // Setup: move the trip to the first modified pattern
    TripUpdateApplier.apply(
      buffer,
      RealTimeTripUpdate.of(MODIFIED_PATTERN, rtTripTimes, TODAY)
        .withHideTripInScheduledPattern(SCHEDULED_PATTERN)
        .build()
    );

    // Precondition: trip is on the first modified pattern
    assertEquals(MODIFIED_PATTERN, buffer.getNewTripPatternForModifiedTrip(TRIP.getId(), TODAY));

    // Now revert from the first modified pattern, delete from scheduled, and update on the
    // second modified pattern
    TripUpdateApplier.apply(
      buffer,
      RealTimeTripUpdate.of(SECOND_MODIFIED_PATTERN, rtTripTimes, TODAY)
        .withRevertPreviousRealTimeUpdates(true)
        .withHideTripInScheduledPattern(SCHEDULED_PATTERN)
        .build()
    );

    // Trip should be DELETED in the scheduled pattern
    var scheduledTimetable = buffer.resolve(SCHEDULED_PATTERN, TODAY);
    assertTrue(scheduledTimetable.getTripTimes(TRIP).isDeleted());

    // Trip should be UPDATED in the second modified pattern
    var secondModifiedTimetable = buffer.resolve(SECOND_MODIFIED_PATTERN, TODAY);
    assertTrue(secondModifiedTimetable.getTripTimes(TRIP).hasAnyUpdates());

    // Modified pattern registration should point to the second modified pattern
    assertEquals(
      SECOND_MODIFIED_PATTERN,
      buffer.getNewTripPatternForModifiedTrip(TRIP.getId(), TODAY)
    );
  }

  /**
   * Cancel a scheduled trip: build canceled trip times before calling apply, as the
   * adapter does.
   */
  @Test
  void cancelScheduledTrip() {
    var buffer = createBuffer();
    var canceledTripTimes = SCHEDULED_TRIP_TIMES.createRealTimeFromScheduledTimes()
      .withCanceled()
      .build();

    TripUpdateApplier.apply(
      buffer,
      RealTimeTripUpdate.of(SCHEDULED_PATTERN, canceledTripTimes, TODAY)
        .withRevertPreviousRealTimeUpdates(true)
        .build()
    );

    var timetable = buffer.resolve(SCHEDULED_PATTERN, TODAY);
    var tripTimes = timetable.getTripTimes(TRIP);
    assertNotNull(tripTimes);
    assertTrue(tripTimes.isCanceled());
  }

  /**
   * Delete a scheduled trip: build deleted trip times before calling apply, as the
   * adapter does.
   */
  @Test
  void deleteScheduledTrip() {
    var buffer = createBuffer();
    var deletedTripTimes = SCHEDULED_TRIP_TIMES.createRealTimeFromScheduledTimes()
      .withDeleted()
      .build();

    TripUpdateApplier.apply(
      buffer,
      RealTimeTripUpdate.of(SCHEDULED_PATTERN, deletedTripTimes, TODAY)
        .withRevertPreviousRealTimeUpdates(true)
        .build()
    );

    var timetable = buffer.resolve(SCHEDULED_PATTERN, TODAY);
    var tripTimes = timetable.getTripTimes(TRIP);
    assertNotNull(tripTimes);
    assertTrue(tripTimes.isDeleted());
  }

  /**
   * Set up a trip on a modified pattern, then cancel it with revert. Verify the trip is reverted
   * back to the scheduled pattern and is canceled there.
   */
  @Test
  void cancelWithRevert() {
    var buffer = createBuffer();
    var rtTripTimes = SCHEDULED_TRIP_TIMES.createRealTimeFromScheduledTimes()
      .withArrivalDelay(0, 60)
      .withDepartureDelay(0, 60)
      .withArrivalDelay(1, 60)
      .withDepartureDelay(1, 60)
      .build();

    // Setup: move the trip to the modified pattern
    TripUpdateApplier.apply(
      buffer,
      RealTimeTripUpdate.of(MODIFIED_PATTERN, rtTripTimes, TODAY)
        .withHideTripInScheduledPattern(SCHEDULED_PATTERN)
        .build()
    );

    // Precondition: trip is on modified pattern
    assertEquals(MODIFIED_PATTERN, buffer.getNewTripPatternForModifiedTrip(TRIP.getId(), TODAY));

    // Now revert and cancel on the scheduled pattern
    var canceledTripTimes = SCHEDULED_TRIP_TIMES.createRealTimeFromScheduledTimes()
      .withCanceled()
      .build();
    TripUpdateApplier.apply(
      buffer,
      RealTimeTripUpdate.of(SCHEDULED_PATTERN, canceledTripTimes, TODAY)
        .withRevertPreviousRealTimeUpdates(true)
        .build()
    );

    // Trip should no longer be registered on the modified pattern
    assertNull(buffer.getNewTripPatternForModifiedTrip(TRIP.getId(), TODAY));

    // Trip should be CANCELED in the scheduled pattern
    var timetable = buffer.resolve(SCHEDULED_PATTERN, TODAY);
    assertTrue(timetable.getTripTimes(TRIP).isCanceled());
  }
}
