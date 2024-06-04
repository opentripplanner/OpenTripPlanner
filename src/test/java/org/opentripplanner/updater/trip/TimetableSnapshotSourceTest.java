package org.opentripplanner.updater.trip;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.CANCELED;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.DELETED;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.updater.trip.BackwardsDelayPropagationType.REQUIRED_NO_DATA;
import static org.opentripplanner.updater.trip.TimetableSnapshotSourceTest.SameAssert.NotSame;
import static org.opentripplanner.updater.trip.TimetableSnapshotSourceTest.SameAssert.Same;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import de.mfdz.MfdzRealtimeExtensions.StopTimePropertiesExtension.DropOffPickupType;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;
import org.opentripplanner.updater.spi.UpdateSuccess.WarningType;

public class TimetableSnapshotSourceTest {

  private static final LocalDate SERVICE_DATE = LocalDate.parse("2009-02-01");
  private static final TripUpdate CANCELLATION = new TripUpdateBuilder(
    "1.1",
    SERVICE_DATE,
    CANCELED,
    ZoneIds.NEW_YORK
  )
    .build();
  private TransitModel transitModel;

  private final GtfsRealtimeFuzzyTripMatcher TRIP_MATCHER_NOOP = null;

  private final boolean FULL_DATASET = false;
  private String feedId;

  @BeforeEach
  public void setUp() {
    TestOtpModel model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.SIMPLE_GTFS);
    transitModel = model.transitModel();

