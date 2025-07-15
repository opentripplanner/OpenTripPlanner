package org.opentripplanner.updater.trip.gtfs;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_ARRIVAL_TIME;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_DEPARTURE_TIME;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NEGATIVE_DWELL_TIME;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND_IN_PATTERN;

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
import java.util.function.BiConsumer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ConstantsForTests;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.model.TripTimesPatch;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.TripUpdateBuilder;

public class TripTimesUpdaterTest {

  private static final ZoneId TIME_ZONE = ZoneIds.NEW_YORK;
  private static final LocalDate SERVICE_DATE = LocalDate.of(2009, 8, 7);
  private static final String TRIP_ID = "1.1";
  private static final String TRIP_ID_WITH_MORE_STOPS = "19.1";
  private static Map<FeedScopedId, TripPattern> patternIndex;
  private static Timetable timetable;
  private static String feedId;
  private static FeedScopedId tripId;

  @BeforeAll
  public static void setUp() throws Exception {
    TestOtpModel model = ConstantsForTests.buildGtfsGraph(ConstantsForTests.SIMPLE_GTFS);
    TimetableRepository timetableRepository = model.timetableRepository();

    feedId = timetableRepository.getFeedIds().stream().findFirst().get();
    patternIndex = new HashMap<>();

    for (TripPattern pattern : timetableRepository.getAllTripPatterns()) {
      pattern.scheduledTripsAsStream().forEach(trip -> patternIndex.put(trip.getId(), pattern));
    }

    tripId = new FeedScopedId(feedId, TRIP_ID);
    TripPattern pattern = patternIndex.get(tripId);
    timetable = pattern.getScheduledTimetable();
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

    Result<TripTimesPatch, UpdateError> result = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
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
    var tripDescriptorBuilder = tripDescriptorBuilder(TRIP_ID);
    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(0);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SKIPPED);
    tripUpdate = tripUpdateBuilder.build();
    var result = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
    assertTrue(result.isFailure());

