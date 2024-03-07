package org.opentripplanner.updater.trip;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate.ScheduleRelationship.SKIPPED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_SERVICE_ON_DATE;
import static org.opentripplanner.updater.trip.BackwardsDelayPropagationType.REQUIRED_NO_DATA;
import static org.opentripplanner.updater.trip.TimetableSnapshotSourceTest.SameAssert.NotSame;
import static org.opentripplanner.updater.trip.TimetableSnapshotSourceTest.SameAssert.Same;

import com.google.protobuf.InvalidProtocolBufferException;
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
  private TransitModel transitModel;

  private final GtfsRealtimeFuzzyTripMatcher TRIP_MATCHER_NOOP = null;

  private final boolean fullDataset = false;
  private byte[] cancellation;
  private String feedId;

  @BeforeEach
  public void setUp() {
    TestOtpModel model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.SIMPLE_GTFS);
    transitModel = model.transitModel();

    feedId = transitModel.getFeedIds().stream().findFirst().get();

    final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.CANCELED);

    final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

    tripUpdateBuilder.setTrip(tripDescriptorBuilder);

    cancellation = tripUpdateBuilder.build().toByteArray();
  }

  @Test
  public void testGetSnapshot() throws InvalidProtocolBufferException {
    var updater = defaultUpdater();

    updater.applyTripUpdates(
      TRIP_MATCHER_NOOP,
      REQUIRED_NO_DATA,
      fullDataset,
      List.of(TripUpdate.parseFrom(cancellation)),
      feedId
    );

    final TimetableSnapshot snapshot = updater.getTimetableSnapshot();
    assertNotNull(snapshot);
    assertSame(snapshot, updater.getTimetableSnapshot());

    updater.applyTripUpdates(
      TRIP_MATCHER_NOOP,
      REQUIRED_NO_DATA,
      fullDataset,
      List.of(TripUpdate.parseFrom(cancellation)),
      feedId
    );
    assertSame(snapshot, updater.getTimetableSnapshot());
  }

  @Test
  public void testGetSnapshotWithMaxSnapshotFrequencyCleared()
    throws InvalidProtocolBufferException {
    var updater = new TimetableSnapshotSource(
      TimetableSnapshotSourceParameters.DEFAULT.withMaxSnapshotFrequency(Duration.ofMillis(-1)),
      transitModel
    );

    final TimetableSnapshot snapshot = updater.getTimetableSnapshot();

    updater.applyTripUpdates(
      TRIP_MATCHER_NOOP,
      REQUIRED_NO_DATA,
      fullDataset,
      List.of(TripUpdate.parseFrom(cancellation)),
      feedId
    );

    final TimetableSnapshot newSnapshot = updater.getTimetableSnapshot();
    assertNotNull(newSnapshot);
    assertNotSame(snapshot, newSnapshot);
  }

  @Test
  public void testHandleCanceledTrip() throws InvalidProtocolBufferException {
    final FeedScopedId tripId = new FeedScopedId(feedId, "1.1");
    final FeedScopedId tripId2 = new FeedScopedId(feedId, "1.2");
    final Trip trip = transitModel.getTransitModelIndex().getTripForId().get(tripId);
    final TripPattern pattern = transitModel.getTransitModelIndex().getPatternForTrip().get(trip);
    final int tripIndex = pattern.getScheduledTimetable().getTripIndex(tripId);
    final int tripIndex2 = pattern.getScheduledTimetable().getTripIndex(tripId2);

    var updater = new TimetableSnapshotSource(
      TimetableSnapshotSourceParameters.DEFAULT,
      transitModel,
      () -> SERVICE_DATE
    );

    updater.applyTripUpdates(
      TRIP_MATCHER_NOOP,
      REQUIRED_NO_DATA,
      fullDataset,
      List.of(TripUpdate.parseFrom(cancellation)),
      feedId
    );

    final TimetableSnapshot snapshot = updater.getTimetableSnapshot();
    final Timetable forToday = snapshot.resolve(pattern, SERVICE_DATE);
    final Timetable schedule = snapshot.resolve(pattern, null);
    assertNotSame(forToday, schedule);
    assertNotSame(forToday.getTripTimes(tripIndex), schedule.getTripTimes(tripIndex));
    assertSame(forToday.getTripTimes(tripIndex2), schedule.getTripTimes(tripIndex2));

    final TripTimes tripTimes = forToday.getTripTimes(tripIndex);

    assertEquals(RealTimeState.CANCELED, tripTimes.getRealTimeState());
  }

  @Test
  public void testHandleDeletedTrip() throws InvalidProtocolBufferException {
    final FeedScopedId tripId = new FeedScopedId(feedId, "1.1");
    final FeedScopedId tripId2 = new FeedScopedId(feedId, "1.2");
    final Trip trip = transitModel.getTransitModelIndex().getTripForId().get(tripId);
    final TripPattern pattern = transitModel.getTransitModelIndex().getPatternForTrip().get(trip);
    final int tripIndex = pattern.getScheduledTimetable().getTripIndex(tripId);
    final int tripIndex2 = pattern.getScheduledTimetable().getTripIndex(tripId2);

    var updater = defaultUpdater();

    final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.DELETED);

    final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

    tripUpdateBuilder.setTrip(tripDescriptorBuilder);

    var deletion = tripUpdateBuilder.build().toByteArray();

    updater.applyTripUpdates(
      TRIP_MATCHER_NOOP,
      REQUIRED_NO_DATA,
      fullDataset,
      List.of(TripUpdate.parseFrom(deletion)),
      feedId
    );

    final TimetableSnapshot snapshot = updater.getTimetableSnapshot();
    final Timetable forToday = snapshot.resolve(pattern, SERVICE_DATE);
    final Timetable schedule = snapshot.resolve(pattern, null);
    assertNotSame(forToday, schedule);
    assertNotSame(forToday.getTripTimes(tripIndex), schedule.getTripTimes(tripIndex));
    assertSame(forToday.getTripTimes(tripIndex2), schedule.getTripTimes(tripIndex2));

    final TripTimes tripTimes = forToday.getTripTimes(tripIndex);

    assertEquals(RealTimeState.DELETED, tripTimes.getRealTimeState());
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

        updater.applyTripUpdates(
          TRIP_MATCHER_NOOP,
          REQUIRED_NO_DATA,
          fullDataset,
          List.of(tripUpdate),
          feedId
        );

        var snapshot = updater.getTimetableSnapshot();
        assertNull(snapshot);
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
      fullDataset,
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
    public void delayed() {
      final FeedScopedId tripId = new FeedScopedId(feedId, "1.1");
      final FeedScopedId tripId2 = new FeedScopedId(feedId, "1.2");
      final Trip trip = transitModel.getTransitModelIndex().getTripForId().get(tripId);
      final TripPattern pattern = transitModel.getTransitModelIndex().getPatternForTrip().get(trip);
      final int tripIndex = pattern.getScheduledTimetable().getTripIndex(tripId);
      final int tripIndex2 = pattern.getScheduledTimetable().getTripIndex(tripId2);

      var tripUpdateBuilder = new TripUpdateBuilder(
        tripId.getId(),
        SERVICE_DATE,
        ScheduleRelationship.SCHEDULED,
        transitModel.getTimeZone()
      );

      int stopSequence = 2;
      int delay = 1;
      tripUpdateBuilder.addDelayedStopTime(stopSequence, delay);

      final TripUpdate tripUpdate = tripUpdateBuilder.build();

      var updater = defaultUpdater();

      updater.applyTripUpdates(
        TRIP_MATCHER_NOOP,
        REQUIRED_NO_DATA,
        fullDataset,
        List.of(tripUpdate),
        feedId
      );

      final TimetableSnapshot snapshot = updater.getTimetableSnapshot();
      final Timetable forToday = snapshot.resolve(pattern, SERVICE_DATE);
      final Timetable schedule = snapshot.resolve(pattern, null);
      assertNotSame(forToday, schedule);
      assertNotSame(forToday.getTripTimes(tripIndex), schedule.getTripTimes(tripIndex));
      assertSame(forToday.getTripTimes(tripIndex2), schedule.getTripTimes(tripIndex2));
      assertEquals(1, forToday.getTripTimes(tripIndex).getArrivalDelay(1));
      assertEquals(1, forToday.getTripTimes(tripIndex).getDepartureDelay(1));

      assertEquals(RealTimeState.SCHEDULED, schedule.getTripTimes(tripIndex).getRealTimeState());
      assertEquals(RealTimeState.UPDATED, forToday.getTripTimes(tripIndex).getRealTimeState());

      assertEquals(RealTimeState.SCHEDULED, schedule.getTripTimes(tripIndex2).getRealTimeState());
      assertEquals(RealTimeState.SCHEDULED, forToday.getTripTimes(tripIndex2).getRealTimeState());
    }

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
        fullDataset,
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

    /**
     * This test just asserts that trip with start date that is outside the service period doesn't
     * throw an exception and is ignored instead.
     */
    @Test
    public void invalidTripDate() {
      // GIVEN

      String scheduledTripId = "1.1";

      var serviceDateOutsideService = SERVICE_DATE.minusYears(10);
      var builder = new TripUpdateBuilder(
        scheduledTripId,
        serviceDateOutsideService,
        SCHEDULED,
        transitModel.getTimeZone()
      )
        .addDelayedStopTime(1, 0)
        .addDelayedStopTime(2, 60, 80)
        .addDelayedStopTime(3, 90, 90);

      var tripUpdate = builder.build();

      var updater = defaultUpdater();

      // WHEN
      var result = updater.applyTripUpdates(
        TRIP_MATCHER_NOOP,
        REQUIRED_NO_DATA,
        fullDataset,
        List.of(tripUpdate),
        feedId
      );

      // THEN
      final TimetableSnapshot snapshot = updater.getTimetableSnapshot();
      assertNull(snapshot);
      assertEquals(1, result.failed());
      var errors = result.failures();
      assertEquals(1, errors.get(NO_SERVICE_ON_DATE).size());
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
        fullDataset,
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

    @Test
    public void scheduledTripWithSkippedAndScheduled() {
      // GIVEN

      String scheduledTripId = "1.1";

      var builder = new TripUpdateBuilder(
        scheduledTripId,
        SERVICE_DATE,
        SCHEDULED,
        transitModel.getTimeZone()
      )
        .addDelayedStopTime(1, 0)
        .addSkippedStop(2)
        .addDelayedStopTime(3, 90);

      var tripUpdate = builder.build();

      var updater = defaultUpdater();

      // WHEN
      updater.applyTripUpdates(
        TRIP_MATCHER_NOOP,
        REQUIRED_NO_DATA,
        fullDataset,
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

        final int originalTripIndexForToday = originalTimetableForToday.getTripIndex(tripId);
        final TripTimes originalTripTimesForToday = originalTimetableForToday.getTripTimes(
          originalTripIndexForToday
        );
        assertTrue(
          originalTripTimesForToday.isDeleted(),
          "Original trip times should be deleted in time table for service date"
        );
        // original trip should be canceled
        assertEquals(RealTimeState.DELETED, originalTripTimesForToday.getRealTimeState());
      }

      // New trip pattern
      {
        final TripPattern newTripPattern = snapshot.getRealtimeAddedTripPattern(
          new FeedScopedId(feedId, scheduledTripId),
          SERVICE_DATE
        );

        final Timetable newTimetableForToday = snapshot.resolve(newTripPattern, SERVICE_DATE);
        final Timetable newTimetableScheduled = snapshot.resolve(newTripPattern, null);

        assertNotSame(newTimetableForToday, newTimetableScheduled);

        assertTrue(newTripPattern.canBoard(0));
        assertFalse(newTripPattern.canBoard(1));
        assertTrue(newTripPattern.canBoard(2));

        final int newTimetableForTodayModifiedTripIndex = newTimetableForToday.getTripIndex(
          scheduledTripId
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
        assertEquals(45, newTripTimes.getArrivalDelay(1));
        assertEquals(45, newTripTimes.getDepartureDelay(1));
        assertEquals(90, newTripTimes.getArrivalDelay(2));
        assertEquals(90, newTripTimes.getDepartureDelay(2));
        assertFalse(newTripTimes.isCancelledStop(0));
        assertTrue(newTripTimes.isCancelledStop(1));
        assertFalse(newTripTimes.isNoDataStop(2));
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
        fullDataset,
        List.of(tripUpdate),
        feedId
      );

      // THEN
      assertAddedTrip(SERVICE_DATE, this.addedTripId, updater);
    }

    private TripPattern assertAddedTrip(
      LocalDate serviceDate,
      String tripId,
      TimetableSnapshotSource updater
    ) {
      var stopA = transitModel.getStopModel().getRegularStop(new FeedScopedId(feedId, "A"));
      // Get trip pattern of last (most recently added) outgoing edge
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
        fullDataset,
        List.of(tripUpdate),
        feedId
      );

      // THEN

      assertTrue(result.warnings().isEmpty());

      var pattern = assertAddedTrip(SERVICE_DATE, addedTripId, updater);

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
        fullDataset,
        List.of(tripUpdate),
        feedId
      );

      // THEN

      assertFalse(result.warnings().isEmpty());

      assertEquals(List.of(WarningType.UNKNOWN_STOPS_REMOVED_FROM_ADDED_TRIP), result.warnings());

      var pattern = assertAddedTrip(SERVICE_DATE, addedTripId, updater);

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
        fullDataset,
        List.of(tripUpdate),
        feedId
      );
      var pattern = assertAddedTrip(SERVICE_DATE, addedTripId, updater);
      var firstRoute = pattern.getRoute();

      // apply the update a second time to check that no new route instance is created but the old one is reused
      updater.applyTripUpdates(
        TRIP_MATCHER_NOOP,
        REQUIRED_NO_DATA,
        fullDataset,
        List.of(tripUpdate),
        feedId
      );
      var secondPattern = assertAddedTrip(SERVICE_DATE, addedTripId, updater);
      var secondRoute = secondPattern.getRoute();

      // THEN

      assertSame(firstRoute, secondRoute);
      assertNotNull(transitModel.getTransitModelIndex().getRouteForId(firstRoute.getId()));
    }
  }

  @Nonnull
  private TimetableSnapshotSource defaultUpdater() {
    return new TimetableSnapshotSource(
      TimetableSnapshotSourceParameters.DEFAULT,
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
      fullDataset,
      List.of(tripUpdateYesterday),
      feedId
    );

    final TimetableSnapshot snapshotA = updater.getTimetableSnapshot();

    // Turn the clock to tomorrow
    clock.set(tomorrow);

    updater.applyTripUpdates(
      TRIP_MATCHER_NOOP,
      REQUIRED_NO_DATA,
      fullDataset,
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
