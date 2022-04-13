package org.opentripplanner.updater.stoptime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import java.time.LocalDate;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.CalendarService;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.GraphIndex;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;

public class TimetableSnapshotSourceTest {

  static Graph graph = new Graph();
  private static final boolean fullDataset = false;
  private static final ServiceDate serviceDate = new ServiceDate();
  private static byte[] cancellation;
  private static String feedId;

  private TimetableSnapshotSource updater;

  @BeforeAll
  public static void setUpClass() {
    graph = ConstantsForTests.buildGtfsGraph(ConstantsForTests.FAKE_GTFS);

    feedId = graph.getFeedIds().stream().findFirst().get();

    final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.CANCELED);

    final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

    tripUpdateBuilder.setTrip(tripDescriptorBuilder);

    cancellation = tripUpdateBuilder.build().toByteArray();
  }

  @BeforeEach
  public void setUp() {
    updater = new TimetableSnapshotSource(graph);
  }

  @Test
  public void testGetSnapshot() throws InvalidProtocolBufferException {
    CalendarService calendarService = graph.getCalendarService();
    Deduplicator deduplicator = graph.deduplicator;
    GraphIndex graphIndex = graph.index;
    Map<FeedScopedId, Integer> serviceCodes = graph.getServiceCodes();
    updater.applyTripUpdates(
      calendarService,
      deduplicator,
      graphIndex,
      serviceCodes,
      fullDataset,
      List.of(TripUpdate.parseFrom(cancellation)),
      feedId
    );

    final TimetableSnapshot snapshot = updater.getTimetableSnapshot();
    assertNotNull(snapshot);
    assertSame(snapshot, updater.getTimetableSnapshot());

    updater.applyTripUpdates(
      calendarService,
      deduplicator,
      graphIndex,
      serviceCodes,
      fullDataset,
      List.of(TripUpdate.parseFrom(cancellation)),
      feedId
    );
    assertSame(snapshot, updater.getTimetableSnapshot());

    updater.maxSnapshotFrequency = (-1);
    final TimetableSnapshot newSnapshot = updater.getTimetableSnapshot();
    assertNotNull(newSnapshot);
    assertNotSame(snapshot, newSnapshot);
  }

  @Test
  public void testHandleCanceledTrip() throws InvalidProtocolBufferException {
    final FeedScopedId tripId = new FeedScopedId(feedId, "1.1");
    final FeedScopedId tripId2 = new FeedScopedId(feedId, "1.2");
    final Trip trip = graph.index.getTripForId().get(tripId);
    final TripPattern pattern = graph.index.getPatternForTrip().get(trip);
    final int tripIndex = pattern.getScheduledTimetable().getTripIndex(tripId);
    final int tripIndex2 = pattern.getScheduledTimetable().getTripIndex(tripId2);

    CalendarService calendarService = graph.getCalendarService();
    Deduplicator deduplicator = graph.deduplicator;
    GraphIndex graphIndex = graph.index;
    Map<FeedScopedId, Integer> serviceCodes = graph.getServiceCodes();

    updater.applyTripUpdates(
      calendarService,
      deduplicator,
      graphIndex,
      serviceCodes,
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
    final Trip trip = graph.index.getTripForId().get(tripId);
    final TripPattern pattern = graph.index.getPatternForTrip().get(trip);
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

    CalendarService calendarService = graph.getCalendarService();
    Deduplicator deduplicator = graph.deduplicator;
    GraphIndex graphIndex = graph.index;
    Map<FeedScopedId, Integer> serviceCodes = graph.getServiceCodes();
    updater.applyTripUpdates(
      calendarService,
      deduplicator,
      graphIndex,
      serviceCodes,
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

  @Test
  public void testHandleAddedTrip() {
    // GIVEN

    // Get service date of today because old dates will be purged after applying updates
    final ServiceDate serviceDate = new ServiceDate(LocalDate.now());

    final String addedTripId = "added_trip";

    TripUpdate tripUpdate;
    {
      final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

      tripDescriptorBuilder.setTripId(addedTripId);
      tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.ADDED);
      tripDescriptorBuilder.setStartDate(serviceDate.asCompactString());

      final Calendar calendar = serviceDate.getAsCalendar(graph.getTimeZone());
      final long midnightSecondsSinceEpoch = calendar.getTimeInMillis() / 1000;

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

    // WHEN
    CalendarService calendarService = graph.getCalendarService();
    Deduplicator deduplicator = graph.deduplicator;
    GraphIndex graphIndex = graph.index;

    Map<FeedScopedId, Integer> serviceCodes = graph.getServiceCodes();
    updater.applyTripUpdates(
      calendarService,
      deduplicator,
      graphIndex,
      serviceCodes,
      fullDataset,
      List.of(tripUpdate),
      feedId
    );

    // THEN
    // Find new pattern in graph starting from stop A
    var stopA = graph.index.getStopForId(new FeedScopedId(feedId, "A"));
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
    ServiceDate serviceDate = new ServiceDate(LocalDate.now());
    String modifiedTripId = "10.1";

    TripUpdate tripUpdate;
    {
      final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

      tripDescriptorBuilder.setTripId(modifiedTripId);
      tripDescriptorBuilder.setScheduleRelationship(ScheduleRelationship.REPLACEMENT);
      tripDescriptorBuilder.setStartDate(serviceDate.asCompactString());

      final Calendar calendar = serviceDate.getAsCalendar(graph.getTimeZone());
      final long midnightSecondsSinceEpoch = calendar.getTimeInMillis() / 1000;

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

    // WHEN
    CalendarService calendarService = graph.getCalendarService();
    Deduplicator deduplicator = graph.deduplicator;
    GraphIndex graphIndex = graph.index;
    Map<FeedScopedId, Integer> serviceCodes = graph.getServiceCodes();
    updater.applyTripUpdates(
      calendarService,
      deduplicator,
      graphIndex,
      serviceCodes,
      fullDataset,
      List.of(tripUpdate),
      feedId
    );

    // THEN
    final TimetableSnapshot snapshot = updater.getTimetableSnapshot();

    // Original trip pattern
    {
      final FeedScopedId tripId = new FeedScopedId(feedId, modifiedTripId);
      final Trip trip = graph.index.getTripForId().get(tripId);
      final TripPattern originalTripPattern = graph.index.getPatternForTrip().get(trip);

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
      final TripPattern newTripPattern = snapshot.getLastAddedTripPattern(
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
  public void testPurgeExpiredData() throws InvalidProtocolBufferException {
    final FeedScopedId tripId = new FeedScopedId(feedId, "1.1");
    final ServiceDate previously = serviceDate.previous().previous(); // Just to be safe...
    final Trip trip = graph.index.getTripForId().get(tripId);
    final TripPattern pattern = graph.index.getPatternForTrip().get(trip);

    updater.maxSnapshotFrequency = 0;
    updater.purgeExpiredData = false;

    CalendarService calendarService = graph.getCalendarService();
    Deduplicator deduplicator = graph.deduplicator;
    GraphIndex graphIndex = graph.index;
    Map<FeedScopedId, Integer> serviceCodes = graph.getServiceCodes();
    updater.applyTripUpdates(
      calendarService,
      deduplicator,
      graphIndex,
      serviceCodes,
      fullDataset,
      List.of(TripUpdate.parseFrom(cancellation)),
      feedId
    );
    final TimetableSnapshot snapshotA = updater.getTimetableSnapshot();

    updater.purgeExpiredData = true;

    final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.CANCELED);
    tripDescriptorBuilder.setStartDate(previously.asCompactString());

    final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

    tripUpdateBuilder.setTrip(tripDescriptorBuilder);

    final TripUpdate tripUpdate = tripUpdateBuilder.build();

    updater.applyTripUpdates(
      calendarService,
      deduplicator,
      graphIndex,
      serviceCodes,
      fullDataset,
      List.of(tripUpdate),
      feedId
    );
    final TimetableSnapshot snapshotB = updater.getTimetableSnapshot();

    assertNotSame(snapshotA, snapshotB);

    assertSame(snapshotA.resolve(pattern, null), snapshotB.resolve(pattern, null));
    assertSame(snapshotA.resolve(pattern, serviceDate), snapshotB.resolve(pattern, serviceDate));
    assertNotSame(snapshotA.resolve(pattern, null), snapshotA.resolve(pattern, serviceDate));
    assertSame(snapshotB.resolve(pattern, null), snapshotB.resolve(pattern, previously));
  }
}
