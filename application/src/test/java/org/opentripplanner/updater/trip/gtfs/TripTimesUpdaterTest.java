package org.opentripplanner.updater.trip.gtfs;

import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.SCHEDULED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_ARRIVAL_TIME;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_DEPARTURE_TIME;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NEGATIVE_DWELL_TIME;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND_IN_PATTERN;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import java.time.LocalDate;
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
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.TripUpdateBuilder;
import org.opentripplanner.updater.trip.gtfs.model.TripUpdate;
import org.opentripplanner.utils.time.TimeUtils;

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
    var nonExistingTripId = "b";
    var tripUpdate = new TripUpdateBuilder(nonExistingTripId, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addNoDataStop(0)
      .build();

    var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
    assertTrue(result.isFailure());

    result.ifFailure(r -> {
      assertEquals(new FeedScopedId(feedId, nonExistingTripId), r.tripId());
      assertEquals(TRIP_NOT_FOUND_IN_PATTERN, r.errorType());
    });
  }

  @Test
  public void badData() {
    // update trip with bad data
    var tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addSkippedStop(0)
      .build();

    var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
    assertTrue(result.isFailure());

    result.ifFailure(e -> assertEquals(INVALID_STOP_SEQUENCE, e.errorType()));
  }

  @Test
  public void nonIncreasingTimes() {
    // update trip with non-increasing data
    var tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addStopTimeWithArrivalAndDeparture(2, "00:10:01", "00:10:00")
      .build();

    var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
    assertTrue(result.isFailure());

    result.ifFailure(e -> assertEquals(NEGATIVE_DWELL_TIME, e.errorType()));
  }

  @Test
  public void update() {
    var timetable = TripTimesUpdaterTest.timetable;
    assertTimetable(
      timetable.getTripTimes(tripId),
      "00:00:00",
      "00:00:00",
      "00:10:00",
      "00:10:00",
      "00:20:00",
      "00:20:00"
    );

    // update trip
    var tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addStopTime(1, "00:02:00")
      .build();

    var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(result.isSuccess());
    var p = result.successValue();

    var updatedTripTimes = p.tripTimes();
    assertNotNull(updatedTripTimes);
    timetable = timetable.copyOf().addOrUpdateTripTimes(updatedTripTimes).build();
    assertTimetable(
      timetable.getTripTimes(tripId),
      "00:02:00",
      "00:02:00",
      "00:12:00",
      "00:12:00",
      "00:22:00",
      "00:22:00"
    );

    // update trip arrival time incorrectly
    tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopSequence(1)
          .setArrival(StopTimeEvent.newBuilder().setDelay(0).build())
          .build()
      )
      .build();

    result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(result.isSuccess());

    p = result.successValue();
    updatedTripTimes = p.tripTimes();
    assertNotNull(updatedTripTimes);
    timetable = timetable.copyOf().addOrUpdateTripTimes(updatedTripTimes).build();
    assertTimetable(
      timetable.getTripTimes(tripId),
      "00:00:00",
      "00:00:00",
      "00:10:00",
      "00:10:00",
      "00:20:00",
      "00:20:00"
    );

    // update trip arrival time only
    tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopSequence(2)
          .setArrival(StopTimeEvent.newBuilder().setDelay(1).build())
          .build()
      )
      .build();

    result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(result.isSuccess());

    p = result.successValue();
    updatedTripTimes = p.tripTimes();
    assertNotNull(updatedTripTimes);
    timetable = timetable.copyOf().addOrUpdateTripTimes(updatedTripTimes).build();
    assertTimetable(
      timetable.getTripTimes(tripId),
      "00:00:00",
      "00:00:00",
      "00:10:01",
      "00:10:01",
      "00:20:01",
      "00:20:01"
    );

    // update trip departure time only
    tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopSequence(2)
          .setDeparture(StopTimeEvent.newBuilder().setDelay(120).build())
          .build()
      )
      .build();

    result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(result.isSuccess());

    p = result.successValue();
    updatedTripTimes = p.tripTimes();
    assertNotNull(updatedTripTimes);
    timetable = timetable.copyOf().addOrUpdateTripTimes(updatedTripTimes).build();
    assertTimetable(
      timetable.getTripTimes(tripId),
      "00:00:00",
      "00:00:00",
      "00:10:00",
      "00:12:00",
      "00:22:00",
      "00:22:00"
    );

    // update trip using stop id
    tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopId("B")
          .setDeparture(StopTimeEvent.newBuilder().setDelay(180).build())
          .build()
      )
      .build();

    result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(result.isSuccess());

    p = result.successValue();
    updatedTripTimes = p.tripTimes();
    assertNotNull(updatedTripTimes);
    timetable = timetable.copyOf().addOrUpdateTripTimes(updatedTripTimes).build();
    assertTimetable(
      timetable.getTripTimes(tripId),
      "00:00:00",
      "00:00:00",
      "00:10:00",
      "00:13:00",
      "00:23:00",
      "00:23:00"
    );
  }

  @Test
  public void fixIncoherentTimes() {
    // update trip arrival time at first stop and make departure time incoherent at second stop
    var tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopSequence(1)
          .setArrival(StopTimeEvent.newBuilder().setDelay(900).build())
          .build()
      )
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopSequence(2)
          .setDeparture(StopTimeEvent.newBuilder().setDelay(-1).build())
          .build()
      )
      .build();

    var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
    assertTrue(result.isFailure());
  }

  @Test
  public void testUpdateWithNoForwardPropagationWhenItIsRequired() {
    // update trip arrival time at first stop and make departure time incoherent at second stop
    var tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopSequence(1)
          .setArrival(StopTimeEvent.newBuilder().setDelay(15).build())
          .build()
      )
      .build();

    var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.NONE,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(result.isFailure());

    result.ifFailure(p -> assertEquals(INVALID_ARRIVAL_TIME, p.errorType()));
  }

  @Test
  public void testUpdateWithNoForwardPropagationWithCompleteData() {
    var tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addDelayedStopTime(1, 15, 20)
      .addDelayedStopTime(2, 25, 30)
      .addDelayedStopTime(3, 35, 40)
      .build();

    var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.NONE,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(result.isSuccess());

    result.ifSuccess(p -> {
      var tripTimes = p.tripTimes();
      assertEquals(15, tripTimes.getArrivalDelay(0));
      assertEquals(20, tripTimes.getDepartureDelay(0));
      assertEquals(25, tripTimes.getArrivalDelay(1));
      assertEquals(30, tripTimes.getDepartureDelay(1));
      assertEquals(35, tripTimes.getArrivalDelay(2));
      assertEquals(40, tripTimes.getDepartureDelay(2));
    });
  }

  @Test
  public void testUpdateWithNoData() {
    var tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addNoDataStop(1)
      .addSkippedStop(2)
      .addNoDataStop(3)
      .build();

    var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(result.isSuccess());

    result.ifSuccess(p -> {
      var updatedTripTimes = p.tripTimes();
      assertNotNull(updatedTripTimes);
      assertEquals(RealTimeState.UPDATED, updatedTripTimes.getRealTimeState());
      assertTrue(updatedTripTimes.isNoDataStop(0));
      assertFalse(updatedTripTimes.isNoDataStop(1));
      assertTrue(updatedTripTimes.isCancelledStop(1));
      assertFalse(updatedTripTimes.isCancelledStop(2));
      assertTrue(updatedTripTimes.isNoDataStop(2));
      var updatedPickup = p.updatedPickup();
      var updatedDropoff = p.updatedDropoff();
      assertIterableEquals(Map.of(1, PickDrop.CANCELLED).entrySet(), updatedPickup.entrySet());
      assertIterableEquals(Map.of(1, PickDrop.CANCELLED).entrySet(), updatedDropoff.entrySet());
    });
  }

  @Test
  public void testUpdateWithUnchangedTripAndStopProperties() {
    var tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE, "foo", null)
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopSequence(1)
          .setArrival(StopTimeEvent.newBuilder().setDelay(0).build())
          .setDeparture(StopTimeEvent.newBuilder().setDelay(0).build())
          .setStopTimeProperties(
            StopTimeUpdate.StopTimeProperties.newBuilder()
              .setStopHeadsign("foo")
              .setAssignedStopId("A")
              .build()
          )
          .build()
      )
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopSequence(2)
          .setStopTimeProperties(
            StopTimeUpdate.StopTimeProperties.newBuilder()
              .setDropOffType(StopTimeUpdate.StopTimeProperties.DropOffPickupType.REGULAR)
              .setPickupType(StopTimeUpdate.StopTimeProperties.DropOffPickupType.REGULAR)
              .build()
          )
          .build()
      )
      .addDelayedStopTime(3, 0)
      .build();

    var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(result.isSuccess());

    result.ifSuccess(p -> {
      assertTrue(p.updatedDropoff().isEmpty(), "dropoffs are not modified");
      assertTrue(p.updatedPickup().isEmpty(), "pickups are not modified");
      assertTrue(p.replacedStopIndices().isEmpty(), "stop indices are not modified");
      assertEquals(
        "foo",
        p.tripTimes().getHeadsign(0).toString(),
        "headsigns [1] are not modified"
      );
      assertEquals(
        "foo",
        p.tripTimes().getHeadsign(1).toString(),
        "headsigns [2] are not modified"
      );
      assertEquals(
        "foo",
        p.tripTimes().getHeadsign(2).toString(),
        "headsigns [3] are not modified"
      );
    });
  }

  @Test
  public void testUpdateWithTripAndStopProperties() {
    var tripUpdate = new TripUpdateBuilder(
      TRIP_ID,
      SERVICE_DATE,
      SCHEDULED,
      TIME_ZONE,
      "new trip headsign",
      null
    )
      .addDelayedStopTime(1, 0, "new stop headsign")
      .addSkippedStop(2)
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopSequence(3)
          .setArrival(StopTimeEvent.newBuilder().setDelay(0).build())
          .setDeparture(StopTimeEvent.newBuilder().setDelay(0).build())
          .setStopTimeProperties(
            StopTimeUpdate.StopTimeProperties.newBuilder()
              .setPickupType(StopTimeUpdate.StopTimeProperties.DropOffPickupType.NONE)
              .setDropOffType(
                StopTimeUpdate.StopTimeProperties.DropOffPickupType.COORDINATE_WITH_DRIVER
              )
              .build()
          )
          .build()
      )
      .build();

    var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(result.isSuccess());

    result.ifSuccess(p -> {
      var updatedTripTimes = p.tripTimes();
      assertNotNull(updatedTripTimes);
      assertEquals(RealTimeState.UPDATED, updatedTripTimes.getRealTimeState());
      assertFalse(updatedTripTimes.isCancelledStop(0));
      assertTrue(updatedTripTimes.isCancelledStop(1));
      assertFalse(updatedTripTimes.isCancelledStop(2));
      assertEquals(I18NString.of("new stop headsign"), updatedTripTimes.getHeadsign(0));
      assertEquals(I18NString.of("new trip headsign"), updatedTripTimes.getHeadsign(1));
      assertEquals(I18NString.of("new trip headsign"), updatedTripTimes.getHeadsign(2));
      var updatedPickup = p.updatedPickup();
      var updatedDropoff = p.updatedDropoff();
      assertEquals(Map.of(1, PickDrop.CANCELLED, 2, PickDrop.NONE), updatedPickup);
      assertEquals(
        Map.of(1, PickDrop.CANCELLED, 2, PickDrop.COORDINATE_WITH_DRIVER),
        updatedDropoff
      );
    });
  }

  @Test
  public void testUpdateWithAlwaysDelayPropagationFromSecondStop() {
    var tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addDelayedStopTime(2, 10, 10)
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopSequence(3)
          .setArrival(StopTimeEvent.newBuilder().setDelay(15).build())
          .build()
      )
      .build();

    var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.ALWAYS
    );

    assertTrue(result.isSuccess());

    result.ifSuccess(p -> {
      var updatedTripTimes = p.tripTimes();
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
    var tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopSequence(3)
          .setArrival(StopTimeEvent.newBuilder().setDelay(15).build())
          .build()
      )
      .build();

    var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.ALWAYS
    );

    assertTrue(result.isSuccess());

    result.ifSuccess(p -> {
      var updatedTripTimes = p.tripTimes();
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
  public void testUpdateWithNoBackwardPropagationWhenItIsNotRequired() {
    var tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopSequence(1)
          .setArrival(StopTimeEvent.newBuilder().setDelay(15).build())
          .build()
      )
      .build();

    var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.NONE
    );

    assertTrue(result.isSuccess());

    result.ifSuccess(p -> {
      var updatedTripTimes = p.tripTimes();
      assertNotNull(updatedTripTimes);
      assertEquals(15, updatedTripTimes.getArrivalDelay(0));
      assertFalse(updatedTripTimes.isNoDataStop(0));
    });
  }

  @Test
  public void testUpdateWithNoBackwardPropagationWhenItIsRequired() {
    var tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopSequence(3)
          .setArrival(StopTimeEvent.newBuilder().setDelay(15).build())
          .build()
      )
      .build();

    var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.NONE
    );

    assertTrue(result.isFailure());

    result.ifFailure(p -> {
      assertEquals(UpdateError.UpdateErrorType.INVALID_ARRIVAL_TIME, p.errorType());
    });
  }

  @Test
  public void testUpdateWithRequiredNoDataDelayPropagationWhenItsNotRequired() {
    var tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopSequence(3)
          .setArrival(StopTimeEvent.newBuilder().setDelay(-100).build())
          .build()
      )
      .build();

    var patch = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(patch.isSuccess());

    patch.ifSuccess(p -> {
      var updatedTripTimes = p.tripTimes();
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
    var tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopSequence(3)
          .setArrival(StopTimeEvent.newBuilder().setDelay(-700).build())
          .build()
      )
      .build();

    var patch = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(patch.isSuccess());

    patch.ifSuccess(p -> {
      var updatedTripTimes = p.tripTimes();
      assertNotNull(updatedTripTimes);
      assertEquals(0, updatedTripTimes.getArrivalDelay(0));
      assertEquals(0, updatedTripTimes.getDepartureDelay(0));
      assertEquals(-100, updatedTripTimes.getArrivalDelay(1));
      assertEquals(-100, updatedTripTimes.getDepartureDelay(1));
      assertEquals(-700, updatedTripTimes.getArrivalDelay(2));
      assertEquals(-700, updatedTripTimes.getDepartureDelay(2));
      assertTrue(updatedTripTimes.getDepartureTime(1) <= updatedTripTimes.getArrivalTime(2));

      // REQUIRED_NO_DATA propagation type should always set NO_DATA flags'
      // on stops at the beginning with no estimates
      assertTrue(updatedTripTimes.isNoDataStop(0));
      assertTrue(updatedTripTimes.isNoDataStop(1));
      assertFalse(updatedTripTimes.isNoDataStop(2));
    });
  }

  @Test
  public void testUpdateWithRequiredNoDataDelayPropagationOnArrivalTime() {
    var tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopSequence(2)
          .setDeparture(StopTimeEvent.newBuilder().setDelay(-700).build())
          .build()
      )
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopSequence(3)
          .setArrival(StopTimeEvent.newBuilder().setDelay(15).build())
          .build()
      )
      .build();

    var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );
    // the spec does not require you to supply both arrival and departure on a StopTimeUpdate
    // it says:
    // either arrival or departure must be provided within a StopTimeUpdate - both fields cannot be
    // empty
    // therefore the processing should succeed even if only one of them is given
    assertTrue(result.isSuccess());
    result.ifSuccess(p -> {
      var updatedTripTimes = p.tripTimes();
      assertNotNull(updatedTripTimes);
      assertEquals(15, updatedTripTimes.getArrivalDelay(2));
      assertEquals(15, updatedTripTimes.getDepartureDelay(2));
    });
  }

  @Test
  public void testUpdateWithRequiredDelayPropagationWhenItsRequired() {
    var tripUpdate = new TripUpdateBuilder(TRIP_ID, SERVICE_DATE, SCHEDULED, TIME_ZONE)
      .addRawStopTime(
        StopTimeUpdate.newBuilder()
          .setStopSequence(3)
          .setArrival(StopTimeEvent.newBuilder().setDelay(-700).build())
          .build()
      )
      .build();

    var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED
    );

    assertTrue(result.isSuccess());

    result.ifSuccess(p -> {
      var updatedTripTimes = p.tripTimes();
      assertNotNull(updatedTripTimes);
      assertEquals(0, updatedTripTimes.getArrivalDelay(0));
      assertEquals(0, updatedTripTimes.getDepartureDelay(0));
      assertEquals(-100, updatedTripTimes.getArrivalDelay(1));
      assertEquals(-100, updatedTripTimes.getDepartureDelay(1));
      assertEquals(-700, updatedTripTimes.getArrivalDelay(2));
      assertEquals(-700, updatedTripTimes.getDepartureDelay(2));
      assertTrue(updatedTripTimes.getDepartureTime(1) <= updatedTripTimes.getArrivalTime(2));

      // REQUIRED propagation type should never set NO_DATA flags'
      // on stops at the beginning with no estimates
      assertFalse(updatedTripTimes.isNoDataStop(0));
      assertFalse(updatedTripTimes.isNoDataStop(1));
      assertFalse(updatedTripTimes.isNoDataStop(2));
    });
  }

  /**
   * Test GtfsRealtime.TripUpdate with 1 stop cancellation and the regular stop times after the cancelled stop is
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

    GtfsRealtime.TripUpdate tripUpdate = tripUpdateBuilder.build();

    var patch = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(patch.isSuccess());

    patch.ifSuccess(p -> {
      var updatedTripTimes = p.tripTimes();
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
   * Test GtfsRealtime.TripUpdate with 1 stop cancellation and the regular stop times before the cancelled stop is
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

    GtfsRealtime.TripUpdate tripUpdate = tripUpdateBuilder.build();

    var patch = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(patch.isSuccess());

    patch.ifSuccess(p -> {
      var updatedTripTimes = p.tripTimes();
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
   * Test GtfsRealtime.TripUpdate with 1 stop cancellation and the regular stop times are non-increasing.
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

    GtfsRealtime.TripUpdate tripUpdate = tripUpdateBuilder.build();

    var patch = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(patch.isFailure());

    patch.ifFailure(p -> {
      assertEquals(UpdateError.UpdateErrorType.NEGATIVE_HOP_TIME, p.errorType());
    });
  }

  /**
   * Test GtfsRealtime.TripUpdate with 1 stop cancellation at the beginning of the route and running early.
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

    GtfsRealtime.TripUpdate tripUpdate = tripUpdateBuilder.build();

    var patch = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(patch.isSuccess());

    patch.ifSuccess(p -> {
      var updatedTripTimes = p.tripTimes();
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
   * Test GtfsRealtime.TripUpdate with 1 stop cancellation at the end of the route and running late.
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

    GtfsRealtime.TripUpdate tripUpdate = tripUpdateBuilder.build();

    var patch = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      timetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(patch.isSuccess());

    patch.ifSuccess(p -> {
      var updatedTripTimes = p.tripTimes();
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
   * Test GtfsRealtime.TripUpdate with multiple stop cancelled together.
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

    GtfsRealtime.TripUpdate tripUpdate = tripUpdateBuilder.build();

    var scheduledTimetable = patternIndex
      .get(new FeedScopedId(feedId, TRIP_ID_WITH_MORE_STOPS))
      .getScheduledTimetable();
    var patch = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      scheduledTimetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(patch.isSuccess());

    patch.ifSuccess(p -> {
      var updatedTripTimes = p.tripTimes();
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
   * Test GtfsRealtime.TripUpdate with multiple separate stop cancellations.
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

    GtfsRealtime.TripUpdate tripUpdate = tripUpdateBuilder.build();

    var scheduledTimetable = patternIndex
      .get(new FeedScopedId(feedId, TRIP_ID_WITH_MORE_STOPS))
      .getScheduledTimetable();

    var patch = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
      scheduledTimetable,
      new TripUpdate(tripUpdate),
      TIME_ZONE,
      SERVICE_DATE,
      ForwardsDelayPropagationType.DEFAULT,
      BackwardsDelayPropagationType.REQUIRED_NO_DATA
    );

    assertTrue(patch.isSuccess());

    patch.ifSuccess(p -> {
      var updatedTripTimes = p.tripTimes();
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
      GtfsRealtime.TripUpdate tripUpdate = builder.build();

      var result = TripTimesUpdater.createUpdatedTripTimesFromGtfsRt(
        timetable,
        new TripUpdate(tripUpdate),
        TIME_ZONE,
        SERVICE_DATE,
        ForwardsDelayPropagationType.DEFAULT,
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

  private void assertTimetable(TripTimes tripTimes, String... expectedTimes) {
    var actualTimes = new String[tripTimes.getNumStops() * 2];
    for (int i = 0; i < tripTimes.getNumStops(); i++) {
      actualTimes[i * 2] = TimeUtils.timeToStrLong(tripTimes.getArrivalTime(i));
      actualTimes[i * 2 + 1] = TimeUtils.timeToStrLong(tripTimes.getDepartureTime(i));
    }

    assertEquals(String.join(" ", expectedTimes), String.join(" ", actualTimes));
  }
}
