package org.opentripplanner.updater.trip;

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

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.test.support.VariableSource;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.GtfsRealtimeFuzzyTripMatcher;
import org.opentripplanner.updater.TimetableSnapshotSourceParameters;

public class TimetableSnapshotSourceTest {

  private TransitModel transitModel;

  private final GtfsRealtimeFuzzyTripMatcher TRIP_MATCHER_NOOP = null;

  private final boolean fullDataset = false;
  private LocalDate serviceDate;
  private byte[] cancellation;
  private String feedId;

  @BeforeEach
  public void setUp() {
    TestOtpModel model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.FAKE_GTFS);
    transitModel = model.transitModel();
    // fuzzyTripMatcher = new GtfsRealtimeFuzzyTripMatcher(new DefaultTransitService(transitModel));

    feedId = transitModel.getFeedIds().stream().findFirst().get();

    serviceDate = LocalDate.now(transitModel.getTimeZone());

    final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.CANCELED);

    final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

    tripUpdateBuilder.setTrip(tripDescriptorBuilder);

    cancellation = tripUpdateBuilder.build().toByteArray();
  }

  @Test
  public void testGetSnapshot() throws InvalidProtocolBufferException {
    var updater = new TimetableSnapshotSource(
      TimetableSnapshotSourceParameters.DEFAULT,
      transitModel
    );

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
      TimetableSnapshotSourceParameters.DEFAULT.withMaxSnapshotFrequencyMs(-1),
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
      transitModel
    );

    updater.applyTripUpdates(
      TRIP_MATCHER_NOOP,
      REQUIRED_NO_DATA,
      fullDataset,
      List.of(TripUpdate.parseFrom(cancellation)),
      feedId
    );

    final TimetableSnapshot snapshot = updater.getTimetableSnapshot();
    final Timetable forToday = snapshot.resolve(pattern, serviceDate);
    final Timetable schedule = snapshot.resolve(pattern, null);
    assertNotSame(forToday, schedule);
    assertNotSame(forToday.getTripTimes(tripIndex), schedule.getTripTimes(tripIndex));
    assertSame(forToday.getTripTimes(tripIndex2), schedule.getTripTimes(tripIndex2));

    final TripTimes tripTimes = forToday.getTripTimes(tripIndex);

    assertEquals(RealTimeState.CANCELED, tripTimes.getRealTimeState());
  }

  @Test
  public void testHandleDelayedTrip() {
    final FeedScopedId tripId = new FeedScopedId(feedId, "1.1");
    final FeedScopedId tripId2 = new FeedScopedId(feedId, "1.2");
    final Trip trip = transitModel.getTransitModelIndex().getTripForId().get(tripId);
    final TripPattern pattern = transitModel.getTransitModelIndex().getPatternForTrip().get(trip);
    final int tripIndex = pattern.getScheduledTimetable().getTripIndex(tripId);
    final int tripIndex2 = pattern.getScheduledTimetable().getTripIndex(tripId2);

    final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);

    final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

    tripUpdateBuilder.setTrip(tripDescriptorBuilder);

    final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder();

    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeUpdateBuilder.setStopSequence(2);

    final StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    final StopTimeEvent.Builder departureBuilder = stopTimeUpdateBuilder.getDepartureBuilder();

    arrivalBuilder.setDelay(1);
    departureBuilder.setDelay(1);

    final TripUpdate tripUpdate = tripUpdateBuilder.build();

    var updater = new TimetableSnapshotSource(
      TimetableSnapshotSourceParameters.DEFAULT,
      transitModel
    );

    updater.applyTripUpdates(
      TRIP_MATCHER_NOOP,
      REQUIRED_NO_DATA,
      fullDataset,
      List.of(tripUpdate),
      feedId
    );

    final TimetableSnapshot snapshot = updater.getTimetableSnapshot();
    final Timetable forToday = snapshot.resolve(pattern, serviceDate);
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
  public void testHandleAddedTrip() {
    // GIVEN

    // Get service date of today because old dates will be purged after applying updates
    final LocalDate serviceDate = LocalDate.now(transitModel.getTimeZone());

    final String addedTripId = "added_trip";

    TripUpdate tripUpdate;
    {
      final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

      tripDescriptorBuilder.setTripId(addedTripId);
      tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.ADDED);
      tripDescriptorBuilder.setStartDate(ServiceDateUtils.asCompactString(serviceDate));

      final long midnightSecondsSinceEpoch = ServiceDateUtils
        .asStartOfService(serviceDate, transitModel.getTimeZone())
        .toEpochSecond();

      final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

      tripUpdateBuilder.setTrip(tripDescriptorBuilder);

      { // Stop A
        final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder();
        stopTimeUpdateBuilder.setScheduleRelationship(
          StopTimeUpdate.ScheduleRelationship.SCHEDULED
        );
        stopTimeUpdateBuilder.setStopId("A");

        { // Arrival
          final StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
          arrivalBuilder.setTime(midnightSecondsSinceEpoch + (8 * 3600) + (30 * 60));
          arrivalBuilder.setDelay(0);
        }

        { // Departure
          final StopTimeEvent.Builder departureBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
          departureBuilder.setTime(midnightSecondsSinceEpoch + (8 * 3600) + (30 * 60));
          departureBuilder.setDelay(0);
        }
      }

      { // Stop C
        final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder();
        stopTimeUpdateBuilder.setScheduleRelationship(
          StopTimeUpdate.ScheduleRelationship.SCHEDULED
        );
        stopTimeUpdateBuilder.setStopId("C");

        { // Arrival
          final StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
          arrivalBuilder.setTime(midnightSecondsSinceEpoch + (8 * 3600) + (40 * 60));
          arrivalBuilder.setDelay(0);
        }

        { // Departure
          final StopTimeEvent.Builder departureBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
          departureBuilder.setTime(midnightSecondsSinceEpoch + (8 * 3600) + (45 * 60));
          departureBuilder.setDelay(0);
        }
      }

      { // Stop E
        final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder();
        stopTimeUpdateBuilder.setScheduleRelationship(
          StopTimeUpdate.ScheduleRelationship.SCHEDULED
        );
        stopTimeUpdateBuilder.setStopId("E");

        { // Arrival
          final StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
          arrivalBuilder.setTime(midnightSecondsSinceEpoch + (8 * 3600) + (55 * 60));
          arrivalBuilder.setDelay(0);
        }

        { // Departure
          final StopTimeEvent.Builder departureBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
          departureBuilder.setTime(midnightSecondsSinceEpoch + (8 * 3600) + (55 * 60));
          departureBuilder.setDelay(0);
        }
      }

      tripUpdate = tripUpdateBuilder.build();
    }

    var updater = new TimetableSnapshotSource(
      TimetableSnapshotSourceParameters.DEFAULT,
      transitModel
    );

    // WHEN
    updater.applyTripUpdates(
      TRIP_MATCHER_NOOP,
      REQUIRED_NO_DATA,
      fullDataset,
      List.of(tripUpdate),
      feedId
    );

    // THEN
    // Find new pattern in graph starting from stop A
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

    final int forTodayAddedTripIndex = forToday.getTripIndex(addedTripId);
    assertTrue(
      forTodayAddedTripIndex > -1,
      "Added trip should be found in time table for service date"
    );
    assertEquals(
      RealTimeState.ADDED,
      forToday.getTripTimes(forTodayAddedTripIndex).getRealTimeState()
    );

    final int scheduleTripIndex = schedule.getTripIndex(addedTripId);
    assertEquals(-1, scheduleTripIndex, "Added trip should not be found in scheduled time table");
  }

  @Test
  public void testHandleModifiedTrip() {
    // GIVEN

    // Get service date of today because old dates will be purged after applying updates
    LocalDate serviceDate = LocalDate.now(transitModel.getTimeZone());
    String modifiedTripId = "10.1";

    TripUpdate tripUpdate;
    {
      final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

      tripDescriptorBuilder.setTripId(modifiedTripId);
      tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.REPLACEMENT);
      tripDescriptorBuilder.setStartDate(ServiceDateUtils.asCompactString(serviceDate));

      final long midnightSecondsSinceEpoch = ServiceDateUtils
        .asStartOfService(serviceDate, transitModel.getTimeZone())
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
        stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SKIPPED);
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

    var updater = new TimetableSnapshotSource(
      TimetableSnapshotSourceParameters.DEFAULT,
      transitModel
    );

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
        serviceDate
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
        originalTripTimesScheduled.isCanceled(),
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
        originalTripTimesForToday.isCanceled(),
        "Original trip times should be canceled in time table for service date"
      );
      assertEquals(RealTimeState.CANCELED, originalTripTimesForToday.getRealTimeState());
    }

    // New trip pattern
    {
      final TripPattern newTripPattern = snapshot.getRealtimeAddedTripPattern(
        new FeedScopedId(feedId, modifiedTripId),
        serviceDate
      );
      assertNotNull(newTripPattern, "New trip pattern should be found");

      final Timetable newTimetableForToday = snapshot.resolve(newTripPattern, serviceDate);
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

  @Test
  public void testHandleScheduledTrip() {
    // GIVEN

    // Get service date of today because old dates will be purged after applying updates
    LocalDate serviceDate = LocalDate.now(transitModel.getTimeZone());
    String scheduledTripId = "1.1";

    TripUpdate tripUpdate;
    {
      final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

      tripDescriptorBuilder.setTripId(scheduledTripId);
      tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.SCHEDULED);
      tripDescriptorBuilder.setStartDate(ServiceDateUtils.asCompactString(serviceDate));

      final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

      tripUpdateBuilder.setTrip(tripDescriptorBuilder);

      { // First stop
        final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(
          0
        );
        stopTimeUpdateBuilder.setScheduleRelationship(
          StopTimeUpdate.ScheduleRelationship.SCHEDULED
        );
        stopTimeUpdateBuilder.setStopSequence(1);

        { // Departure
          final StopTimeEvent.Builder departureBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
          departureBuilder.setDelay(0);
        }
      }

      { // Second stop
        final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(
          1
        );
        stopTimeUpdateBuilder.setScheduleRelationship(
          StopTimeUpdate.ScheduleRelationship.SCHEDULED
        );
        stopTimeUpdateBuilder.setStopSequence(2);

        { // Arrival
          final StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
          arrivalBuilder.setDelay(60);
        }
        { // Departure
          final StopTimeEvent.Builder departureBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
          departureBuilder.setDelay(80);
        }
      }

      { // Last stop
        final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(
          2
        );
        stopTimeUpdateBuilder.setScheduleRelationship(
          StopTimeUpdate.ScheduleRelationship.SCHEDULED
        );
        stopTimeUpdateBuilder.setStopSequence(3);

        { // Arrival
          final StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
          arrivalBuilder.setDelay(90);
        }
        { // Departure
          final StopTimeEvent.Builder departureBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
          departureBuilder.setDelay(90);
        }
      }

      tripUpdate = tripUpdateBuilder.build();
    }

    var updater = new TimetableSnapshotSource(
      TimetableSnapshotSourceParameters.DEFAULT,
      transitModel
    );

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

    final Timetable originalTimetableForToday = snapshot.resolve(originalTripPattern, serviceDate);
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
      originalTripTimesScheduled.isCanceled(),
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
  public void testHandleScheduledTripWithSkippedAndNoData() {
    // GIVEN

    // Get service date of today because old dates will be purged after applying updates
    LocalDate serviceDate = LocalDate.now(transitModel.getTimeZone());
    String scheduledTripId = "1.1";

    TripUpdate tripUpdate;
    {
      final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

      tripDescriptorBuilder.setTripId(scheduledTripId);
      tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.SCHEDULED);
      tripDescriptorBuilder.setStartDate(ServiceDateUtils.asCompactString(serviceDate));

      final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

      tripUpdateBuilder.setTrip(tripDescriptorBuilder);

      { // First stop
        final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(
          0
        );
        stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.NO_DATA);
        stopTimeUpdateBuilder.setStopSequence(1);
      }

      { // Second stop
        final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(
          1
        );
        stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SKIPPED);
        stopTimeUpdateBuilder.setStopSequence(2);
      }

      { // Last stop
        final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(
          2
        );
        stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.NO_DATA);
        stopTimeUpdateBuilder.setStopSequence(3);
      }

      tripUpdate = tripUpdateBuilder.build();
    }

    var updater = new TimetableSnapshotSource(
      TimetableSnapshotSourceParameters.DEFAULT,
      transitModel
    );

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
        serviceDate
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
        originalTripTimesScheduled.isCanceled(),
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
      // original trip should be canceled
      assertEquals(RealTimeState.CANCELED, originalTripTimesForToday.getRealTimeState());
    }

    // New trip pattern
    {
      final TripPattern newTripPattern = snapshot.getRealtimeAddedTripPattern(
        new FeedScopedId(feedId, scheduledTripId),
        serviceDate
      );
      assertNotNull(newTripPattern, "New trip pattern should be found");

      final Timetable newTimetableForToday = snapshot.resolve(newTripPattern, serviceDate);
      final Timetable newTimetableScheduled = snapshot.resolve(newTripPattern, null);

      assertNotSame(newTimetableForToday, newTimetableScheduled);

      assertTrue(newTripPattern.canBoard(0));
      assertFalse(newTripPattern.canBoard(1));
      assertTrue(newTripPattern.canBoard(2));

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
  public void testHandleScheduledTripWithSkippedAndScheduled() {
    // GIVEN

    // Get service date of today because old dates will be purged after applying updates
    LocalDate serviceDate = LocalDate.now();
    String scheduledTripId = "1.1";

    TripUpdate tripUpdate;
    {
      final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

      tripDescriptorBuilder.setTripId(scheduledTripId);
      tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.SCHEDULED);
      tripDescriptorBuilder.setStartDate(ServiceDateUtils.asCompactString(serviceDate));

      final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

      tripUpdateBuilder.setTrip(tripDescriptorBuilder);

      { // First stop
        final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(
          0
        );
        stopTimeUpdateBuilder.setScheduleRelationship(
          StopTimeUpdate.ScheduleRelationship.SCHEDULED
        );
        stopTimeUpdateBuilder.setStopSequence(1);

        { // Departure
          final StopTimeEvent.Builder departureBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
          departureBuilder.setDelay(0);
        }
      }

      { // Second stop
        final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(
          1
        );
        stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SKIPPED);
        stopTimeUpdateBuilder.setStopSequence(2);
      }

      { // Last stop
        final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(
          2
        );
        stopTimeUpdateBuilder.setScheduleRelationship(
          StopTimeUpdate.ScheduleRelationship.SCHEDULED
        );
        stopTimeUpdateBuilder.setStopSequence(3);

        { // Arrival
          final StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
          arrivalBuilder.setDelay(90);
        }
        { // Departure
          final StopTimeEvent.Builder departureBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
          departureBuilder.setDelay(90);
        }
      }

      tripUpdate = tripUpdateBuilder.build();
    }

    var updater = new TimetableSnapshotSource(
      TimetableSnapshotSourceParameters.DEFAULT,
      transitModel
    );

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
        serviceDate
      );
      final Timetable originalTimetableScheduled = snapshot.resolve(originalTripPattern, null);

      assertNotSame(originalTimetableForToday, originalTimetableScheduled);

      final int originalTripIndexForToday = originalTimetableForToday.getTripIndex(tripId);
      final TripTimes originalTripTimesForToday = originalTimetableForToday.getTripTimes(
        originalTripIndexForToday
      );
      // original trip should be canceled
      assertEquals(RealTimeState.CANCELED, originalTripTimesForToday.getRealTimeState());
    }

    // New trip pattern
    {
      final TripPattern newTripPattern = snapshot.getRealtimeAddedTripPattern(
        new FeedScopedId(feedId, scheduledTripId),
        serviceDate
      );

      final Timetable newTimetableForToday = snapshot.resolve(newTripPattern, serviceDate);
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
      assertEquals(0, newTripTimes.getArrivalDelay(1));
      assertEquals(0, newTripTimes.getDepartureDelay(1));
      assertEquals(90, newTripTimes.getArrivalDelay(2));
      assertEquals(90, newTripTimes.getDepartureDelay(2));
      assertFalse(newTripTimes.isCancelledStop(0));
      assertTrue(newTripTimes.isCancelledStop(1));
      assertFalse(newTripTimes.isNoDataStop(2));
    }
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

  static Stream<Arguments> purgeExpiredDataTestCases = Stream.of(
    // purgeExpiredData   maxSnapshotFrequency || snapshots PatternSnapshotA  PatternSnapshotB
    Arguments.of(Boolean.TRUE, -1, NotSame, NotSame),
    Arguments.of(Boolean.FALSE, -1, NotSame, Same),
    Arguments.of(Boolean.TRUE, 1000, NotSame, NotSame),
    Arguments.of(Boolean.FALSE, 1000, Same, Same)
  );

  @ParameterizedTest(name = "purgeExpired: {0}, maxFrequency: {1}  ||  {2}  {3}")
  @VariableSource("purgeExpiredDataTestCases")
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
    final LocalDate yesterday = serviceDate.minusDays(1);
    final LocalDate tomorrow = serviceDate.plusDays(1);
    final AtomicReference<LocalDate> clock = new AtomicReference<>(yesterday);

    var tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.CANCELED);

    tripDescriptorBuilder.setStartDate(ServiceDateUtils.asCompactString(yesterday));
    var tripUpdateYesterday = TripUpdate.newBuilder().setTrip(tripDescriptorBuilder).build();

    // Update pattern on today, even if the time the update is performed is tomorrow
    tripDescriptorBuilder.setStartDate(ServiceDateUtils.asCompactString(serviceDate));
    var tripUpdateToday = TripUpdate.newBuilder().setTrip(tripDescriptorBuilder).build();

    var updater = new TimetableSnapshotSource(
      TimetableSnapshotSourceParameters.DEFAULT
        .withPurgeExpiredData(purgeExpiredData)
        .withMaxSnapshotFrequencyMs(maxSnapshotFrequency),
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