    feedId = transitModel.getFeedIds().stream().findFirst().get();
  }

  @Test
  public void testGetSnapshot() {
    var updater = defaultUpdater();

    updater.applyTripUpdates(
      TRIP_MATCHER_NOOP,
      REQUIRED_NO_DATA,
      FULL_DATASET,
      List.of(CANCELLATION),
      feedId
    );

    final TimetableSnapshot snapshot = updater.getTimetableSnapshot();
    assertNotNull(snapshot);
    assertSame(snapshot, updater.getTimetableSnapshot());
  }

  @Test
  public void testGetSnapshotWithMaxSnapshotFrequencyCleared() {
    var updater = new TimetableSnapshotSource(
      TimetableSnapshotSourceParameters.DEFAULT.withMaxSnapshotFrequency(Duration.ofMillis(-1)),
      transitModel
    );

    final TimetableSnapshot snapshot = updater.getTimetableSnapshot();

    updater.applyTripUpdates(
      TRIP_MATCHER_NOOP,
      REQUIRED_NO_DATA,
      FULL_DATASET,
      List.of(CANCELLATION),
      feedId
    );

    final TimetableSnapshot newSnapshot = updater.getTimetableSnapshot();
    assertNotNull(newSnapshot);
    assertNotSame(snapshot, newSnapshot);
  }

  /**
   * This test just asserts that invalid trip ids don't throw an exception and are ignored instead
   */
  @Test
  public void invalidTripId() {
    var updater = new TimetableSnapshotSource(
      TimetableSnapshotSourceParameters.DEFAULT,
      transitModel
    );

    Stream
      .of("", null)
      .forEach(id -> {
        var tripDescriptorBuilder = TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("");
        tripDescriptorBuilder.setScheduleRelationship(
          TripDescriptor.ScheduleRelationship.SCHEDULED
        );
        var tripUpdateBuilder = TripUpdate.newBuilder();

        tripUpdateBuilder.setTrip(tripDescriptorBuilder);
        var tripUpdate = tripUpdateBuilder.build();

        var result = updater.applyTripUpdates(
          TRIP_MATCHER_NOOP,
          REQUIRED_NO_DATA,
          FULL_DATASET,
          List.of(tripUpdate),
          feedId
        );

        assertEquals(0, result.successful());
      });
  }

  @Test
  public void testHandleModifiedTrip() {
    // GIVEN

    String modifiedTripId = "10.1";

    TripUpdate tripUpdate;
    {
      final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

      tripDescriptorBuilder.setTripId(modifiedTripId);
      tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.REPLACEMENT);
      tripDescriptorBuilder.setStartDate(ServiceDateUtils.asCompactString(SERVICE_DATE));

      final long midnightSecondsSinceEpoch = ServiceDateUtils
        .asStartOfService(SERVICE_DATE, transitModel.getTimeZone())
        .toEpochSecond();

      final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

      tripUpdateBuilder.setTrip(tripDescriptorBuilder);

      { // Stop O
        final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder();
        stopTimeUpdateBuilder.setScheduleRelationship(
          StopTimeUpdate.ScheduleRelationship.SCHEDULED
        );
        stopTimeUpdateBuilder.setStopId("O");
        stopTimeUpdateBuilder.setStopSequence(10);

        { // Arrival
          final StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
          arrivalBuilder.setTime(midnightSecondsSinceEpoch + (12 * 3600) + (30 * 60));
          arrivalBuilder.setDelay(0);
        }

        { // Departure
          final StopTimeEvent.Builder departureBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
          departureBuilder.setTime(midnightSecondsSinceEpoch + (12 * 3600) + (30 * 60));
          departureBuilder.setDelay(0);
        }
      }

      { // Stop C
        final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder();
        stopTimeUpdateBuilder.setScheduleRelationship(
          StopTimeUpdate.ScheduleRelationship.SCHEDULED
        );
        stopTimeUpdateBuilder.setStopId("C");
        stopTimeUpdateBuilder.setStopSequence(30);

        { // Arrival
          final StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
          arrivalBuilder.setTime(midnightSecondsSinceEpoch + (12 * 3600) + (40 * 60));
          arrivalBuilder.setDelay(0);
        }

        { // Departure
          final StopTimeEvent.Builder departureBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
          departureBuilder.setTime(midnightSecondsSinceEpoch + (12 * 3600) + (45 * 60));
          departureBuilder.setDelay(0);
        }
      }

      { // Stop D
        final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder();
        stopTimeUpdateBuilder.setScheduleRelationship(SKIPPED);
        stopTimeUpdateBuilder.setStopId("D");
        stopTimeUpdateBuilder.setStopSequence(40);

        { // Arrival
          final StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
          arrivalBuilder.setTime(midnightSecondsSinceEpoch + (12 * 3600) + (50 * 60));
          arrivalBuilder.setDelay(0);
        }

        { // Departure
          final StopTimeEvent.Builder departureBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
          departureBuilder.setTime(midnightSecondsSinceEpoch + (12 * 3600) + (51 * 60));
          departureBuilder.setDelay(0);
        }
      }

      { // Stop P
        final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder();
        stopTimeUpdateBuilder.setScheduleRelationship(
          StopTimeUpdate.ScheduleRelationship.SCHEDULED
        );
        stopTimeUpdateBuilder.setStopId("P");
        stopTimeUpdateBuilder.setStopSequence(50);

        { // Arrival
          final StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
          arrivalBuilder.setTime(midnightSecondsSinceEpoch + (12 * 3600) + (55 * 60));
          arrivalBuilder.setDelay(0);
        }

        { // Departure
          final StopTimeEvent.Builder departureBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
          departureBuilder.setTime(midnightSecondsSinceEpoch + (12 * 3600) + (55 * 60));
          departureBuilder.setDelay(0);
        }
      }

      tripUpdate = tripUpdateBuilder.build();
    }

    var updater = defaultUpdater();

    // WHEN
    updater.applyTripUpdates(
      TRIP_MATCHER_NOOP,
      REQUIRED_NO_DATA,
      FULL_DATASET,
      List.of(tripUpdate),
      feedId
    );

    // THEN
    final TimetableSnapshot snapshot = updater.getTimetableSnapshot();

    // Original trip pattern
    {
      final FeedScopedId tripId = new FeedScopedId(feedId, modifiedTripId);
      final Trip trip = transitModel.getTransitModelIndex().getTripForId().get(tripId);
      final TripPattern originalTripPattern = transitModel
        .getTransitModelIndex()
        .getPatternForTrip()
        .get(trip);

      final Timetable originalTimetableForToday = snapshot.resolve(
        originalTripPattern,
        SERVICE_DATE
      );
      final Timetable originalTimetableScheduled = snapshot.resolve(originalTripPattern, null);

      assertNotSame(originalTimetableForToday, originalTimetableScheduled);

      final int originalTripIndexScheduled = originalTimetableScheduled.getTripIndex(
        modifiedTripId
      );
      assertTrue(
        originalTripIndexScheduled > -1,
        "Original trip should be found in scheduled time table"
      );
      final TripTimes originalTripTimesScheduled = originalTimetableScheduled.getTripTimes(
        originalTripIndexScheduled
      );
      assertFalse(
        originalTripTimesScheduled.isCanceledOrDeleted(),
        "Original trip times should not be canceled in scheduled time table"
      );
      assertEquals(RealTimeState.SCHEDULED, originalTripTimesScheduled.getRealTimeState());

      final int originalTripIndexForToday = originalTimetableForToday.getTripIndex(modifiedTripId);
      assertTrue(
        originalTripIndexForToday > -1,
        "Original trip should be found in time table for service date"
      );
      final TripTimes originalTripTimesForToday = originalTimetableForToday.getTripTimes(
        originalTripIndexForToday
      );
      assertTrue(
        originalTripTimesForToday.isDeleted(),
        "Original trip times should be deleted in time table for service date"
      );
      assertEquals(RealTimeState.DELETED, originalTripTimesForToday.getRealTimeState());
    }

    // New trip pattern
    {
      final TripPattern newTripPattern = snapshot.getRealtimeAddedTripPattern(
        new FeedScopedId(feedId, modifiedTripId),
        SERVICE_DATE
      );
      assertNotNull(newTripPattern, "New trip pattern should be found");

      final Timetable newTimetableForToday = snapshot.resolve(newTripPattern, SERVICE_DATE);
      final Timetable newTimetableScheduled = snapshot.resolve(newTripPattern, null);

      assertNotSame(newTimetableForToday, newTimetableScheduled);

      final int newTimetableForTodayModifiedTripIndex = newTimetableForToday.getTripIndex(
        modifiedTripId
      );
      assertTrue(
        newTimetableForTodayModifiedTripIndex > -1,
        "New trip should be found in time table for service date"
      );
      assertEquals(
        RealTimeState.MODIFIED,
        newTimetableForToday.getTripTimes(newTimetableForTodayModifiedTripIndex).getRealTimeState()
      );

      assertEquals(
        -1,
        newTimetableScheduled.getTripIndex(modifiedTripId),
        "New trip should not be found in scheduled time table"
      );
    }
  }

  @Nested
  class Scheduled {

    @Test
    public void scheduled() {
      // GIVEN

      String scheduledTripId = "1.1";

      var builder = new TripUpdateBuilder(
        scheduledTripId,
        SERVICE_DATE,
        SCHEDULED,
        transitModel.getTimeZone()
      )
        .addDelayedStopTime(1, 0)
        .addDelayedStopTime(2, 60, 80)
        .addDelayedStopTime(3, 90, 90);

      var tripUpdate = builder.build();

      var updater = defaultUpdater();

      // WHEN
      updater.applyTripUpdates(
        TRIP_MATCHER_NOOP,
        REQUIRED_NO_DATA,
        FULL_DATASET,
        List.of(tripUpdate),
        feedId
      );

      // THEN
      final TimetableSnapshot snapshot = updater.getTimetableSnapshot();

      final FeedScopedId tripId = new FeedScopedId(feedId, scheduledTripId);
      final Trip trip = transitModel.getTransitModelIndex().getTripForId().get(tripId);
      final TripPattern originalTripPattern = transitModel
        .getTransitModelIndex()
        .getPatternForTrip()
        .get(trip);

      final Timetable originalTimetableForToday = snapshot.resolve(
        originalTripPattern,
        SERVICE_DATE
      );
      final Timetable originalTimetableScheduled = snapshot.resolve(originalTripPattern, null);

      assertNotSame(originalTimetableForToday, originalTimetableScheduled);

      final int originalTripIndexScheduled = originalTimetableScheduled.getTripIndex(tripId);
      assertTrue(
        originalTripIndexScheduled > -1,
        "Original trip should be found in scheduled time table"
      );
      final TripTimes originalTripTimesScheduled = originalTimetableScheduled.getTripTimes(
        originalTripIndexScheduled
      );
      assertFalse(
        originalTripTimesScheduled.isCanceledOrDeleted(),
        "Original trip times should not be canceled in scheduled time table"
      );
      assertEquals(RealTimeState.SCHEDULED, originalTripTimesScheduled.getRealTimeState());

      final int originalTripIndexForToday = originalTimetableForToday.getTripIndex(tripId);
      assertTrue(
        originalTripIndexForToday > -1,
        "Original trip should be found in time table for service date"
      );
      final TripTimes originalTripTimesForToday = originalTimetableForToday.getTripTimes(
        originalTripIndexForToday
      );
      assertEquals(RealTimeState.UPDATED, originalTripTimesForToday.getRealTimeState());
      assertEquals(0, originalTripTimesForToday.getArrivalDelay(0));
      assertEquals(0, originalTripTimesForToday.getDepartureDelay(0));
      assertEquals(60, originalTripTimesForToday.getArrivalDelay(1));
      assertEquals(80, originalTripTimesForToday.getDepartureDelay(1));
      assertEquals(90, originalTripTimesForToday.getArrivalDelay(2));
      assertEquals(90, originalTripTimesForToday.getDepartureDelay(2));
    }

    @Test
    public void scheduledTripWithSkippedAndNoData() {
      // GIVEN

      String scheduledTripId = "1.1";

      var builder = new TripUpdateBuilder(
        scheduledTripId,
        SERVICE_DATE,
        SCHEDULED,
        transitModel.getTimeZone()
      )
        .addNoDataStop(1)
        .addSkippedStop(2)
        .addNoDataStop(3);

      var tripUpdate = builder.build();

      var updater = defaultUpdater();

      // WHEN
      updater.applyTripUpdates(
        TRIP_MATCHER_NOOP,
        REQUIRED_NO_DATA,
        FULL_DATASET,
        List.of(tripUpdate),
        feedId
      );

      // THEN
      final TimetableSnapshot snapshot = updater.getTimetableSnapshot();

      // Original trip pattern
      {
        final FeedScopedId tripId = new FeedScopedId(feedId, scheduledTripId);
        final Trip trip = transitModel.getTransitModelIndex().getTripForId().get(tripId);
        final TripPattern originalTripPattern = transitModel
          .getTransitModelIndex()
          .getPatternForTrip()
          .get(trip);

        final Timetable originalTimetableForToday = snapshot.resolve(
          originalTripPattern,
          SERVICE_DATE
        );
        final Timetable originalTimetableScheduled = snapshot.resolve(originalTripPattern, null);

        assertNotSame(originalTimetableForToday, originalTimetableScheduled);

        final int originalTripIndexScheduled = originalTimetableScheduled.getTripIndex(tripId);
        assertTrue(
          originalTripIndexScheduled > -1,
          "Original trip should be found in scheduled time table"
        );
        final TripTimes originalTripTimesScheduled = originalTimetableScheduled.getTripTimes(
          originalTripIndexScheduled
        );
        assertFalse(
          originalTripTimesScheduled.isCanceledOrDeleted(),
          "Original trip times should not be canceled in scheduled time table"
        );
        assertEquals(RealTimeState.SCHEDULED, originalTripTimesScheduled.getRealTimeState());

        final int originalTripIndexForToday = originalTimetableForToday.getTripIndex(tripId);
        assertTrue(
          originalTripIndexForToday > -1,
          "Original trip should be found in time table for service date"
        );
        final TripTimes originalTripTimesForToday = originalTimetableForToday.getTripTimes(
          originalTripIndexForToday
        );
        assertTrue(
          originalTripTimesForToday.isDeleted(),
          "Original trip times should be deleted in time table for service date"
        );
        // original trip should be deleted
        assertEquals(RealTimeState.DELETED, originalTripTimesForToday.getRealTimeState());
      }

      // New trip pattern
      {
        final TripPattern newTripPattern = snapshot.getRealtimeAddedTripPattern(
          new FeedScopedId(feedId, scheduledTripId),
          SERVICE_DATE
        );
        assertNotNull(newTripPattern, "New trip pattern should be found");

        final Timetable newTimetableForToday = snapshot.resolve(newTripPattern, SERVICE_DATE);
        final Timetable newTimetableScheduled = snapshot.resolve(newTripPattern, null);

        assertNotSame(newTimetableForToday, newTimetableScheduled);

        assertTrue(newTripPattern.canBoard(0));
        assertFalse(newTripPattern.canBoard(1));
        assertTrue(newTripPattern.canBoard(2));

        assertEquals(new NonLocalizedString("foo"), newTripPattern.getTripHeadsign());
        assertEquals(
          newTripPattern.getOriginalTripPattern().getTripHeadsign(),
          newTripPattern.getTripHeadsign()
        );

        final int newTimetableForTodayModifiedTripIndex = newTimetableForToday.getTripIndex(
          scheduledTripId
        );
        assertTrue(
          newTimetableForTodayModifiedTripIndex > -1,
          "New trip should be found in time table for service date"
        );

        var newTripTimes = newTimetableForToday.getTripTimes(newTimetableForTodayModifiedTripIndex);
        assertEquals(RealTimeState.UPDATED, newTripTimes.getRealTimeState());

        assertEquals(
          -1,
          newTimetableScheduled.getTripIndex(scheduledTripId),
          "New trip should not be found in scheduled time table"
        );

        assertEquals(0, newTripTimes.getArrivalDelay(0));
        assertEquals(0, newTripTimes.getDepartureDelay(0));
        assertEquals(0, newTripTimes.getArrivalDelay(1));
        assertEquals(0, newTripTimes.getDepartureDelay(1));
        assertEquals(0, newTripTimes.getArrivalDelay(2));
        assertEquals(0, newTripTimes.getDepartureDelay(2));
        assertTrue(newTripTimes.isNoDataStop(0));
        assertTrue(newTripTimes.isCancelledStop(1));
        assertTrue(newTripTimes.isNoDataStop(2));
      }
    }

    /**
     * Test realtime system behavior under one very particular case from issue #5725.
     * When applying differential realtime updates, an update may cancel some stops on a trip. A
     * later update may then revert the trip back to its originally scheduled sequence of stops.
     * When this happens, we expect the trip to be associated with a new trip pattern (where some
     * stops have no pickup or dropoff) then dissociated from that new pattern and re-associated
     * with its originally scheduled pattern. Any trip times that were created in timetables under
     * the new stop-skipping trip pattern should also be removed.
     */
    @Test
    public void scheduledTripWithPreviouslySkipped() {
      // Create update with a skipped stop at first
      String scheduledTripId = "1.1";

      var skippedBuilder = new TripUpdateBuilder(
        scheduledTripId,
        SERVICE_DATE,
        SCHEDULED,
        transitModel.getTimeZone()
      )
        .addDelayedStopTime(1, 0)
        .addSkippedStop(2)
        .addDelayedStopTime(3, 90);

      var tripUpdate = skippedBuilder.build();

      var updater = defaultUpdater();

      // apply the update with a skipped stop
      updater.applyTripUpdates(
        TRIP_MATCHER_NOOP,
        REQUIRED_NO_DATA,
        false,
        List.of(tripUpdate),
        feedId
      );

      // Force a snapshot commit. This is done to mimic normal behaviour where a new update arrives
      // after the original snapshot has been committed
      updater.commitTimetableSnapshot(true);

      // Create update to the same trip but now the skipped stop is no longer skipped
      var scheduledBuilder = new TripUpdateBuilder(
        scheduledTripId,
        SERVICE_DATE,
        SCHEDULED,
        transitModel.getTimeZone()
      )
        .addDelayedStopTime(1, 0)
        .addDelayedStopTime(2, 60, 80)
        .addDelayedStopTime(3, 90, 90);

      tripUpdate = scheduledBuilder.build();

      // apply the update with the previously skipped stop now scheduled
      updater.applyTripUpdates(
        TRIP_MATCHER_NOOP,
        REQUIRED_NO_DATA,
        false,
        List.of(tripUpdate),
        feedId
      );

      // Check that the there is no longer a realtime added trip pattern for the trip and that the
      // stoptime updates have gone through
      var snapshot = updater.getTimetableSnapshot();
      {
        final TripPattern newTripPattern = snapshot.getRealtimeAddedTripPattern(
          new FeedScopedId(feedId, scheduledTripId),
          SERVICE_DATE
        );
        assertNull(newTripPattern);

        final FeedScopedId tripId = new FeedScopedId(feedId, scheduledTripId);
        final Trip trip = transitModel.getTransitModelIndex().getTripForId().get(tripId);
        final TripPattern originalTripPattern = transitModel
          .getTransitModelIndex()
          .getPatternForTrip()
          .get(trip);

        final Timetable originalTimetableForToday = snapshot.resolve(
          originalTripPattern,
          SERVICE_DATE
        );
        final Timetable originalTimetableScheduled = snapshot.resolve(originalTripPattern, null);

        assertNotSame(originalTimetableForToday, originalTimetableScheduled);

        final int originalTripIndexScheduled = originalTimetableScheduled.getTripIndex(tripId);
        assertTrue(
          originalTripIndexScheduled > -1,
          "Original trip should be found in scheduled time table"
        );
        final TripTimes originalTripTimesScheduled = originalTimetableScheduled.getTripTimes(
          originalTripIndexScheduled
        );
        assertFalse(
          originalTripTimesScheduled.isCanceledOrDeleted(),
          "Original trip times should not be canceled in scheduled time table"
        );
        assertEquals(RealTimeState.SCHEDULED, originalTripTimesScheduled.getRealTimeState());

        final int originalTripIndexForToday = originalTimetableForToday.getTripIndex(tripId);
        assertTrue(
          originalTripIndexForToday > -1,
          "Original trip should be found in time table for service date"
        );
        final TripTimes originalTripTimesForToday = originalTimetableForToday.getTripTimes(
          originalTripIndexForToday
        );
        assertEquals(RealTimeState.UPDATED, originalTripTimesForToday.getRealTimeState());
        assertEquals(0, originalTripTimesForToday.getArrivalDelay(0));
        assertEquals(0, originalTripTimesForToday.getDepartureDelay(0));
        assertEquals(60, originalTripTimesForToday.getArrivalDelay(1));
        assertEquals(80, originalTripTimesForToday.getDepartureDelay(1));
        assertEquals(90, originalTripTimesForToday.getArrivalDelay(2));
        assertEquals(90, originalTripTimesForToday.getDepartureDelay(2));
      }
    }
  }

  @Nested
  class Added {

    final String addedTripId = "added_trip";

    @Test
    public void addedTrip() {
      var builder = new TripUpdateBuilder(
        addedTripId,
        SERVICE_DATE,
        ADDED,
        transitModel.getTimeZone()
      );

      builder.addStopTime("A", 30).addStopTime("C", 40).addStopTime("E", 55);

      var tripUpdate = builder.build();

      var updater = defaultUpdater();

      // WHEN
      updater.applyTripUpdates(
        TRIP_MATCHER_NOOP,
        REQUIRED_NO_DATA,
        FULL_DATASET,
        List.of(tripUpdate),
        feedId
      );

      // THEN
      assertAddedTrip(SERVICE_DATE, this.addedTripId, updater, false);
    }

    private TripPattern assertAddedTrip(
      LocalDate serviceDate,
      String tripId,
      TimetableSnapshotSource updater,
      boolean forceSnapshotCommit
    ) {
      var stopA = transitModel.getStopModel().getRegularStop(new FeedScopedId(feedId, "A"));
      // Get the trip pattern of the added trip which goes through stopA
      if (forceSnapshotCommit) {
        updater.commitTimetableSnapshot(true);
      }
      var snapshot = updater.getTimetableSnapshot();
      var patternsAtA = snapshot.getPatternsForStop(stopA);

      assertNotNull(patternsAtA, "Added trip pattern should be found");
      assertEquals(1, patternsAtA.size());
      var tripPattern = patternsAtA.stream().findFirst().get();

      final Timetable forToday = snapshot.resolve(tripPattern, serviceDate);
      final Timetable schedule = snapshot.resolve(tripPattern, null);

      assertNotSame(forToday, schedule);

      final int forTodayAddedTripIndex = forToday.getTripIndex(tripId);
      assertTrue(
        forTodayAddedTripIndex > -1,
        "Added trip should be found in time table for service date"
      );
      assertEquals(
        RealTimeState.ADDED,
        forToday.getTripTimes(forTodayAddedTripIndex).getRealTimeState()
      );

      final int scheduleTripIndex = schedule.getTripIndex(tripId);
      assertEquals(-1, scheduleTripIndex, "Added trip should not be found in scheduled time table");
      return tripPattern;
    }

    @Test
    public void addedTripWithNewRoute() {
      // GIVEN

      final var builder = new TripUpdateBuilder(
        addedTripId,
        SERVICE_DATE,
        ADDED,
        transitModel.getTimeZone()
      );
      // add extension to set route name, url, mode
      builder.addTripExtension();

      builder
        .addStopTime("A", 30, DropOffPickupType.PHONE_AGENCY)
        .addStopTime("C", 40, DropOffPickupType.COORDINATE_WITH_DRIVER)
        .addStopTime("E", 55, DropOffPickupType.NONE);

      var tripUpdate = builder.build();

      var updater = defaultUpdater();

      // WHEN
      var result = updater.applyTripUpdates(
        TRIP_MATCHER_NOOP,
        REQUIRED_NO_DATA,
        FULL_DATASET,
        List.of(tripUpdate),
        feedId
      );

      // THEN

      assertTrue(result.warnings().isEmpty());

      var pattern = assertAddedTrip(SERVICE_DATE, addedTripId, updater, false);

      var route = pattern.getRoute();
      assertEquals(TripUpdateBuilder.ROUTE_URL, route.getUrl());
      assertEquals(TripUpdateBuilder.ROUTE_NAME, route.getName());
      assertEquals(TransitMode.RAIL, route.getMode());

      var fromTransitModel = transitModel.getTransitModelIndex().getRouteForId(route.getId());
      assertEquals(fromTransitModel, route);

      assertEquals(PickDrop.CALL_AGENCY, pattern.getBoardType(0));
      assertEquals(PickDrop.CALL_AGENCY, pattern.getAlightType(0));

      assertEquals(PickDrop.COORDINATE_WITH_DRIVER, pattern.getBoardType(1));
      assertEquals(PickDrop.COORDINATE_WITH_DRIVER, pattern.getAlightType(1));
    }

    @Test
    public void addedWithUnknownStop() {
      // GIVEN
      final var builder = new TripUpdateBuilder(
        addedTripId,
        SERVICE_DATE,
        ADDED,
        transitModel.getTimeZone()
      );
      // add extension to set route name, url, mode
      builder.addTripExtension();

      builder
        .addStopTime("A", 30, DropOffPickupType.PHONE_AGENCY)
        .addStopTime("UNKNOWN_STOP_ID", 40, DropOffPickupType.COORDINATE_WITH_DRIVER)
        .addStopTime("E", 55, DropOffPickupType.NONE);

      var tripUpdate = builder.build();

      var updater = defaultUpdater();

      // WHEN
      var result = updater.applyTripUpdates(
        TRIP_MATCHER_NOOP,
        REQUIRED_NO_DATA,
        FULL_DATASET,
        List.of(tripUpdate),
        feedId
      );

      // THEN

      assertFalse(result.warnings().isEmpty());

      assertEquals(List.of(WarningType.UNKNOWN_STOPS_REMOVED_FROM_ADDED_TRIP), result.warnings());

      var pattern = assertAddedTrip(SERVICE_DATE, addedTripId, updater, false);

      assertEquals(2, pattern.getStops().size());
    }

    @Test
    public void repeatedlyAddedTripWithNewRoute() {
      // GIVEN

      final var builder = new TripUpdateBuilder(
        addedTripId,
        SERVICE_DATE,
        ADDED,
        transitModel.getTimeZone()
      );
      // add extension to set route name, url, mode
      builder.addTripExtension();

      builder
        .addStopTime("A", 30, DropOffPickupType.PHONE_AGENCY)
        .addStopTime("C", 40, DropOffPickupType.COORDINATE_WITH_DRIVER)
        .addStopTime("E", 55, DropOffPickupType.NONE);

      var tripUpdate = builder.build();

      var updater = defaultUpdater();

      // WHEN
      updater.applyTripUpdates(
        TRIP_MATCHER_NOOP,
        REQUIRED_NO_DATA,
        FULL_DATASET,
        List.of(tripUpdate),
        feedId
      );
      var pattern = assertAddedTrip(SERVICE_DATE, addedTripId, updater, false);
      var firstRoute = pattern.getRoute();

      // apply the update a second time to check that no new route instance is created but the old one is reused
      updater.applyTripUpdates(
        TRIP_MATCHER_NOOP,
        REQUIRED_NO_DATA,
        FULL_DATASET,
        List.of(tripUpdate),
        feedId
      );
      var secondPattern = assertAddedTrip(SERVICE_DATE, addedTripId, updater, false);
      var secondRoute = secondPattern.getRoute();

      // THEN

      assertSame(firstRoute, secondRoute);
      assertNotNull(transitModel.getTransitModelIndex().getRouteForId(firstRoute.getId()));
    }

    static List<Arguments> cancelingAddedTripTestCases() {
      return List.of(
        // TODO we might want to change the behaviour so that only the trip without pattern is
        // persisted if the added trip is cancelled
        Arguments.of(CANCELED, RealTimeState.CANCELED),
        Arguments.of(DELETED, RealTimeState.DELETED)
      );
    }

    /**
     * Test behavior of the realtime system in a case related to #5725 that is discussed at:
     * https://github.com/opentripplanner/OpenTripPlanner/pull/5726#discussion_r1521653840
     * When a trip is added by a realtime message, in the realtime data indexes a corresponding
     * trip pattern should be associated with the stops that trip visits. When a subsequent
     * realtime message cancels or deletes that trip, the pattern should continue to be present in
     * the realtime data indexes, and it should still contain the previously added trip, but that
     * trip should be marked as having canceled or deleted status. At no point should the trip
     * added by realtime data be present in the trip pattern for scheduled service.
     */
    @ParameterizedTest
    @MethodSource("cancelingAddedTripTestCases")
    public void cancelingAddedTrip(
      ScheduleRelationship scheduleRelationship,
      RealTimeState expectedState
    ) {
      var builder = new TripUpdateBuilder(
        addedTripId,
        SERVICE_DATE,
        ADDED,
        transitModel.getTimeZone()
      );

      builder.addStopTime("A", 30).addStopTime("C", 40).addStopTime("E", 55);

      var tripUpdate = builder.build();

      var updater = defaultUpdater();

      // WHEN
      updater.applyTripUpdates(
        TRIP_MATCHER_NOOP,
        REQUIRED_NO_DATA,
        FULL_DATASET,
        List.of(tripUpdate),
        feedId
      );

      // THEN
      assertAddedTrip(SERVICE_DATE, this.addedTripId, updater, true);

      var tripDescriptorBuilder = TripDescriptor.newBuilder();
      tripDescriptorBuilder.setTripId(addedTripId);
      tripDescriptorBuilder.setScheduleRelationship(scheduleRelationship);

      tripDescriptorBuilder.setStartDate(ServiceDateUtils.asCompactString(SERVICE_DATE));
      tripUpdate = TripUpdate.newBuilder().setTrip(tripDescriptorBuilder).build();

      // WHEN
      updater.applyTripUpdates(
        TRIP_MATCHER_NOOP,
        REQUIRED_NO_DATA,
        FULL_DATASET,
        List.of(tripUpdate),
        feedId
      );

      // THEN
      var snapshot = updater.getTimetableSnapshot();
      var stopA = transitModel.getStopModel().getRegularStop(new FeedScopedId(feedId, "A"));
      // Get the trip pattern of the added trip which goes through stopA
      var patternsAtA = snapshot.getPatternsForStop(stopA);

      assertNotNull(patternsAtA, "Added trip pattern should be found");
      var tripPattern = patternsAtA.stream().findFirst().get();

      final Timetable forToday = snapshot.resolve(tripPattern, SERVICE_DATE);
      final Timetable schedule = snapshot.resolve(tripPattern, null);

      assertNotSame(forToday, schedule);

      final int forTodayAddedTripIndex = forToday.getTripIndex(addedTripId);
      assertTrue(
        forTodayAddedTripIndex > -1,
        "Added trip should be found in time table for the service date"
      );
      assertEquals(expectedState, forToday.getTripTimes(forTodayAddedTripIndex).getRealTimeState());

      final int scheduleTripIndex = schedule.getTripIndex(addedTripId);
      assertEquals(-1, scheduleTripIndex, "Added trip should not be found in scheduled time table");
    }
  }

  @Nonnull
  private TimetableSnapshotSource defaultUpdater() {
    return new TimetableSnapshotSource(
      new TimetableSnapshotSourceParameters(Duration.ZERO, true),
      transitModel,
      () -> SERVICE_DATE
    );
  }

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
      // purgeExpiredData   maxSnapshotFrequency || snapshots PatternSnapshotA  PatternSnapshotB
      Arguments.of(Boolean.TRUE, -1, NotSame, NotSame),
      Arguments.of(Boolean.FALSE, -1, NotSame, Same),
      Arguments.of(Boolean.TRUE, 1000, NotSame, NotSame),
      Arguments.of(Boolean.FALSE, 1000, Same, Same)
    );
  }

  @ParameterizedTest(name = "purgeExpired: {0}, maxFrequency: {1}  ||  {2}  {3}")
  @MethodSource("purgeExpiredDataTestCases")
  public void testPurgeExpiredData(
    boolean purgeExpiredData,
    int maxSnapshotFrequency,
    SameAssert expSnapshots,
    SameAssert expPatternAeqB
  ) {
    final FeedScopedId tripId = new FeedScopedId(feedId, "1.1");
    final Trip trip = transitModel.getTransitModelIndex().getTripForId().get(tripId);
    final TripPattern pattern = transitModel.getTransitModelIndex().getPatternForTrip().get(trip);

    // We will simulate the clock turning midnight into tomorrow, data on
    // yesterday is candidate to expire
    final LocalDate yesterday = SERVICE_DATE.minusDays(1);
    final LocalDate tomorrow = SERVICE_DATE.plusDays(1);
    final AtomicReference<LocalDate> clock = new AtomicReference<>(yesterday);

    var tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.CANCELED);

    tripDescriptorBuilder.setStartDate(ServiceDateUtils.asCompactString(yesterday));
    var tripUpdateYesterday = TripUpdate.newBuilder().setTrip(tripDescriptorBuilder).build();

    // Update pattern on today, even if the time the update is performed is tomorrow
    tripDescriptorBuilder.setStartDate(ServiceDateUtils.asCompactString(SERVICE_DATE));
    var tripUpdateToday = TripUpdate.newBuilder().setTrip(tripDescriptorBuilder).build();

    var updater = new TimetableSnapshotSource(
      TimetableSnapshotSourceParameters.DEFAULT
        .withPurgeExpiredData(purgeExpiredData)
        .withMaxSnapshotFrequency(Duration.ofMillis(maxSnapshotFrequency)),
      transitModel,
      clock::get
    );

    // Apply update when clock is yesterday
    updater.applyTripUpdates(
      TRIP_MATCHER_NOOP,
      REQUIRED_NO_DATA,
      FULL_DATASET,
      List.of(tripUpdateYesterday),
      feedId
    );
    updater.commitTimetableSnapshot(true);

    final TimetableSnapshot snapshotA = updater.getTimetableSnapshot();

    // Turn the clock to tomorrow
    clock.set(tomorrow);

    updater.applyTripUpdates(
      TRIP_MATCHER_NOOP,
      REQUIRED_NO_DATA,
      FULL_DATASET,
      List.of(tripUpdateToday),
      feedId
    );
    final TimetableSnapshot snapshotB = updater.getTimetableSnapshot();

    expSnapshots.test(snapshotA, snapshotB);
    expPatternAeqB.test(
      snapshotA.resolve(pattern, yesterday),
      snapshotB.resolve(pattern, yesterday)
    );
    expPatternAeqB
      .not()
      .test(snapshotB.resolve(pattern, null), snapshotB.resolve(pattern, yesterday));

    // Expect the same results regardless of the config for these
    assertNotSame(snapshotA.resolve(pattern, null), snapshotA.resolve(pattern, yesterday));
    assertSame(snapshotA.resolve(pattern, null), snapshotB.resolve(pattern, null));
  }
}
