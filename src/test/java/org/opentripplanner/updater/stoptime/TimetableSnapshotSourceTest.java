package org.opentripplanner.updater.stoptime;

import static org.junit.Assert.*;
import static org.opentripplanner.calendar.impl.CalendarServiceDataFactoryImpl.createCalendarServiceData;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.FareAttribute;
import org.opentripplanner.model.Pathway;
import org.opentripplanner.model.Route;
import org.opentripplanner.model.ServiceCalendar;
import org.opentripplanner.model.ServiceCalendarDate;
import org.opentripplanner.model.ShapePoint;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.model.OtpTransitService;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.edgetype.Timetable;
import org.opentripplanner.routing.edgetype.TimetableSnapshot;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.edgetype.factory.PatternHopFactory;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.DefaultStreetVertexIndexFactory;
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStopDepart;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;

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
        context = GtfsLibrary.readGtfs(new File(ConstantsForTests.FAKE_GTFS));

        OtpTransitService transitService = context.getOtpTransitService();

        feedId = context.getFeedId().getId();

        for (ShapePoint shapePoint : transitService.getAllShapePoints()) {
            shapePoint.getShapeId().setAgencyId(feedId);
        }
        for (Route route : transitService.getAllRoutes()) {
            route.getId().setAgencyId(feedId);
        }
        for (Stop stop : transitService.getAllStops()) {
            stop.getId().setAgencyId(feedId);
        }
        for (Trip trip : transitService.getAllTrips()) {
            trip.getId().setAgencyId(feedId);
        }
        for (ServiceCalendar serviceCalendar : transitService.getAllCalendars()) {
            serviceCalendar.getServiceId().setAgencyId(feedId);
        }
        for (ServiceCalendarDate serviceCalendarDate : transitService.getAllCalendarDates()) {
            serviceCalendarDate.getServiceId().setAgencyId(feedId);
        }
        for (FareAttribute fareAttribute : transitService.getAllFareAttributes()) {
            fareAttribute.getId().setAgencyId(feedId);
        }
        for (Pathway pathway : transitService.getAllPathways()) {
            pathway.getId().setAgencyId(feedId);
        }

        PatternHopFactory factory = new PatternHopFactory(context);
        factory.run(graph);
        graph.index(new DefaultStreetVertexIndexFactory());

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
                CalendarServiceData.class,
                createCalendarServiceData(context.getOtpTransitService())
        );
        updater = new TimetableSnapshotSource(graph);
        updater.blockUpdateWindow = 3600;//set the block update window to 1 hour
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
        final Trip trip = graph.index.tripForId.get(tripId);
        final TripPattern pattern = graph.index.patternForTrip.get(trip);
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
    public void testUpdateBlockTrips() {
        final FeedScopedId tripId = new FeedScopedId(feedId, "6.2");
        final FeedScopedId tripId1 = new FeedScopedId(feedId, "7.2");//another trip on block
        final FeedScopedId tripId2 = new FeedScopedId(feedId, "8.1");//later trip on block
        final Trip trip = graph.index.tripForId.get(tripId);
        final Trip trip1 = graph.index.tripForId.get(tripId1);
        final Trip trip2 = graph.index.tripForId.get(tripId2);
        final TripPattern pattern = graph.index.patternForTrip.get(trip);
        final TripPattern pattern1 = graph.index.patternForTrip.get(trip1);
        final TripPattern pattern2 = graph.index.patternForTrip.get(trip2);
        final int tripIndex = pattern.scheduledTimetable.getTripIndex(tripId);
        final int tripIndex1 = pattern1.scheduledTimetable.getTripIndex(tripId1);
        final int tripIndex2 = pattern2.scheduledTimetable.getTripIndex(tripId2);

        final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId("6.2");
        tripDescriptorBuilder.setScheduleRelationship(
                TripDescriptor.ScheduleRelationship.SCHEDULED);

        final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

        tripUpdateBuilder.setTrip(tripDescriptorBuilder);

        Calendar calToday = Calendar.getInstance(graph.getTimeZone());
        calToday.set(Calendar.HOUR_OF_DAY, 13);
        calToday.set(Calendar.MINUTE, 5);
        calToday.set(Calendar.SECOND, 0);
        calToday.set(Calendar.MILLISECOND, 0);
        tripUpdateBuilder.setTimestamp(calToday.getTimeInMillis() / 1000);//today at 13:05 (in sec)

        final StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder();

        stopTimeUpdateBuilder.setScheduleRelationship(
                StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeUpdateBuilder.setStopSequence(2);

        final StopTimeEvent.Builder arrivalBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
        final StopTimeEvent.Builder departureBuilder = stopTimeUpdateBuilder.getDepartureBuilder();

        Calendar _1311 = Calendar.getInstance(graph.getTimeZone());
        _1311.set(Calendar.HOUR_OF_DAY, 13);
        _1311.set(Calendar.MINUTE, 11);
        _1311.set(Calendar.SECOND, 0);
        _1311.set(Calendar.MILLISECOND, 0);
        arrivalBuilder.setTime(_1311.getTimeInMillis() / 1000);//13:11 (60 sec delay)
        departureBuilder.setTime(_1311.getTimeInMillis() / 1000);//13:11

        final TripUpdate tripUpdate = tripUpdateBuilder.build();

        updater.applyTripUpdates(graph, fullDataset, Arrays.asList(tripUpdate), feedId);

        final TimetableSnapshot snapshot = updater.getTimetableSnapshot();
        final Timetable forToday = snapshot.resolve(pattern, serviceDate);
        final Timetable schedule = snapshot.resolve(pattern, null);
        final Timetable forToday1 = snapshot.resolve(pattern1, serviceDate);
        final Timetable schedule1 = snapshot.resolve(pattern1, null);
        final Timetable forToday2 = snapshot.resolve(pattern2, serviceDate);
        final Timetable schedule2 = snapshot.resolve(pattern2, null);

        //assert that 6.2 was updated since it was from trip update
        //assert that 7.2 was updated since it is within the window (1hr set above)
        //assert that 8.3 was not updated since it was out of the window (1hr set above)

        assertNotSame(forToday, schedule);
        assertNotSame(forToday1, schedule1);
        assertSame(forToday2, schedule2);

        assertNotSame(forToday.getTripTimes(tripIndex), schedule.getTripTimes(tripIndex));
        assertNotSame(forToday1.getTripTimes(tripIndex1), schedule1.getTripTimes(tripIndex1));
        assertSame(forToday2.getTripTimes(tripIndex2), schedule2.getTripTimes(tripIndex2));

        assertEquals(60, forToday.getTripTimes(tripIndex).getArrivalDelay(1));
        assertEquals(60, forToday.getTripTimes(tripIndex).getDepartureDelay(1));
        assertEquals(60, forToday1.getTripTimes(tripIndex1).getArrivalDelay(1));
        assertEquals(60, forToday1.getTripTimes(tripIndex1).getDepartureDelay(1));

        assertEquals(RealTimeState.SCHEDULED, schedule.getTripTimes(tripIndex).getRealTimeState());
        assertEquals(RealTimeState.UPDATED, forToday.getTripTimes(tripIndex).getRealTimeState());

        assertEquals(RealTimeState.SCHEDULED, schedule1.getTripTimes(tripIndex1).getRealTimeState());
        assertEquals(RealTimeState.UPDATED, forToday1.getTripTimes(tripIndex1).getRealTimeState());

        assertEquals(RealTimeState.SCHEDULED, schedule2.getTripTimes(tripIndex2).getRealTimeState());
        assertEquals(RealTimeState.SCHEDULED, forToday2.getTripTimes(tripIndex2).getRealTimeState());
    }

    @Test
    public void testUpdateBlockTrips2() {
        final FeedScopedId tripId = new FeedScopedId(feedId, "6.2");
        final FeedScopedId tripId1 = new FeedScopedId(feedId, "7.2");//another trip on block
        final FeedScopedId tripId2 = new FeedScopedId(feedId, "8.1");//later trip on block
        final Trip trip = graph.index.tripForId.get(tripId);
        final Trip trip1 = graph.index.tripForId.get(tripId1);
        final Trip trip2 = graph.index.tripForId.get(tripId2);
        final TripPattern pattern = graph.index.patternForTrip.get(trip);
        final TripPattern pattern1 = graph.index.patternForTrip.get(trip1);
        final TripPattern pattern2 = graph.index.patternForTrip.get(trip2);
        final int tripIndex = pattern.scheduledTimetable.getTripIndex(tripId);
        final int tripIndex1 = pattern1.scheduledTimetable.getTripIndex(tripId1);
        final int tripIndex2 = pattern2.scheduledTimetable.getTripIndex(tripId2);

        final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId("7.2");
        tripDescriptorBuilder.setScheduleRelationship(
                TripDescriptor.ScheduleRelationship.SCHEDULED);

        final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

        tripUpdateBuilder.setTrip(tripDescriptorBuilder);

        Calendar calToday = Calendar.getInstance(graph.getTimeZone());
        calToday.set(Calendar.HOUR_OF_DAY, 13);
        calToday.set(Calendar.MINUTE, 5);
        calToday.set(Calendar.SECOND, 0);
        calToday.set(Calendar.MILLISECOND, 0);
        tripUpdateBuilder.setTimestamp(calToday.getTimeInMillis() / 1000);//today at 13:05 (in sec)

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
        final Timetable forToday1 = snapshot.resolve(pattern1, serviceDate);
        final Timetable schedule1 = snapshot.resolve(pattern1, null);
        final Timetable forToday2 = snapshot.resolve(pattern2, serviceDate);
        final Timetable schedule2 = snapshot.resolve(pattern2, null);

        //assert that 6.2 was not updated since it was before trip update
        //assert that 7.2 was updated since it was the trip update
        //assert that 8.3 was not updated since it was out of the window (1hr set above)

        assertSame(forToday, schedule);
        assertNotSame(forToday1, schedule1);
        assertSame(forToday2, schedule2);

        assertSame(forToday.getTripTimes(tripIndex), schedule.getTripTimes(tripIndex));
        assertNotSame(forToday1.getTripTimes(tripIndex1), schedule1.getTripTimes(tripIndex1));
        assertSame(forToday2.getTripTimes(tripIndex2), schedule2.getTripTimes(tripIndex2));

        assertEquals(1, forToday1.getTripTimes(tripIndex1).getArrivalDelay(1));
        assertEquals(1, forToday1.getTripTimes(tripIndex1).getDepartureDelay(1));

        assertEquals(RealTimeState.SCHEDULED, schedule.getTripTimes(tripIndex).getRealTimeState());
        assertEquals(RealTimeState.SCHEDULED, forToday.getTripTimes(tripIndex).getRealTimeState());

        assertEquals(RealTimeState.SCHEDULED, schedule1.getTripTimes(tripIndex1).getRealTimeState());
        assertEquals(RealTimeState.UPDATED, forToday1.getTripTimes(tripIndex1).getRealTimeState());

        assertEquals(RealTimeState.SCHEDULED, schedule2.getTripTimes(tripIndex2).getRealTimeState());
        assertEquals(RealTimeState.SCHEDULED, forToday2.getTripTimes(tripIndex2).getRealTimeState());
    }

    @Test
    public void testUpdateBlockTrips3() {
        final FeedScopedId tripId = new FeedScopedId(feedId, "18.13");
        final FeedScopedId tripId1 = new FeedScopedId(feedId, "18.14");//another trip on block
        final Trip trip = graph.index.tripForId.get(tripId);
        final Trip trip1 = graph.index.tripForId.get(tripId1);
        final TripPattern pattern = graph.index.patternForTrip.get(trip);
        final TripPattern pattern1 = graph.index.patternForTrip.get(trip1);
        final int tripIndex = pattern.scheduledTimetable.getTripIndex(tripId);
        final int tripIndex1 = pattern1.scheduledTimetable.getTripIndex(tripId1);

        final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId("18.13");
        tripDescriptorBuilder.setScheduleRelationship(
                TripDescriptor.ScheduleRelationship.SCHEDULED);

        final TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();

        Calendar calToday = Calendar.getInstance(graph.getTimeZone());
        calToday.set(Calendar.HOUR_OF_DAY, 0);
        calToday.set(Calendar.MINUTE, 7);
        calToday.set(Calendar.SECOND, 0);
        calToday.set(Calendar.MILLISECOND, 0);
        calToday.add(Calendar.DATE, 1);
        tripUpdateBuilder.setTimestamp(calToday.getTimeInMillis() / 1000);//tomorrow at 00:07 (in sec)

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
        final Timetable forToday1 = snapshot.resolve(pattern1, serviceDate);
        final Timetable schedule1 = snapshot.resolve(pattern1, null);

        assertNotSame(forToday, schedule);
        assertNotSame(forToday1, schedule1);

        assertNotSame(forToday.getTripTimes(tripIndex), schedule.getTripTimes(tripIndex));
        assertNotSame(forToday1.getTripTimes(tripIndex1), schedule1.getTripTimes(tripIndex1));


        assertEquals(1, forToday.getTripTimes(tripIndex).getArrivalDelay(1));
        assertEquals(1, forToday.getTripTimes(tripIndex).getDepartureDelay(1));
        assertEquals(1, forToday1.getTripTimes(tripIndex1).getArrivalDelay(0));
        assertEquals(1, forToday1.getTripTimes(tripIndex1).getDepartureDelay(0));
        assertEquals(1, forToday1.getTripTimes(tripIndex1).getArrivalDelay(1));
        assertEquals(1, forToday1.getTripTimes(tripIndex1).getDepartureDelay(1));

        assertEquals(RealTimeState.SCHEDULED, schedule.getTripTimes(tripIndex).getRealTimeState());
        assertEquals(RealTimeState.UPDATED, forToday.getTripTimes(tripIndex).getRealTimeState());

        assertEquals(RealTimeState.SCHEDULED, schedule1.getTripTimes(tripIndex1).getRealTimeState());
        assertEquals(RealTimeState.UPDATED, forToday1.getTripTimes(tripIndex1).getRealTimeState());
    }

    @Test
    public void testHandleDelayedTrip() {
        final FeedScopedId tripId = new FeedScopedId(feedId, "1.1");
        final FeedScopedId tripId2 = new FeedScopedId(feedId, "1.2");
        final Trip trip = graph.index.tripForId.get(tripId);
        final TripPattern pattern = graph.index.patternForTrip.get(trip);
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
            tripDescriptorBuilder.setStartDate(serviceDate.getAsString());

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
        Stop stopA = graph.index.stopForId.get(new FeedScopedId(feedId, "A"));
        TransitStopDepart transitStopDepartA = graph.index.stopVertexForStop.get(stopA).departVertex;
        // Get trip pattern of last (most recently added) outgoing edge
        final List<Edge> outgoingEdges = (List<Edge>) transitStopDepartA.getOutgoing();
        final TripPattern tripPattern = ((TransitBoardAlight) outgoingEdges.get(outgoingEdges.size() - 1)).getPattern();
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
            tripDescriptorBuilder.setStartDate(serviceDate.getAsString());

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
            final Trip trip = graph.index.tripForId.get(tripId);
            final TripPattern originalTripPattern = graph.index.patternForTrip.get(trip);

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
            final TripPattern newTripPattern = snapshot.getLastAddedTripPattern(feedId, modifiedTripId, serviceDate);
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
        final Trip trip = graph.index.tripForId.get(tripId);
        final TripPattern pattern = graph.index.patternForTrip.get(trip);

        updater.maxSnapshotFrequency = (0);
        updater.purgeExpiredData = (false);

        updater.applyTripUpdates(graph, fullDataset, Arrays.asList(TripUpdate.parseFrom(cancellation)), feedId);
        final TimetableSnapshot snapshotA = updater.getTimetableSnapshot();

        updater.purgeExpiredData = (true);

        final TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();

        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.CANCELED);
        tripDescriptorBuilder.setStartDate(previously.getAsString());

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
