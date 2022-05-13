package org.opentripplanner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.opentripplanner.util.TestUtils.AUGUST;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.model.calendar.ServiceDate;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.util.TestUtils;

public class TimetableTest {

  private static final TimeZone timeZone = TimeZone.getTimeZone("America/New_York");
  private static final ServiceDate serviceDate = new ServiceDate(2009, 8, 7);
  private static Graph graph;
  private static Map<FeedScopedId, TripPattern> patternIndex;
  private static TripPattern pattern;
  private static Timetable timetable;
  private static String feedId;

  @BeforeAll
  public static void setUp() throws Exception {
    graph = ConstantsForTests.buildGtfsGraph(ConstantsForTests.FAKE_GTFS);

    feedId = graph.getFeedIds().stream().findFirst().get();
    patternIndex = new HashMap<>();

    for (TripPattern pattern : graph.tripPatternForId.values()) {
      pattern.scheduledTripsAsStream().forEach(trip -> patternIndex.put(trip.getId(), pattern));
    }

    pattern = patternIndex.get(new FeedScopedId(feedId, "1.1"));
    timetable = pattern.getScheduledTimetable();
  }

  @Test
  public void testUpdate() {
    TripUpdate tripUpdate;
    TripUpdate.Builder tripUpdateBuilder;
    TripDescriptor.Builder tripDescriptorBuilder;
    StopTimeUpdate.Builder stopTimeUpdateBuilder;
    StopTimeEvent.Builder stopTimeEventBuilder;

    int trip_1_1_index = timetable.getTripIndex(new FeedScopedId(feedId, "1.1"));

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
    var patch = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate);
    assertNull(patch);

    // update trip with bad data
    tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);
    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(0);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SKIPPED);
    tripUpdate = tripUpdateBuilder.build();
    patch = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate);
    assertNull(patch);

    // update trip with non-increasing data
    tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);
    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(2);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setTime(
      TestUtils.dateInSeconds("America/New_York", 2009, AUGUST, 7, 0, 10, 1)
    );
    stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
    stopTimeEventBuilder.setTime(
      TestUtils.dateInSeconds("America/New_York", 2009, AUGUST, 7, 0, 10, 0)
    );
    tripUpdate = tripUpdateBuilder.build();
    patch = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate);
    assertNull(patch);

    //---
    long startTime = TestUtils.dateInSeconds("America/New_York", 2009, AUGUST, 7, 0, 0, 0);
    options.setDateTime(Instant.ofEpochSecond(startTime));

    // update trip
    tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);
    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(1);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setTime(
      TestUtils.dateInSeconds("America/New_York", 2009, AUGUST, 7, 0, 2, 0)
    );
    stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
    stopTimeEventBuilder.setTime(
      TestUtils.dateInSeconds("America/New_York", 2009, AUGUST, 7, 0, 2, 0)
    );
    tripUpdate = tripUpdateBuilder.build();
    assertEquals(20 * 60, timetable.getTripTimes(trip_1_1_index).getArrivalTime(2));
    patch = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate);
    var updatedTripTimes = patch.getTripTimes();
    assertNotNull(updatedTripTimes);
    timetable.setTripTimes(trip_1_1_index, updatedTripTimes);
    assertEquals(20 * 60 + 120, timetable.getTripTimes(trip_1_1_index).getArrivalTime(2));

    // update trip arrival time incorrectly
    tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);
    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(1);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(0);
    tripUpdate = tripUpdateBuilder.build();
    patch = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate);
    updatedTripTimes = patch.getTripTimes();
    assertNotNull(updatedTripTimes);
    timetable.setTripTimes(trip_1_1_index, updatedTripTimes);

    // update trip arrival time only
    tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);
    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(2);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(1);
    tripUpdate = tripUpdateBuilder.build();
    patch = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate);
    updatedTripTimes = patch.getTripTimes();
    assertNotNull(updatedTripTimes);
    timetable.setTripTimes(trip_1_1_index, updatedTripTimes);

    // update trip departure time only
    tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);
    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(2);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
    stopTimeEventBuilder.setDelay(120);
    tripUpdate = tripUpdateBuilder.build();
    patch = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate);
    updatedTripTimes = patch.getTripTimes();
    assertNotNull(updatedTripTimes);
    timetable.setTripTimes(trip_1_1_index, updatedTripTimes);

    // update trip using stop id
    tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);
    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopId("B");
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
    stopTimeEventBuilder.setDelay(120);
    tripUpdate = tripUpdateBuilder.build();
    patch = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate);
    updatedTripTimes = patch.getTripTimes();
    assertNotNull(updatedTripTimes);
    timetable.setTripTimes(trip_1_1_index, updatedTripTimes);

    // update trip arrival time at first stop and make departure time incoherent at second stop
    tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);
    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(1);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(0);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(1);
    stopTimeUpdateBuilder.setStopSequence(2);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
    stopTimeEventBuilder.setDelay(-1);
    tripUpdate = tripUpdateBuilder.build();
    patch = timetable.createUpdatedTripTimes(tripUpdate, timeZone, serviceDate);
    assertNull(patch);
  }
}
