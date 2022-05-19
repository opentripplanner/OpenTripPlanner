package org.opentripplanner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.opentripplanner.routing.trippattern.RealTimeState;
import org.opentripplanner.transit.model.basic.FeedScopedId;
import org.opentripplanner.updater.stoptime.BackwardsDelayPropagationType;
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
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);
    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(0);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.NO_DATA);
    tripUpdate = tripUpdateBuilder.build();
    var patch = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
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
    patch =
      timetable.createUpdatedTripTimes(
        tripUpdate,
        timeZone,
        serviceDate,
        BackwardsDelayPropagationType.REQUIRED_NO_DATA
      );
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
    patch =
      timetable.createUpdatedTripTimes(
        tripUpdate,
        timeZone,
        serviceDate,
        BackwardsDelayPropagationType.REQUIRED_NO_DATA
      );
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
    patch =
      timetable.createUpdatedTripTimes(
        tripUpdate,
        timeZone,
        serviceDate,
        BackwardsDelayPropagationType.REQUIRED_NO_DATA
      );
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
    patch =
      timetable.createUpdatedTripTimes(
        tripUpdate,
        timeZone,
        serviceDate,
        BackwardsDelayPropagationType.REQUIRED_NO_DATA
      );
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
    patch =
      timetable.createUpdatedTripTimes(
        tripUpdate,
        timeZone,
        serviceDate,
        BackwardsDelayPropagationType.REQUIRED_NO_DATA
      );
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
    patch =
      timetable.createUpdatedTripTimes(
        tripUpdate,
        timeZone,
        serviceDate,
        BackwardsDelayPropagationType.REQUIRED_NO_DATA
      );
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
    patch =
      timetable.createUpdatedTripTimes(
        tripUpdate,
        timeZone,
        serviceDate,
        BackwardsDelayPropagationType.REQUIRED_NO_DATA
      );
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
    patch =
      timetable.createUpdatedTripTimes(
        tripUpdate,
        timeZone,
        serviceDate,
        BackwardsDelayPropagationType.REQUIRED_NO_DATA
      );
    assertNull(patch);
  }

  @Test
  public void testUpdateWithNoData() {
    TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);
    TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(1);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.NO_DATA);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(1);
    stopTimeUpdateBuilder.setStopSequence(2);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SKIPPED);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(2);
    stopTimeUpdateBuilder.setStopSequence(3);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.NO_DATA);
    TripUpdate tripUpdate = tripUpdateBuilder.build();
    var patch = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
    var updatedTripTimes = patch.getTripTimes();
    assertNotNull(updatedTripTimes);
    assertEquals(RealTimeState.UPDATED, updatedTripTimes.getRealTimeState());
    assertTrue(updatedTripTimes.isNoDataStop(0));
    assertFalse(updatedTripTimes.isNoDataStop(1));
    assertTrue(updatedTripTimes.isCancelledStop(1));
    assertFalse(updatedTripTimes.isCancelledStop(2));
    assertTrue(updatedTripTimes.isNoDataStop(2));
    var skippedStops = patch.getSkippedStopIndices();
    assertEquals(1, skippedStops.size());
    assertEquals(1, skippedStops.get(0));
  }

  @Test
  public void testUpdateWithAlwaysDelayPropagationFromSecondStop() {
    TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);
    TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(2);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    StopTimeEvent.Builder stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(10);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
    stopTimeEventBuilder.setDelay(10);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(1);
    stopTimeUpdateBuilder.setStopSequence(3);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(15);
    TripUpdate tripUpdate = tripUpdateBuilder.build();
    var patch = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.ALWAYS
    );
    var updatedTripTimes = patch.getTripTimes();
    assertNotNull(updatedTripTimes);
    assertEquals(10, updatedTripTimes.getArrivalDelay(0));
    assertEquals(10, updatedTripTimes.getDepartureDelay(0));
    assertEquals(10, updatedTripTimes.getArrivalDelay(1));
    assertEquals(10, updatedTripTimes.getDepartureDelay(1));
    assertEquals(15, updatedTripTimes.getArrivalDelay(2));
    assertEquals(15, updatedTripTimes.getDepartureDelay(2));

    // ALWAYS propagation type shouldn't set NO_DATA flags
    assertFalse(updatedTripTimes.isNoDataStop(0));
    assertFalse(updatedTripTimes.isNoDataStop(1));
    assertFalse(updatedTripTimes.isNoDataStop(2));
  }

  @Test
  public void testUpdateWithAlwaysDelayPropagationFromThirdStop() {
    TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);
    TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(3);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    StopTimeEvent.Builder stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(15);
    TripUpdate tripUpdate = tripUpdateBuilder.build();
    var patch = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.ALWAYS
    );
    var updatedTripTimes = patch.getTripTimes();
    assertNotNull(updatedTripTimes);
    assertEquals(15, updatedTripTimes.getArrivalDelay(0));
    assertEquals(15, updatedTripTimes.getDepartureDelay(0));
    assertEquals(15, updatedTripTimes.getArrivalDelay(1));
    assertEquals(15, updatedTripTimes.getDepartureDelay(1));
    assertEquals(15, updatedTripTimes.getArrivalDelay(2));
    assertEquals(15, updatedTripTimes.getDepartureDelay(2));
  }

  @Test
  public void testUpdateWithRequiredNoDataDelayPropagationWhenItsNotRequired() {
    TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);
    TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(3);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    StopTimeEvent.Builder stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(-100);
    TripUpdate tripUpdate = tripUpdateBuilder.build();
    var timetable = this.timetable;
    var patch = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
    var updatedTripTimes = patch.getTripTimes();
    assertNotNull(updatedTripTimes);
    assertEquals(0, updatedTripTimes.getArrivalDelay(0));
    assertEquals(0, updatedTripTimes.getDepartureDelay(0));
    assertEquals(0, updatedTripTimes.getArrivalDelay(1));
    assertEquals(0, updatedTripTimes.getDepartureDelay(1));
    assertEquals(-100, updatedTripTimes.getArrivalDelay(2));
    assertEquals(-100, updatedTripTimes.getDepartureDelay(2));
    assertTrue(updatedTripTimes.getDepartureTime(1) < updatedTripTimes.getArrivalTime(2));

    // REQUIRED_NO_DATA propagation type should always set NO_DATA flags'
    // on stops at the beginning with no estimates
    assertTrue(updatedTripTimes.isNoDataStop(0));
    assertTrue(updatedTripTimes.isNoDataStop(1));
    assertFalse(updatedTripTimes.isNoDataStop(2));
  }

  @Test
  public void testUpdateWithRequiredNoDataDelayPropagationWhenItsRequired() {
    TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);
    TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(3);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    StopTimeEvent.Builder stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(-700);
    TripUpdate tripUpdate = tripUpdateBuilder.build();
    var timetable = this.timetable;
    var patch = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
    var updatedTripTimes = patch.getTripTimes();
    assertNotNull(updatedTripTimes);
    assertEquals(-700, updatedTripTimes.getArrivalDelay(0));
    assertEquals(-700, updatedTripTimes.getDepartureDelay(0));
    assertEquals(-700, updatedTripTimes.getArrivalDelay(1));
    assertEquals(-700, updatedTripTimes.getDepartureDelay(1));
    assertEquals(-700, updatedTripTimes.getArrivalDelay(2));
    assertEquals(-700, updatedTripTimes.getDepartureDelay(2));
    assertTrue(updatedTripTimes.getDepartureTime(1) < updatedTripTimes.getArrivalTime(2));

    // REQUIRED_NO_DATA propagation type should always set NO_DATA flags'
    // on stops at the beginning with no estimates
    assertTrue(updatedTripTimes.isNoDataStop(0));
    assertTrue(updatedTripTimes.isNoDataStop(1));
    assertFalse(updatedTripTimes.isNoDataStop(2));
  }

  @Test
  public void testUpdateWithRequiredNoDataDelayPropagationOnArrivalTime() {
    TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);
    TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(2);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    StopTimeEvent.Builder stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
    stopTimeEventBuilder.setDelay(-700);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(1);
    stopTimeUpdateBuilder.setStopSequence(3);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(15);
    TripUpdate tripUpdate = tripUpdateBuilder.build();
    var timetable = this.timetable;
    var patch = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
    var updatedTripTimes = patch.getTripTimes();
    assertNotNull(updatedTripTimes);
    assertEquals(-700, updatedTripTimes.getArrivalDelay(0));
    assertEquals(-700, updatedTripTimes.getDepartureDelay(0));
    assertEquals(-700, updatedTripTimes.getArrivalDelay(1));
    assertEquals(-700, updatedTripTimes.getDepartureDelay(1));
    assertEquals(15, updatedTripTimes.getArrivalDelay(2));
    assertEquals(15, updatedTripTimes.getDepartureDelay(2));
    assertTrue(updatedTripTimes.getDepartureTime(1) < updatedTripTimes.getArrivalTime(2));

    // REQUIRED_NO_DATA propagation type should always set NO_DATA flags'
    // on stops at the beginning with no estimates but not on stop that has estimate
    assertTrue(updatedTripTimes.isNoDataStop(0));
    assertFalse(updatedTripTimes.isNoDataStop(1));
    assertFalse(updatedTripTimes.isNoDataStop(2));
  }

  @Test
  public void testUpdateWithRequiredDelayPropagationWhenItsRequired() {
    TripDescriptor.Builder tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setTripId("1.1");
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);
    TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(3);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    StopTimeEvent.Builder stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(-700);
    TripUpdate tripUpdate = tripUpdateBuilder.build();
    var timetable = this.timetable;
    var patch = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.REQUIRED
    );
    var updatedTripTimes = patch.getTripTimes();
    assertNotNull(updatedTripTimes);
    assertEquals(-700, updatedTripTimes.getArrivalDelay(0));
    assertEquals(-700, updatedTripTimes.getDepartureDelay(0));
    assertEquals(-700, updatedTripTimes.getArrivalDelay(1));
    assertEquals(-700, updatedTripTimes.getDepartureDelay(1));
    assertEquals(-700, updatedTripTimes.getArrivalDelay(2));
    assertEquals(-700, updatedTripTimes.getDepartureDelay(2));
    assertTrue(updatedTripTimes.getDepartureTime(1) < updatedTripTimes.getArrivalTime(2));

    // REQUIRED propagation type should never set NO_DATA flags'
    // on stops at the beginning with no estimates
    assertFalse(updatedTripTimes.isNoDataStop(0));
    assertFalse(updatedTripTimes.isNoDataStop(1));
    assertFalse(updatedTripTimes.isNoDataStop(2));
  }
}
