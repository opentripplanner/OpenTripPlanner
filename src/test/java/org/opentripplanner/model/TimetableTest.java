package org.opentripplanner.model;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.gtfs.GtfsContext;
import org.opentripplanner.model.calendar.CalendarServiceData;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.algorithm.astar.AStar;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.graph_builder.module.geometry.GeometryAndBlockProcessor;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.util.TestUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.opentripplanner.gtfs.GtfsContextBuilder.contextBuilder;
import static org.opentripplanner.util.TestUtils.AUGUST;



/**
 * TODO OTP2 - Test is too close to the implementation and will need to be reimplemented.
 */
@Ignore
public class TimetableTest {
    
    private static Graph graph;
    private AStar aStar = new AStar();
    private static Map<FeedScopedId, TripPattern> patternIndex;
    private static TripPattern pattern;
    private static Timetable timetable;
    private static TimeZone timeZone = TimeZone.getTimeZone("America/New_York");
    private static ServiceDate serviceDate = new ServiceDate(2009, 8, 7);
    
    @BeforeClass
    public static void setUp() throws Exception {

        GtfsContext context = contextBuilder(ConstantsForTests.FAKE_GTFS).build();
        graph = new Graph();

        GeometryAndBlockProcessor factory = new GeometryAndBlockProcessor(context);
        factory.run(graph);
        graph.putService(
                CalendarServiceData.class, context.getCalendarServiceData()
        );

        patternIndex = new HashMap<>();

        for (TripPattern pattern : graph.tripPatternForId.values()) {
            for (Trip trip : pattern.getTrips()) {
                patternIndex.put(trip.getId(), pattern);
            }
        }
        
        pattern = patternIndex.get(new FeedScopedId("agency", "1.1"));
        timetable = pattern.scheduledTimetable;
    }

    @Test
    public void testUpdate() {
        TripUpdate tripUpdate;
        TripUpdate.Builder tripUpdateBuilder;
        TripDescriptor.Builder tripDescriptorBuilder;
        StopTimeUpdate.Builder stopTimeUpdateBuilder;
        StopTimeEvent.Builder stopTimeEventBuilder;

        String feedId = graph.getFeedIds().iterator().next();

        int trip_1_1_index = timetable.getTripIndex(new FeedScopedId("agency", "1.1"));

        Vertex stop_a = graph.getVertex(feedId + ":A");
        Vertex stop_c = graph.getVertex(feedId + ":C");
        RoutingRequest options = new RoutingRequest();

        ShortestPathTree spt;
        GraphPath path;

        // non-existing trip
        tripDescriptorBuilder = TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("b");
        tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder = TripUpdate.newBuilder();
        tripUpdateBuilder.setTrip(tripDescriptorBuilder);
        tripUpdate = tripUpdateBuilder.build();
        TripTimes updatedTripTimes = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate); 
        assertNull(updatedTripTimes);