    result.ifFailure(e -> assertEquals(INVALID_STOP_SEQUENCE, e.errorType()));
  }

  @Test
  public void nonIncreasingTimes() {
    // update trip with non-increasing data
    var tripDescriptorBuilder = tripDescriptorBuilder(TRIP_ID);
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
      LocalDateTime.of(2009, Month.AUGUST, 7, 0, 10, 0, 0).atZone(TIME_ZONE).toEpochSecond()
    );
    var tripUpdate = tripUpdateBuilder.build();
    var result = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
    assertTrue(result.isFailure());

    result.ifFailure(e -> assertEquals(NEGATIVE_DWELL_TIME, e.errorType()));
  }

  @Test
  public void update() {
    // update trip
    var tripDescriptorBuilder = tripDescriptorBuilder(TRIP_ID);

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
      LocalDateTime.of(2009, Month.AUGUST.getValue() - 1 + 1, 7, 0, 2, 0, 0)
        .atZone(ZoneId.of("America/New_York"))
        .toEpochSecond()
    );
    var tripUpdate = tripUpdateBuilder.build();
    var timetable = TripTimesUpdaterTest.timetable;
    assertEquals(20 * 60, timetable.getTripTimes(tripId).getArrivalTime(2));
    var result = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(result.isSuccess());
    var p = result.successValue();

    var updatedTripTimes = p.getTripTimes();
    assertNotNull(updatedTripTimes);
    timetable = timetable.copyOf().addOrUpdateTripTimes(updatedTripTimes).build();
    assertEquals(20 * 60 + 120, timetable.getTripTimes(tripId).getArrivalTime(2));

    // update trip arrival time incorrectly
    tripDescriptorBuilder = tripDescriptorBuilder(TRIP_ID);
    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(1);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(0);
    tripUpdate = tripUpdateBuilder.build();
    result = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(result.isSuccess());

    p = result.successValue();
    updatedTripTimes = p.getTripTimes();
    assertNotNull(updatedTripTimes);
    timetable = timetable.copyOf().addOrUpdateTripTimes(updatedTripTimes).build();

    // update trip arrival time only
    tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setTripId(TRIP_ID);
    tripDescriptorBuilder.setScheduleRelationship(SCHEDULED);
    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(2);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(1);
    tripUpdate = tripUpdateBuilder.build();

    result = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(result.isSuccess());

    p = result.successValue();
    updatedTripTimes = p.getTripTimes();
    assertNotNull(updatedTripTimes);
    timetable = timetable.copyOf().addOrUpdateTripTimes(updatedTripTimes).build();

    // update trip departure time only
    tripDescriptorBuilder = tripDescriptorBuilder();
    tripDescriptorBuilder.setTripId(TRIP_ID);
    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(2);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
    stopTimeEventBuilder.setDelay(120);
    tripUpdate = tripUpdateBuilder.build();
    result = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(result.isSuccess());

    p = result.successValue();
    updatedTripTimes = p.getTripTimes();
    assertNotNull(updatedTripTimes);
    timetable = timetable.copyOf().addOrUpdateTripTimes(updatedTripTimes).build();

    // update trip using stop id
    tripDescriptorBuilder = tripDescriptorBuilder(TRIP_ID);
    tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopId("B");
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    stopTimeEventBuilder = stopTimeUpdateBuilder.getDepartureBuilder();
    stopTimeEventBuilder.setDelay(120);
    tripUpdate = tripUpdateBuilder.build();
    result = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(result.isSuccess());

    p = result.successValue();
    updatedTripTimes = p.getTripTimes();
    assertNotNull(updatedTripTimes);
    timetable.copyOf().addOrUpdateTripTimes(updatedTripTimes).build();
  }

  @Test
  public void fixIncoherentTimes() {
    // update trip arrival time at first stop and make departure time incoherent at second stop
    var tripDescriptorBuilder = tripDescriptorBuilder(TRIP_ID);
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

    var result = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
    assertTrue(result.isFailure());
  }

  @Test
  public void testUpdateWithNoData() {
    var tripDescriptorBuilder = tripDescriptorBuilder(TRIP_ID);

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
    var result = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
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
    var tripDescriptorBuilder = tripDescriptorBuilder(TRIP_ID);

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
    var result = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
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
    TripDescriptor.Builder tripDescriptorBuilder = tripDescriptorBuilder(TRIP_ID);
    TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(3);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    StopTimeEvent.Builder stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(15);
    TripUpdate tripUpdate = tripUpdateBuilder.build();

    var result = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
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
    TripDescriptor.Builder tripDescriptorBuilder = tripDescriptorBuilder(TRIP_ID);
    TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(3);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    StopTimeEvent.Builder stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(-100);
    TripUpdate tripUpdate = tripUpdateBuilder.build();
    var patch = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
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
    var tripDescriptorBuilder = tripDescriptorBuilder(TRIP_ID);
    TripUpdate.Builder tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(3);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    StopTimeEvent.Builder stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(-700);
    TripUpdate tripUpdate = tripUpdateBuilder.build();
    var patch = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
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
    var tripDescriptorBuilder = tripDescriptorBuilder(TRIP_ID);
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
    var result = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
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
    var tripDescriptorBuilder = tripDescriptorBuilder(TRIP_ID);
    var tripUpdateBuilder = TripUpdate.newBuilder();
    tripUpdateBuilder.setTrip(tripDescriptorBuilder);
    StopTimeUpdate.Builder stopTimeUpdateBuilder = tripUpdateBuilder.addStopTimeUpdateBuilder(0);
    stopTimeUpdateBuilder.setStopSequence(3);
    stopTimeUpdateBuilder.setScheduleRelationship(StopTimeUpdate.ScheduleRelationship.SCHEDULED);
    StopTimeEvent.Builder stopTimeEventBuilder = stopTimeUpdateBuilder.getArrivalBuilder();
    stopTimeEventBuilder.setDelay(-700);
    TripUpdate tripUpdate = tripUpdateBuilder.build();

    var result = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
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

  /**
   * Test TripUpdate with 1 stop cancellation and the regular stop times after the cancelled stop is
   * earlier than the scheduled time at the cancelled stop.
   * Scheduled: 0, 600, 1200
   * Test case: 0, cancelled, 400
   * Expect no errors and applied stop times should be increasing.
   */
  @Test
  public void testUpdateWithCancellationAndEarlierThanCancelled() {
    TripUpdateBuilder tripUpdateBuilder = new TripUpdateBuilder(
      TRIP_ID,
      SERVICE_DATE,
      SCHEDULED,
      TIME_ZONE
    )
      .addDelayedStopTime(1, 0)
      .addSkippedStop(2)
      .addDelayedStopTime(3, -800, -800);

    TripUpdate tripUpdate = tripUpdateBuilder.build();

    var patch = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(patch.isSuccess());

    patch.ifSuccess(p -> {
      var updatedTripTimes = p.getTripTimes();
      assertNotNull(updatedTripTimes);
      assertEquals(0, updatedTripTimes.getArrivalDelay(0));
      assertEquals(0, updatedTripTimes.getDepartureDelay(0));
      assertEquals(-800, updatedTripTimes.getArrivalDelay(2));
      assertEquals(-800, updatedTripTimes.getDepartureDelay(2));
      assertFalse(updatedTripTimes.isCancelledStop(0));
      assertTrue(updatedTripTimes.isCancelledStop(1));
      assertFalse(updatedTripTimes.isCancelledStop(2));
      assertTrue(updatedTripTimes.getDepartureTime(0) <= updatedTripTimes.getArrivalTime(1));
      assertTrue(updatedTripTimes.getDepartureTime(1) <= updatedTripTimes.getArrivalTime(2));
    });
  }

  /**
   * Test TripUpdate with 1 stop cancellation and the regular stop times before the cancelled stop is
   * later than the scheduled time at the cancelled stop.
   * Scheduled: 0, 600, 1200
   * Test case: 1000, cancelled, 1200
   * Expect no errors and applied stop times should be increasing.
   */
  @Test
  public void testUpdateWithCancellationAndLaterThanCancelled() {
    TripUpdateBuilder tripUpdateBuilder = new TripUpdateBuilder(
      TRIP_ID,
      SERVICE_DATE,
      SCHEDULED,
      TIME_ZONE
    )
      .addDelayedStopTime(1, 1000, 1000)
      .addSkippedStop(2)
      .addDelayedStopTime(3, 0, 0);

    TripUpdate tripUpdate = tripUpdateBuilder.build();

    var patch = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(patch.isSuccess());

    patch.ifSuccess(p -> {
      var updatedTripTimes = p.getTripTimes();
      assertNotNull(updatedTripTimes);
      assertEquals(1000, updatedTripTimes.getArrivalDelay(0));
      assertEquals(1000, updatedTripTimes.getDepartureDelay(0));
      assertEquals(0, updatedTripTimes.getArrivalDelay(2));
      assertEquals(0, updatedTripTimes.getDepartureDelay(2));
      assertFalse(updatedTripTimes.isCancelledStop(0));
      assertTrue(updatedTripTimes.isCancelledStop(1));
      assertFalse(updatedTripTimes.isCancelledStop(2));
      assertTrue(updatedTripTimes.getDepartureTime(0) <= updatedTripTimes.getArrivalTime(1));
      assertTrue(updatedTripTimes.getDepartureTime(1) <= updatedTripTimes.getArrivalTime(2));
    });
  }

  /**
   * Test TripUpdate with 1 stop cancellation and the regular stop times are non-increasing.
   * Scheduled: 0, 600, 1200
   * Test case: 1000, cancelled, 800
   * Expect errors, since the stop times are not increasing.
   */
  @Test
  public void testUpdateWithCancellationAndNonIncreasingTimes() {
    TripUpdateBuilder tripUpdateBuilder = new TripUpdateBuilder(
      TRIP_ID,
      SERVICE_DATE,
      SCHEDULED,
      TIME_ZONE
    )
      .addDelayedStopTime(1, 1000, 1000)
      .addSkippedStop(2)
      .addDelayedStopTime(3, -400, -400);

    TripUpdate tripUpdate = tripUpdateBuilder.build();

    var patch = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(patch.isFailure());

    patch.ifFailure(p -> {
      assertEquals(UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME, p.errorType());
    });
  }

  /**
   * Test TripUpdate with 1 stop cancellation at the beginning of the route and running early.
   * Scheduled: 0, 600, 1200
   * Test case: cancelled, -100, 1200
   * Expect no errors and the stop times are increasing.
   */
  @Test
  public void testUpdateWithStartTerminalCancellationAndEarly() {
    TripUpdateBuilder tripUpdateBuilder = new TripUpdateBuilder(
      TRIP_ID,
      SERVICE_DATE,
      SCHEDULED,
      TIME_ZONE
    )
      .addSkippedStop(1)
      .addDelayedStopTime(2, -700, -700)
      .addDelayedStopTime(3, 0, 0);

    TripUpdate tripUpdate = tripUpdateBuilder.build();

    var patch = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(patch.isSuccess());

    patch.ifSuccess(p -> {
      var updatedTripTimes = p.getTripTimes();
      assertNotNull(updatedTripTimes);
      assertEquals(-700, updatedTripTimes.getArrivalDelay(1));
      assertEquals(-700, updatedTripTimes.getDepartureDelay(1));
      assertEquals(0, updatedTripTimes.getArrivalDelay(2));
      assertEquals(0, updatedTripTimes.getDepartureDelay(2));
      assertTrue(updatedTripTimes.isCancelledStop(0));
      assertFalse(updatedTripTimes.isCancelledStop(1));
      assertFalse(updatedTripTimes.isCancelledStop(2));
      assertTrue(updatedTripTimes.getDepartureTime(0) <= updatedTripTimes.getArrivalTime(1));
      assertTrue(updatedTripTimes.getDepartureTime(1) <= updatedTripTimes.getArrivalTime(2));
    });
  }

  /**
   * Test TripUpdate with 1 stop cancellation at the end of the route and running late.
   * Scheduled: 0, 600, 1200
   * Test case: 0, 1300, cancelled
   * Expect no errors and the stop times are increasing.
   */
  @Test
  public void testUpdateWithEndTerminalCancellationAndLate() {
    TripUpdateBuilder tripUpdateBuilder = new TripUpdateBuilder(
      TRIP_ID,
      SERVICE_DATE,
      SCHEDULED,
      TIME_ZONE
    )
      .addDelayedStopTime(1, 0, 0)
      .addDelayedStopTime(2, 700, 700)
      .addSkippedStop(3);

    TripUpdate tripUpdate = tripUpdateBuilder.build();

    var patch = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      timetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(patch.isSuccess());

    patch.ifSuccess(p -> {
      var updatedTripTimes = p.getTripTimes();
      assertNotNull(updatedTripTimes);
      assertEquals(0, updatedTripTimes.getArrivalDelay(0));
      assertEquals(0, updatedTripTimes.getDepartureDelay(0));
      assertEquals(700, updatedTripTimes.getArrivalDelay(1));
      assertEquals(700, updatedTripTimes.getDepartureDelay(1));
      assertFalse(updatedTripTimes.isCancelledStop(0));
      assertFalse(updatedTripTimes.isCancelledStop(1));
      assertTrue(updatedTripTimes.isCancelledStop(2));
      assertTrue(updatedTripTimes.getDepartureTime(0) <= updatedTripTimes.getArrivalTime(1));
      assertTrue(updatedTripTimes.getDepartureTime(1) <= updatedTripTimes.getArrivalTime(2));
    });
  }

  /**
   * Test TripUpdate with multiple stop cancelled together.
   * Scheduled: 0, 0, 600, 1200, 1800, 2400, 3000, 3600
   * Test case: 600, cancelled, cancelled, cancelled, cancelled, cancelled, cancelled, 2400
   * Expect no errors and the stop times are increasing.
   */
  @Test
  public void testUpdateWithMultipleCancellationsTogether() {
    TripUpdateBuilder tripUpdateBuilder = new TripUpdateBuilder(
      TRIP_ID_WITH_MORE_STOPS,
      SERVICE_DATE,
      SCHEDULED,
      TIME_ZONE
    )
      .addDelayedStopTime(1, 600, 600)
      .addSkippedStop(2)
      .addSkippedStop(3)
      .addSkippedStop(4)
      .addSkippedStop(5)
      .addSkippedStop(6)
      .addSkippedStop(7)
      .addDelayedStopTime(8, -1200, -1200);

    TripUpdate tripUpdate = tripUpdateBuilder.build();

    var scheduledTimetable = patternIndex
      .get(new FeedScopedId(feedId, TRIP_ID_WITH_MORE_STOPS))
      .getScheduledTimetable();
    var patch = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      scheduledTimetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(patch.isSuccess());

    patch.ifSuccess(p -> {
      var updatedTripTimes = p.getTripTimes();
      assertNotNull(updatedTripTimes);
      assertEquals(600, updatedTripTimes.getArrivalDelay(0));
      assertEquals(600, updatedTripTimes.getDepartureDelay(0));
      assertEquals(-1200, updatedTripTimes.getArrivalDelay(7));
      assertEquals(-1200, updatedTripTimes.getDepartureDelay(7));
      assertFalse(updatedTripTimes.isCancelledStop(0));
      assertTrue(updatedTripTimes.isCancelledStop(1));
      assertTrue(updatedTripTimes.isCancelledStop(2));
      assertTrue(updatedTripTimes.isCancelledStop(3));
      assertTrue(updatedTripTimes.isCancelledStop(4));
      assertTrue(updatedTripTimes.isCancelledStop(5));
      assertTrue(updatedTripTimes.isCancelledStop(6));
      assertFalse(updatedTripTimes.isCancelledStop(7));
      assertTrue(updatedTripTimes.getDepartureTime(0) <= updatedTripTimes.getArrivalTime(1));
      assertTrue(updatedTripTimes.getDepartureTime(1) <= updatedTripTimes.getArrivalTime(2));
      assertTrue(updatedTripTimes.getDepartureTime(2) <= updatedTripTimes.getArrivalTime(3));
      assertTrue(updatedTripTimes.getDepartureTime(3) <= updatedTripTimes.getArrivalTime(4));
      assertTrue(updatedTripTimes.getDepartureTime(4) <= updatedTripTimes.getArrivalTime(5));
      assertTrue(updatedTripTimes.getDepartureTime(5) <= updatedTripTimes.getArrivalTime(6));
      assertTrue(updatedTripTimes.getDepartureTime(6) <= updatedTripTimes.getArrivalTime(7));
    });
  }

  /**
   * Test TripUpdate with multiple separate stop cancellations.
   * Scheduled: 0, 0, 600, 1200, 1800, 2400, 3000, 3600
   * Test case: 600, cancelled, cancelled, 700, 2300, cancelled, cancelled, 2400
   * Expect no errors and the stop times are increasing.
   */
  @Test
  public void testUpdateWithMultipleSeparateCancellations() {
    TripUpdateBuilder tripUpdateBuilder = new TripUpdateBuilder(
      TRIP_ID_WITH_MORE_STOPS,
      SERVICE_DATE,
      SCHEDULED,
      TIME_ZONE
    )
      .addDelayedStopTime(1, 600, 600)
      .addSkippedStop(2)
      .addSkippedStop(3)
      .addDelayedStopTime(4, -500, -500)
      .addDelayedStopTime(5, 500, 500)
      .addSkippedStop(6)
      .addSkippedStop(7)
      .addDelayedStopTime(8, -1200, -1200);

    TripUpdate tripUpdate = tripUpdateBuilder.build();

    var scheduledTimetable = patternIndex
      .get(new FeedScopedId(feedId, TRIP_ID_WITH_MORE_STOPS))
      .getScheduledTimetable();

    var patch = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
      scheduledTimetable,
      tripUpdate,
      TIME_ZONE,
      SERVICE_DATE,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(patch.isSuccess());

    patch.ifSuccess(p -> {
      var updatedTripTimes = p.getTripTimes();
      assertNotNull(updatedTripTimes);
      assertEquals(600, updatedTripTimes.getArrivalDelay(0));
      assertEquals(600, updatedTripTimes.getDepartureDelay(0));
      assertEquals(-500, updatedTripTimes.getArrivalDelay(3));
      assertEquals(-500, updatedTripTimes.getDepartureDelay(3));
      assertEquals(500, updatedTripTimes.getArrivalDelay(4));
      assertEquals(500, updatedTripTimes.getDepartureDelay(4));
      assertEquals(-1200, updatedTripTimes.getArrivalDelay(7));
      assertEquals(-1200, updatedTripTimes.getDepartureDelay(7));
      assertFalse(updatedTripTimes.isCancelledStop(0));
      assertTrue(updatedTripTimes.isCancelledStop(1));
      assertTrue(updatedTripTimes.isCancelledStop(2));
      assertFalse(updatedTripTimes.isCancelledStop(3));
      assertFalse(updatedTripTimes.isCancelledStop(4));
      assertTrue(updatedTripTimes.isCancelledStop(5));
      assertTrue(updatedTripTimes.isCancelledStop(6));
      assertFalse(updatedTripTimes.isCancelledStop(7));
      assertTrue(updatedTripTimes.getDepartureTime(0) <= updatedTripTimes.getArrivalTime(1));
      assertTrue(updatedTripTimes.getDepartureTime(1) <= updatedTripTimes.getArrivalTime(2));
      assertTrue(updatedTripTimes.getDepartureTime(2) <= updatedTripTimes.getArrivalTime(3));
      assertTrue(updatedTripTimes.getDepartureTime(3) <= updatedTripTimes.getArrivalTime(4));
      assertTrue(updatedTripTimes.getDepartureTime(4) <= updatedTripTimes.getArrivalTime(5));
      assertTrue(updatedTripTimes.getDepartureTime(5) <= updatedTripTimes.getArrivalTime(6));
      assertTrue(updatedTripTimes.getDepartureTime(6) <= updatedTripTimes.getArrivalTime(7));
    });
  }

  @Nested
  class InvalidStopEvent {

    @Test
    public void testInvalidDeparture() {
      testInvalidStopTime(StopTimeUpdate.Builder::setDeparture, INVALID_DEPARTURE_TIME);
    }

    @Test
    public void testInvalidArrival() {
      testInvalidStopTime(StopTimeUpdate.Builder::setArrival, INVALID_ARRIVAL_TIME);
    }

    private static void testInvalidStopTime(
      BiConsumer<StopTimeUpdate.Builder, StopTimeEvent> setEmptyEvent,
      UpdateError.UpdateErrorType expectedError
    ) {
      var builder = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE);
      builder.addRawStopTime(emptyStopTime(1, setEmptyEvent));
      builder.addRawStopTime(emptyStopTime(2, setEmptyEvent));
      TripUpdate tripUpdate = builder.build();

      var result = TripTimesUpdater.createUpdatedTripTimesFromGTFSRT(
        timetable,
        tripUpdate,
        TIME_ZONE,
        SERVICE_DATE,
        BackwardsDelayPropagationType.REQUIRED_NO_DATA
      );

      assertTrue(result.isFailure());

      result.ifFailure(p -> {
        assertEquals(expectedError, p.errorType());
        assertEquals(0, p.stopIndex());
      });
    }

    private static StopTimeUpdate emptyStopTime(
      int sequence,
      BiConsumer<StopTimeUpdate.Builder, StopTimeEvent> setEmptyEvent
    ) {
      var emptyEvent = StopTimeEvent.newBuilder().build();
      var stopTime = StopTimeUpdate.newBuilder();
      stopTime.setStopSequence(sequence);
      setEmptyEvent.accept(stopTime, emptyEvent);
      return stopTime.build();
    }
  }

  private static TripDescriptor.Builder tripDescriptorBuilder() {
    TripDescriptor.Builder tripDescriptorBuilder;
    tripDescriptorBuilder = TripDescriptor.newBuilder();
    tripDescriptorBuilder.setScheduleRelationship(SCHEDULED);
    return tripDescriptorBuilder;
  }

  private static TripDescriptor.Builder tripDescriptorBuilder(String tripId) {
    var tripDescriptorBuilder = tripDescriptorBuilder();
    tripDescriptorBuilder.setTripId(tripId);
    return tripDescriptorBuilder;
  }
}
