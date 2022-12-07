package org.opentripplanner.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.NEGATIVE_DWELL_TIME;
import static org.opentripplanner.model.UpdateError.UpdateErrorType.TRIP_NOT_FOUND_IN_PATTERN;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.service.TransitModel;
import org.opentripplanner.updater.trip.BackwardsDelayPropagationType;

public class TimetableTest {

  private static final ZoneId timeZone = ZoneIds.NEW_YORK;
  private static final LocalDate serviceDate = LocalDate.of(2009, 8, 7);
  private static Map<FeedScopedId, TripPattern> patternIndex;
  private static Timetable timetable;
  private static String feedId;
  private static int trip_1_1_index;

  @BeforeAll
  public static void setUp() throws Exception {
    TestOtpModel model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.FAKE_GTFS);
    TransitModel transitModel = model.transitModel();

    feedId = transitModel.getFeedIds().stream().findFirst().get();
    patternIndex = new HashMap<>();

    for (TripPattern pattern : transitModel.getAllTripPatterns()) {
      pattern.scheduledTripsAsStream().forEach(trip -> patternIndex.put(trip.getId(), pattern));
    }

    TripPattern pattern = patternIndex.get(new FeedScopedId(feedId, "1.1"));
    timetable = pattern.getScheduledTimetable();
    trip_1_1_index = timetable.getTripIndex(new FeedScopedId(feedId, "1.1"));
  }

  @Test
  public void tripNotFoundInPattern() {
    // non-existing trip
    var tripDescriptorBuilder = tripDescriptorBuilder("b");

    TripUpdate.Builder tripUpdateBuilder;
    StopTimeUpdate.Builder stopTimeUpdateBuilder;

    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(0);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.NO_DATA);
    var tripUpdate = tripUpdateBuilder.build();

    Result<TripTimesPatch, UpdateError> result = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
    assertTrue(result.isFailure());

    result.ifFailure(r -> {
      assertEquals(new FeedScopedId(feedId, "b"), r.tripId());
      assertEquals(TRIP_NOT_FOUND_IN_PATTERN, r.errorType());
    });
  }

  @Test
  public void badData() {
    TripUpdate tripUpdate;
    TripUpdate.Builder tripUpdateBuilder;
    StopTimeUpdate.Builder stopTimeUpdateBuilder;

    // update trip with bad data
    var tripDescriptorBuilder = tripDescriptorBuilder("1.1");
    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(0);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SKIPPED);
    tripUpdate = tripUpdateBuilder.build();
    var result = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
    assertTrue(result.isFailure());

    result.ifFailure(e -> assertEquals(INVALID_STOP_SEQUENCE, e.errorType()));
  }

  @Test
  public void nonIncreasingTimes() {
    // update trip with non-increasing data
    var tripDescriptorBuilder = tripDescriptorBuilder("1.1");
    var tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    var stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(2);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    var stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setTime(
      LocalDateTime.of(2009, Month.AUGUST, 7, 0, 10, 1, 0).atZone(ZoneIds.NEW_YORK).toEpochSecond()
    );
    stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
    stopTimeEventBuilder.setTime(
      LocalDateTime
        .of(2009, Month.AUGUST, 7, 0, 10, 0, 0)
        .atZone(ZoneId.of("America/New_York"))
        .toEpochSecond()
    );
    var tripUpdate = tripUpdateBuilder.build();
    var result = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
    assertTrue(result.isFailure());

    result.ifFailure(e -> assertEquals(NEGATIVE_DWELL_TIME, e.errorType()));
  }

  @Test
  public void update() {
    // update trip
    var tripDescriptorBuilder = tripDescriptorBuilder("1.1");

    var tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    var stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(1);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    var stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setTime(
      LocalDateTime.of(2009, Month.AUGUST, 7, 0, 2, 0, 0).atZone(ZoneIds.NEW_YORK).toEpochSecond()
    );
    stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
    stopTimeEventBuilder.setTime(
      LocalDateTime
        .of(2009, Month.AUGUST.getValue() - 1 + 1, 7, 0, 2, 0, 0)
        .atZone(ZoneId.of("America/New_York"))
        .toEpochSecond()
    );
    var tripUpdate = tripUpdateBuilder.build();
    assertEquals(20 * 60, timetable.getTripTimes(trip_1_1_index).getArrivalTime(2));
    var result = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(result.isSuccess());

    result.ifSuccess(p -> {
      var updatedTripTimes = p.getTripTimes();
      assertNotNull(updatedTripTimes);
      timetable.setTripTimes(trip_1_1_index, updatedTripTimes);
      assertEquals(20 * 60 + 120, timetable.getTripTimes(trip_1_1_index).getArrivalTime(2));
    });

    // update trip arrival time incorrectly
    tripDescriptorBuilder = tripDescriptorBuilder("1.1");
    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(1);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(0);
    tripUpdate = tripUpdateBuilder.build();
    result =
      timetable.createUpdatedTripTimes(
        tripUpdate,
        timeZone,
        serviceDate,
        BackwardsDelayPropagationType.REQUIRED_NO_DATA
      );

    assertTrue(result.isSuccess());

    result.ifSuccess(p -> {
      var updatedTripTimes = p.getTripTimes();
      assertNotNull(updatedTripTimes);
      timetable.setTripTimes(trip_1_1_index, updatedTripTimes);
    });

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

    result =
      timetable.createUpdatedTripTimes(
        tripUpdate,
        timeZone,
        serviceDate,
        BackwardsDelayPropagationType.REQUIRED_NO_DATA
      );

    assertTrue(result.isSuccess());

    result.ifSuccess(p -> {
      var updatedTripTimes = p.getTripTimes();
      assertNotNull(updatedTripTimes);
      timetable.setTripTimes(trip_1_1_index, updatedTripTimes);
    });

    // update trip departure time only
    tripDescriptorBuilder = tripDescriptorBuilder();
    tripDescriptorBuilder.setTripId("1.1");
    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(2);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
    stopTimeEventBuilder.setDelay(120);
    tripUpdate = tripUpdateBuilder.build();
    result =
      timetable.createUpdatedTripTimes(
        tripUpdate,
        timeZone,
        serviceDate,
        BackwardsDelayPropagationType.REQUIRED_NO_DATA
      );

    assertTrue(result.isSuccess());

    result.ifSuccess(p -> {
      var updatedTripTimes = p.getTripTimes();
      assertNotNull(updatedTripTimes);
      timetable.setTripTimes(trip_1_1_index, updatedTripTimes);
    });

    // update trip using stop id
    tripDescriptorBuilder = tripDescriptorBuilder("1.1");
    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopId("B");
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
    stopTimeEventBuilder.setDelay(120);
    tripUpdate = tripUpdateBuilder.build();
    result =
      timetable.createUpdatedTripTimes(
        tripUpdate,
        timeZone,
        serviceDate,
        BackwardsDelayPropagationType.REQUIRED_NO_DATA
      );

    assertTrue(result.isSuccess());

    result.ifSuccess(p -> {
      var updatedTripTimes = p.getTripTimes();
      assertNotNull(updatedTripTimes);
      timetable.setTripTimes(trip_1_1_index, updatedTripTimes);
    });
  }

  @Test
  public void fixIncoherentTimes() {
    // update trip arrival time at first stop and make departure time incoherent at second stop
    var tripDescriptorBuilder = tripDescriptorBuilder("1.1");
    var tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    var stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(1);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    var stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(0);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(1);
    stopTimeUpdateBuilder.setStopSequence(2);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
    stopTimeEventBuilder.setDelay(-1);
    var tripUpdate = tripUpdateBuilder.build();

    var result = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
    assertTrue(result.isFailure());
  }

  @Test
  public void testUpdateWithNoData() {
    var tripDescriptorBuilder = tripDescriptorBuilder("1.1");

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
    var result = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(result.isSuccess());

    result.ifSuccess(p -> {
      var updatedTripTimes = p.getTripTimes();
      assertNotNull(updatedTripTimes);
      assertEquals(RealTimeState.UPDATED, updatedTripTimes.getRealTimeState());
      assertTrue(updatedTripTimes.isNoDataStop(0));
      assertFalse(updatedTripTimes.isNoDataStop(1));
      assertTrue(updatedTripTimes.isCancelledStop(1));
      assertFalse(updatedTripTimes.isCancelledStop(2));
      assertTrue(updatedTripTimes.isNoDataStop(2));
      var skippedStops = p.getSkippedStopIndices();
      assertEquals(1, skippedStops.size());
      assertEquals(1, skippedStops.get(0));
    });
  }

  @Test
  public void testUpdateWithAlwaysDelayPropagationFromSecondStop() {
    var tripDescriptorBuilder = tripDescriptorBuilder("1.1");

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
    var result = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.ALWAYS
    );

    assertTrue(result.isSuccess());

    result.ifSuccess(p -> {
      var updatedTripTimes = p.getTripTimes();
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
    });
  }

  @Test
  public void testUpdateWithAlwaysDelayPropagationFromThirdStop() {
    TripDescriptor.Builder tripDescriptorBuilder = tripDescriptorBuilder("1.1");
    TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(3);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    StopTimeEvent.Builder stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(15);
    TripUpdate tripUpdate = tripUpdateBuilder.build();

    var result = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.ALWAYS
    );

    assertTrue(result.isSuccess());

    result.ifSuccess(p -> {
      var updatedTripTimes = p.getTripTimes();
      assertNotNull(updatedTripTimes);
      assertEquals(15, updatedTripTimes.getArrivalDelay(0));
      assertEquals(15, updatedTripTimes.getDepartureDelay(0));
      assertEquals(15, updatedTripTimes.getArrivalDelay(1));
      assertEquals(15, updatedTripTimes.getDepartureDelay(1));
      assertEquals(15, updatedTripTimes.getArrivalDelay(2));
      assertEquals(15, updatedTripTimes.getDepartureDelay(2));
    });
  }

  @Test
  public void testUpdateWithRequiredNoDataDelayPropagationWhenItsNotRequired() {
    TripDescriptor.Builder tripDescriptorBuilder = tripDescriptorBuilder("1.1");
    TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(3);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    StopTimeEvent.Builder stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(-100);
    TripUpdate tripUpdate = tripUpdateBuilder.build();
    var patch = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(patch.isSuccess());

    patch.ifSuccess(p -> {
      var updatedTripTimes = p.getTripTimes();
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
    });
  }

  @Test
  public void testUpdateWithRequiredNoDataDelayPropagationWhenItsRequired() {
    var tripDescriptorBuilder = tripDescriptorBuilder("1.1");
    TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(3);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    StopTimeEvent.Builder stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(-700);
    TripUpdate tripUpdate = tripUpdateBuilder.build();
    var patch = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(patch.isSuccess());

    patch.ifSuccess(p -> {
      var updatedTripTimes = p.getTripTimes();
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
    });
  }

  @Test
  public void testUpdateWithRequiredNoDataDelayPropagationOnArrivalTime() {
    var tripDescriptorBuilder = tripDescriptorBuilder("1.1");
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
    var result = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
    // if arrival time is not defined but departure time is not and the arrival time is greater
    // than to departure time on a stop, we should not try to fix it by default because the spec
    // only allows you to drop all estimates for a stop when it's passed according to schedule
    assertTrue(result.isFailure());

    result.ifFailure(err -> {
      assertEquals(err.errorType(), NEGATIVE_DWELL_TIME);
    });
  }

  @Test
  public void testUpdateWithRequiredDelayPropagationWhenItsRequired() {
    var tripDescriptorBuilder = tripDescriptorBuilder("1.1");
    var tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(3);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    StopTimeEvent.Builder stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(-700);
    TripUpdate tripUpdate = tripUpdateBuilder.build();

    var result = timetable.createUpdatedTripTimes(
      tripUpdate,
      timeZone,
      serviceDate,
      BackwardsDelayPropagationType.REQUIRED
    );

    assertTrue(result.isSuccess());

    result.ifSuccess(p -> {
      var updatedTripTimes = p.getTripTimes();
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
    });
  }

  private static TripDescriptor.Builder tripDescriptorBuilder() {
    TripDescriptor.Builder tripDescriptorBuilder;
    tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setScheduleRelationship(TripDescriptor.ScheduleRelationship.SCHEDULED);
    return tripDescriptorBuilder;
  }

  private static TripDescriptor.Builder tripDescriptorBuilder(String tripId) {
    var tripDescriptorBuilder = tripDescriptorBuilder();
    tripDescriptorBuilder.setTripId(tripId);
    return tripDescriptorBuilder;
  }
}