        // update trip with bad data
        tripDescriptorBuilder = TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(
                TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder = TripUpdate.newBuilder();
        tripUpdateBuilder.setTrip(tripDescriptorBuilder);
        stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
        stopTimeUpdateBuilder.setStopSequence(0);
        stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SKIPPED);
        tripUpdate = tripUpdateBuilder.build();
        updatedTripTimes = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate); 
        assertNull(updatedTripTimes);

        // update trip with non-increasing data
        tripDescriptorBuilder = TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder = TripUpdate.newBuilder();
        tripUpdateBuilder.setTrip(tripDescriptorBuilder);
        stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setScheduleRelationship(
                StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
        stopTimeEventBuilder.setTime(TestUtils.dateInSeconds(
                "America/New_York", 2009, AUGUST, 7, 0, 10, 1));
        stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
        stopTimeEventBuilder.setTime(TestUtils.dateInSeconds(
                "America/New_York", 2009, AUGUST, 7, 0, 10, 0));
        tripUpdate = tripUpdateBuilder.build();
        updatedTripTimes = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate); 
        assertNull(updatedTripTimes);

        //---
        long startTime = TestUtils.dateInSeconds("America/New_York", 2009, AUGUST, 7, 0, 0, 0);
        long endTime;
        options.dateTime = startTime;

        //---
        options.setRoutingContext(graph, stop_a, stop_c);
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(stop_c, false);
        assertNotNull(path);
        endTime = startTime + 20 * 60;
        assertEquals(endTime, path.getEndTime());

        // update trip
        tripDescriptorBuilder = TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(
                TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder = TripUpdate.newBuilder();
        tripUpdateBuilder.setTrip(tripDescriptorBuilder);
        stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setScheduleRelationship(
                StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
        stopTimeEventBuilder.setTime(TestUtils.dateInSeconds(
                "America/New_York", 2009, AUGUST, 7, 0, 2, 0));
        stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
        stopTimeEventBuilder.setTime(TestUtils.dateInSeconds(
                "America/New_York", 2009, AUGUST, 7, 0, 2, 0));
        tripUpdate = tripUpdateBuilder.build();
        assertEquals(20*60, timetable.getTripTimes(trip_1_1_index).getArrivalTime(2));
        updatedTripTimes = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate); 
        assertNotNull(updatedTripTimes);
        timetable.setTripTimes(trip_1_1_index, updatedTripTimes);
        assertEquals(20*60 + 120, timetable.getTripTimes(trip_1_1_index).getArrivalTime(2));

        //---
        options.setRoutingContext(graph, stop_a, stop_c);
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(stop_c, false);
        assertNotNull(path);
        endTime = startTime + 20 * 60 + 120;
        assertEquals(endTime, path.getEndTime());

        // cancel trip
        tripDescriptorBuilder = TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.CANCELED);
        tripUpdateBuilder = TripUpdate.newBuilder();
        tripUpdateBuilder.setTrip(tripDescriptorBuilder);
        tripUpdate = tripUpdateBuilder.build();
        updatedTripTimes = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate); 
        assertNotNull(updatedTripTimes);
        timetable.setTripTimes(trip_1_1_index, updatedTripTimes);

        TripTimes tripTimes = timetable.getTripTimes(trip_1_1_index);
        for (int i = 0; i < tripTimes.getNumStops(); i++) {
            assertEquals(TripTimes.UNAVAILABLE, tripTimes.getDepartureTime(i));
            assertEquals(TripTimes.UNAVAILABLE, tripTimes.getArrivalTime(i));
        }

        //---
        options.setRoutingContext(graph, stop_a, stop_c);
        spt = aStar.getShortestPathTree(options);
        path = spt.getPath(stop_c, false);
        assertNotNull(path);
        endTime = startTime + 40 * 60;
        assertEquals(endTime, path.getEndTime());

        // update trip arrival time incorrectly
        tripDescriptorBuilder = TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(
                TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder = TripUpdate.newBuilder();
        tripUpdateBuilder.setTrip(tripDescriptorBuilder);
        stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setScheduleRelationship(
                StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
        stopTimeEventBuilder.setDelay(0);
        tripUpdate = tripUpdateBuilder.build();
        updatedTripTimes = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate); 
        assertNotNull(updatedTripTimes);
        timetable.setTripTimes(trip_1_1_index, updatedTripTimes);

        // update trip arrival time only
        tripDescriptorBuilder = TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(
                TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder = TripUpdate.newBuilder();
        tripUpdateBuilder.setTrip(tripDescriptorBuilder);
        stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setScheduleRelationship(
                StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
        stopTimeEventBuilder.setDelay(1);
        tripUpdate = tripUpdateBuilder.build();
        updatedTripTimes = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate); 
        assertNotNull(updatedTripTimes);
        timetable.setTripTimes(trip_1_1_index, updatedTripTimes);

        // update trip departure time only
        tripDescriptorBuilder = TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(
                TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder = TripUpdate.newBuilder();
        tripUpdateBuilder.setTrip(tripDescriptorBuilder);
        stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setScheduleRelationship(
                StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
        stopTimeEventBuilder.setDelay(-1);
        tripUpdate = tripUpdateBuilder.build();
        updatedTripTimes = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate); 
        assertNotNull(updatedTripTimes);
        timetable.setTripTimes(trip_1_1_index, updatedTripTimes);

        // update trip using stop id
        tripDescriptorBuilder = TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(
                TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder = TripUpdate.newBuilder();
        tripUpdateBuilder.setTrip(tripDescriptorBuilder);
        stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
        stopTimeUpdateBuilder.setStopId("B");
        stopTimeUpdateBuilder.setScheduleRelationship(
                StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
        stopTimeEventBuilder.setDelay(-1);
        tripUpdate = tripUpdateBuilder.build();
        updatedTripTimes = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate); 
        assertNotNull(updatedTripTimes);
        timetable.setTripTimes(trip_1_1_index, updatedTripTimes);

        // update trip arrival time at first stop and make departure time incoherent at second stop
        tripDescriptorBuilder = TripDescriptor.newBuilder();
        tripDescriptorBuilder.setTripId("1.1");
        tripDescriptorBuilder.setScheduleRelationship(
                TripDescriptor.ScheduleRelationship.SCHEDULED);
        tripUpdateBuilder = TripUpdate.newBuilder();
        tripUpdateBuilder.setTrip(tripDescriptorBuilder);
        stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
        stopTimeUpdateBuilder.setStopSequence(1);
        stopTimeUpdateBuilder.setScheduleRelationship(
                StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
        stopTimeEventBuilder.setDelay(0);
        stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(1);
        stopTimeUpdateBuilder.setStopSequence(2);
        stopTimeUpdateBuilder.setScheduleRelationship(
                StopTimeUpdate.ScheduleRelationship.SCHEDULED);
        stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
        stopTimeEventBuilder.setDelay(-1);
        tripUpdate = tripUpdateBuilder.build();
        updatedTripTimes = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate); 
        assertNull(updatedTripTimes);
    }
}
