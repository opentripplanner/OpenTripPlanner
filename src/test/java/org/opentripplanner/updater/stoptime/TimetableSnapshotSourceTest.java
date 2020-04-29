package org.opentripplanner.updater.stoptime;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.graph_builder.module.geometry.GeometryAndBlockProcessor;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TimetableSnapshot;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;

/**
 * TODO OTP2 - Test is too close to the implementation and will need to be reimplemented.
 */
@Ignore
public class TimetableSnapshotSourceTest {

    private static byte cancellation[];
    private static Graph graph = new Graph();
    private static boolean fullDataset = false;
    private static GtfsContext context;
    private static ServiceDate serviceDate = new ServiceDate();
    private static String feedId;

    private TimetableSnapshotSource updater;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // The ".turnOnSetAgencyToFeedIdForAllElements()" is commented out so it can be
        // removed from the code, it in no longer in use. It is not deleted here to better
        // allow the reader of the test understand how the test once worked. There should
        // be new test to replace this one.

        context = contextBuilder(ConstantsForTests.FAKE_GTFS)
                .withIssueStoreAndDeduplicator(graph)
                .build();

        feedId = context.getFeedId().getId();

        GeometryAndBlockProcessor factory = new GeometryAndBlockProcessor(context);
        factory.run(graph);
        graph.index();

        final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.CANCELED);

        final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

        tripUpdateBuilder.setTrip(tripDescriptorBuilder);

        cancellation = tripUpdateBuilder.build().toByteArray();
    }

    @Before
    public void setUp() {
        graph.putService(
                CalendarServiceData.class, context.getCalendarServiceData()
        );
        updater = new TimetableSnapshotSource(graph);
    }

    @Test
    public void testGetSnapshot() throws InvalidProtocolBufferException {
        updater.applyTripUpdates(graph, fullDataset, Arrays.asList(TripUpdate.parseFrom(cancellation)), feedId);

        final TimetableSnapshot snapshot = updater.getTimetableSnapshot();
        assertNotNull(snapshot);
        assertSame(snapshot, updater.getTimetableSnapshot());

        updater.applyTripUpdates(graph, fullDataset, Arrays.asList(TripUpdate.parseFrom(cancellation)), feedId);
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
        final int tripIndex = pattern.scheduledTimetable.getTripIndex(tripId);
        final int tripIndex2 = pattern.scheduledTimetable.getTripIndex(tripId2);

        updater.applyTripUpdates(graph, fullDataset, Arrays.asList(TripUpdate.parseFrom(cancellation)), feedId);

        final TimetableSnapshot snapshot = updater.getTimetableSnapshot();
        final Timetable forToday = snapshot.resolve(pattern, serviceDate);
        final Timetable schedule = snapshot.resolve(pattern, null);
        assertNotSame(forToday, schedule);
        assertNotSame(forToday.getTripTimes(tripIndex), schedule.getTripTimes(tripIndex));
        assertSame(forToday.getTripTimes(tripIndex2), schedule.getTripTimes(tripIndex2));

        final TripTimes tripTimes = forToday.getTripTimes(tripIndex);
        for (int i = 0; i < tripTimes.getNumStops(); i++) {
            assertEquals(TripTimes.UNAVAILABLE, tripTimes.getDepartureTime(i));
            assertEquals(TripTimes.UNAVAILABLE, tripTimes.getArrivalTime(i));
        }
        assertEquals(RealTimeState.CANCELED, tripTimes.getRealTimeState());
    }

    @Test
    public void testHandleDelayedTrip() {
        final FeedScopedId tripId = new FeedScopedId(feedId, "1.1");
        final FeedScopedId tripId2 = new FeedScopedId(feedId, "1.2");
        final Trip trip = graph.index.getTripForId().get(tripId);
        final TripPattern pattern = graph.index.getPatternForTrip().get(trip);
        final int tripIndex = pattern.scheduledTimetable.getTripIndex(tripId);
        final int tripIndex2 = pattern.scheduledTimetable.getTripIndex(tripId2);

        final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(
               TripDescriptor.ScheduleRelationship.SCHEDULED);

        final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

        tripUpdateBuilder.setTrip(tripDescriptorBuilder);

        final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder();

        stopTimeUpdateBuilder.setScheduleRelationship(
                StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopSequence(2);

        final StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
        final StopTimeEvent.Builder departureBuilder = stopTimeUpdateBuilder.getDepartureBuilder();

        arrivalBuilder.setDelay(1);
        departureBuilder.setDelay(1);

        final TripUpdate tripUpdate = tripUpdateBuilder.build();

        updater.applyTripUpdates(graph, fullDataset, Arrays.asList(tripUpdate), feedId);

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
    public void testHandleAddedTrip() throws ParseException {
        // GIVEN

        // Get service date of today because old dates will be purged after applying updates
        final ServiceDate serviceDate = new ServiceDate(Calendar.getInstance());

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
                stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
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
                stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
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
                stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
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
        updater.applyTripUpdates(graph, fullDataset, Arrays.asList(tripUpdate), feedId);

        // THEN
        // Find new pattern in graph starting from stop A
        Stop stopA = graph.index.getStopForId(new FeedScopedId(feedId, "A"));
        // Get trip pattern of last (most recently added) outgoing edge
        // FIXME create a new test to see that add-trip realtime updates work
        TripPattern tripPattern = null;
        assertNotNull("Added trip pattern should be found", tripPattern);

        final TimetableSnapshot snapshot = updater.getTimetableSnapshot();
        final Timetable forToday = snapshot.resolve(tripPattern, serviceDate);
        final Timetable schedule = snapshot.resolve(tripPattern, null);

        assertNotSame(forToday, schedule);

        final int forTodayAddedTripIndex = forToday.getTripIndex(addedTripId);
        assertTrue("Added trip should be found in time table for service date", forTodayAddedTripIndex > -1);
        assertEquals(RealTimeState.ADDED, forToday.getTripTimes(forTodayAddedTripIndex).getRealTimeState());

        final int scheduleTripIndex = schedule.getTripIndex(addedTripId);
        assertEquals("Added trip should not be found in scheduled time table", -1, scheduleTripIndex);
    }

    @Test
    public void testHandleModifiedTrip() throws ParseException {
        // TODO

        // GIVEN

        // Get service date of today because old dates will be purged after applying updates
        ServiceDate serviceDate = new ServiceDate(Calendar.getInstance());
        String modifiedTripId = "10.1";

        TripUpdate tripUpdate;
        {
            final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

            tripDescriptorBuilder.setTripId(modifiedTripId);
            tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.MODIFIED);
            tripDescriptorBuilder.setStartDate(serviceDate.asCompactString());

            final Calendar calendar = serviceDate.getAsCalendar(graph.getTimeZone());
            final long midnightSecondsSinceEpoch = calendar.getTimeInMillis() / 1000;

            final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

            tripUpdateBuilder.setTrip(tripDescriptorBuilder);

            { // Stop O
                final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder();
                stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
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
                stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
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
                stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
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
        updater.applyTripUpdates(graph, fullDataset, Arrays.asList(tripUpdate), feedId);
        
        // THEN
        final TimetableSnapshot snapshot = updater.getTimetableSnapshot();

        // Original trip pattern
        {
            final FeedScopedId tripId = new FeedScopedId(feedId, modifiedTripId);
            final Trip trip = graph.index.getTripForId().get(tripId);
            final TripPattern originalTripPattern = graph.index.getPatternForTrip().get(trip);

            final Timetable originalTimetableForToday = snapshot.resolve(originalTripPattern, serviceDate);
            final Timetable originalTimetableScheduled = snapshot.resolve(originalTripPattern, null);

            assertNotSame(originalTimetableForToday, originalTimetableScheduled);

            final int originalTripIndexScheduled = originalTimetableScheduled.getTripIndex(modifiedTripId);
            assertTrue("Original trip should be found in scheduled time table", originalTripIndexScheduled > -1);
            final TripTimes originalTripTimesScheduled = originalTimetableScheduled.getTripTimes(originalTripIndexScheduled);
            assertFalse("Original trip times should not be canceled in scheduled time table", originalTripTimesScheduled.isCanceled());
            assertEquals(RealTimeState.SCHEDULED, originalTripTimesScheduled.getRealTimeState());

            final int originalTripIndexForToday = originalTimetableForToday.getTripIndex(modifiedTripId);
            assertTrue("Original trip should be found in time table for service date", originalTripIndexForToday > -1);
            final TripTimes originalTripTimesForToday = originalTimetableForToday.getTripTimes(originalTripIndexForToday);
            assertTrue("Original trip times should be canceled in time table for service date", originalTripTimesForToday.isCanceled());
            assertEquals(RealTimeState.CANCELED, originalTripTimesForToday.getRealTimeState());
        }

        // New trip pattern
        {
            final TripPattern newTripPattern = snapshot.getLastAddedTripPattern(new FeedScopedId(feedId, modifiedTripId), serviceDate);
            assertNotNull("New trip pattern should be found", newTripPattern);

            final Timetable newTimetableForToday = snapshot.resolve(newTripPattern, serviceDate);
            final Timetable newTimetableScheduled = snapshot.resolve(newTripPattern, null);

            assertNotSame(newTimetableForToday, newTimetableScheduled);

            final int newTimetableForTodayModifiedTripIndex = newTimetableForToday.getTripIndex(modifiedTripId);
            assertTrue("New trip should be found in time table for service date", newTimetableForTodayModifiedTripIndex > -1);
            assertEquals(RealTimeState.MODIFIED, newTimetableForToday.getTripTimes(newTimetableForTodayModifiedTripIndex).getRealTimeState());

            assertEquals("New trip should not be found in scheduled time table", -1, newTimetableScheduled.getTripIndex(modifiedTripId));
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

        updater.applyTripUpdates(graph, fullDataset, Arrays.asList(TripUpdate.parseFrom(cancellation)), feedId);
        final TimetableSnapshot snapshotA = updater.getTimetableSnapshot();

        updater.purgeExpiredData = true;

        final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.CANCELED);
        tripDescriptorBuilder.setStartDate(previously.asCompactString());

        final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

        tripUpdateBuilder.setTrip(tripDescriptorBuilder);

        final TripUpdate tripUpdate = tripUpdateBuilder.build();

        updater.applyTripUpdates(graph, fullDataset, Arrays.asList(tripUpdate), feedId);
        final TimetableSnapshot snapshotB = updater.getTimetableSnapshot();

        assertNotSame(snapshotA, snapshotB);

        assertSame   (snapshotA.resolve(pattern, null ), snapshotB.resolve(pattern, null ));
        assertSame   (snapshotA.resolve(pattern, serviceDate), snapshotB.resolve(pattern, serviceDate));
        assertNotSame(snapshotA.resolve(pattern, null ), snapshotA.resolve(pattern, serviceDate));
        assertSame   (snapshotB.resolve(pattern, null ), snapshotB.resolve(pattern, previously));
    }
}
