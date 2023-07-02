package org.opentripplanner.updater.vehicle_position;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.model.plan.PlanTestConstants.T11_00;
import static org.opentripplanner.transit.model._data.TransitModelForTest.stopTime;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND_IN_PATTERN;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.opentripplanner._support.time.ZoneIds;
import org.opentripplanner.framework.geometry.WgsCoordinate;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.service.vehiclepositions.internal.DefaultVehiclePositionService;
import org.opentripplanner.test.support.VariableSource;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.framework.Deduplicator;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;

public class VehiclePositionsMatcherTest {

  public static final Route ROUTE = TransitModelForTest.route("1").build();
  ZoneId zoneId = ZoneIds.BERLIN;
  String tripId = "trip1";
  FeedScopedId scopedTripId = TransitModelForTest.id(tripId);

  @Test
  public void matchRealtimePositionsToTrip() {
    var pos = vehiclePosition(tripId);
    testVehiclePositions(pos);
  }

  @Test
  @DisplayName("If the vehicle position has no start_date we need to guess the service day")
  public void inferStartDate() {
    var posWithoutServiceDate = VehiclePosition
      .newBuilder()
      .setTrip(TripDescriptor.newBuilder().setTripId(tripId).build())
      .setStopId("stop-1")
      .setPosition(
        GtfsRealtime.Position.newBuilder().setLatitude(1).setLongitude(1).setBearing(30).build()
      )
      .build();
    testVehiclePositions(posWithoutServiceDate);
  }

  @Test
  public void tripNotFoundInPattern() {
    var service = new DefaultVehiclePositionService();

    final String secondTripId = "trip2";

    var trip1 = TransitModelForTest.trip(tripId).build();
    var trip2 = TransitModelForTest.trip(secondTripId).build();

    var stopTimes = TransitModelForTest.stopTimesEvery5Minutes(3, trip1, T11_00);
    var pattern = tripPattern(trip1, stopTimes);

    // Map positions to trips in feed
    VehiclePositionPatternMatcher matcher = new VehiclePositionPatternMatcher(
      TransitModelForTest.FEED_ID,
      ignored -> trip2,
      ignored -> pattern,
      (id, time) -> pattern,
      service,
      zoneId
    );

    var positions = List.of(vehiclePosition(secondTripId));
    var result = matcher.applyVehiclePositionUpdates(positions);

    assertEquals(1, result.failed());
    assertEquals(Set.of(TRIP_NOT_FOUND_IN_PATTERN), result.failures().keySet());
  }

  @Test
  public void sequenceId() {
    var service = new DefaultVehiclePositionService();

    var tripId = "trip1";
    var scopedTripId = TransitModelForTest.id(tripId);
    var trip1 = TransitModelForTest.trip(tripId).build();

    var stopTimes = List.of(stopTime(trip1, 10), stopTime(trip1, 20), stopTime(trip1, 30));
    var pattern1 = tripPattern(trip1, stopTimes);

    var tripForId = Map.of(scopedTripId, trip1);
    var patternForTrip = Map.of(trip1, pattern1);

    // Map positions to trips in feed
    VehiclePositionPatternMatcher matcher = new VehiclePositionPatternMatcher(
      TransitModelForTest.FEED_ID,
      tripForId::get,
      patternForTrip::get,
      (id, time) -> patternForTrip.get(id),
      service,
      zoneId
    );

    var pos = VehiclePosition
      .newBuilder()
      .setTrip(TripDescriptor.newBuilder().setTripId(tripId).build())
      .setCurrentStopSequence(20)
      .build();

    var positions = List.of(pos);

    // Execute the same match-to-pattern step as the runner
    matcher.applyVehiclePositionUpdates(positions);

    // ensure that gtfs-rt was matched to an OTP pattern correctly
    assertEquals(1, service.getVehiclePositions(pattern1).size());
    var nextStop = service.getVehiclePositions(pattern1).get(0).stop().stop();
    assertEquals("F:stop-20", nextStop.getId().toString());
  }

  @Test
  void invalidStopSequence() {
    var posWithInvalidSequence = VehiclePosition
      .newBuilder()
      .setTrip(TripDescriptor.newBuilder().setTripId(tripId).build())
      .setCurrentStopSequence(99)
      .setPosition(
        GtfsRealtime.Position.newBuilder().setLatitude(1).setLongitude(1).setBearing(30).build()
      )
      .build();
    testVehiclePositions(posWithInvalidSequence);
  }

  private void testVehiclePositions(VehiclePosition pos) {
    var service = new DefaultVehiclePositionService();
    var trip = TransitModelForTest.trip(tripId).build();
    var stopTimes = List.of(stopTime(trip, 0), stopTime(trip, 1), stopTime(trip, 2));

    TripPattern pattern = tripPattern(trip, stopTimes);

    var tripForId = Map.of(scopedTripId, trip);
    var patternForTrip = Map.of(trip, pattern);

    // an untouched pattern has no vehicle positions
    assertEquals(0, service.getVehiclePositions(pattern).size());

    // Map positions to trips in feed
    VehiclePositionPatternMatcher matcher = new VehiclePositionPatternMatcher(
      TransitModelForTest.FEED_ID,
      tripForId::get,
      patternForTrip::get,
      (id, time) -> patternForTrip.get(id),
      service,
      zoneId
    );

    var positions = List.of(pos);

    // Execute the same match-to-pattern step as the runner
    matcher.applyVehiclePositionUpdates(positions);

    // ensure that gtfs-rt was matched to an OTP pattern correctly
    var vehiclePositions = service.getVehiclePositions(pattern);
    assertEquals(1, vehiclePositions.size());

    var parsedPos = vehiclePositions.get(0);
    assertEquals(tripId, parsedPos.trip().getId().getId());
    assertEquals(new WgsCoordinate(1, 1), parsedPos.coordinates());
    assertEquals(30, parsedPos.heading());

    // if we have an empty list of updates then clear the positions from the previous update
    matcher.applyVehiclePositionUpdates(List.of());
    assertEquals(0, service.getVehiclePositions(pattern).size());
  }

  @Test
  public void clearOldTrips() {
    var service = new DefaultVehiclePositionService();

    var tripId1 = "trip1";
    var tripId2 = "trip2";
    var scopedTripId1 = TransitModelForTest.id(tripId1);
    var scopedTripId2 = TransitModelForTest.id(tripId2);

    var trip1 = TransitModelForTest.trip(tripId1).build();
    var trip2 = TransitModelForTest.trip(tripId2).build();

    var stopTimes1 = List.of(stopTime(trip1, 0), stopTime(trip1, 1), stopTime(trip1, 2));

    var stopTime2 = List.of(stopTime(trip1, 0), stopTime(trip1, 1), stopTime(trip2, 2));

    var pattern1 = tripPattern(trip1, stopTimes1);
    var pattern2 = tripPattern(trip2, stopTime2);

    var tripForId = Map.of(scopedTripId1, trip1, scopedTripId2, trip2);

    var patternForTrip = Map.of(trip1, pattern1, trip2, pattern2);

    // an untouched pattern has no vehicle positions
    assertEquals(0, service.getVehiclePositions(pattern1).size());

    // Map positions to trips in feed
    VehiclePositionPatternMatcher matcher = new VehiclePositionPatternMatcher(
      TransitModelForTest.FEED_ID,
      tripForId::get,
      patternForTrip::get,
      (id, time) -> patternForTrip.get(id),
      service,
      zoneId
    );

    var pos1 = vehiclePosition(tripId1);

    var pos2 = vehiclePosition(tripId2);

    var positions = List.of(pos1, pos2);

    // Execute the same match-to-pattern step as the runner
    matcher.applyVehiclePositionUpdates(positions);

    // ensure that gtfs-rt was matched to an OTP pattern correctly
    assertEquals(1, service.getVehiclePositions(pattern1).size());
    assertEquals(1, service.getVehiclePositions(pattern2).size());

    matcher.applyVehiclePositionUpdates(List.of(pos1));
    assertEquals(1, service.getVehiclePositions(pattern1).size());
    // because there are no more updates for pattern2 we remove all positions
    assertEquals(0, service.getVehiclePositions(pattern2).size());
  }

  static Stream<Arguments> inferenceTestCases = Stream.of(
    Arguments.of("2022-04-05T15:26:04+02:00", "2022-04-05"),
    Arguments.of("2022-04-06T00:26:04+02:00", "2022-04-05"),
    Arguments.of("2022-04-06T10:26:04+02:00", "2022-04-06")
  );

  @ParameterizedTest(name = "{0} should resolve to {1}")
  @VariableSource("inferenceTestCases")
  void inferServiceDayOfTripAt6(String time, String expectedDate) {
    var trip = TransitModelForTest.trip(tripId).build();

    var sixOclock = (int) Duration.ofHours(18).toSeconds();
    var fivePast6 = sixOclock + 300;

    var stopTimes = List.of(stopTime(trip, 0, sixOclock), stopTime(trip, 1, fivePast6));

    var tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());

    var instant = OffsetDateTime.parse(time).toInstant();
    var inferredDate = VehiclePositionPatternMatcher.inferServiceDate(tripTimes, zoneId, instant);

    assertEquals(LocalDate.parse(expectedDate), inferredDate);
  }

  @Test
  void inferServiceDateCloseToMidnight() {
    var trip = TransitModelForTest.trip(tripId).build();

    var fiveToMidnight = LocalTime.parse("23:55").toSecondOfDay();
    var fivePastMidnight = fiveToMidnight + (10 * 60);
    var stopTimes = List.of(stopTime(trip, 0, fiveToMidnight), stopTime(trip, 1, fivePastMidnight));

    var tripTimes = new TripTimes(trip, stopTimes, new Deduplicator());

    var time = OffsetDateTime.parse("2022-04-05T00:04:00+02:00").toInstant();

    // because the trip "crosses" midnight and we are already on the next day, we infer the service date to be
    // yesterday
    var inferredDate = VehiclePositionPatternMatcher.inferServiceDate(tripTimes, zoneId, time);

    assertEquals(LocalDate.parse("2022-04-04"), inferredDate);
  }

  private static TripPattern tripPattern(Trip trip, List<StopTime> stopTimes) {
    var stopPattern = new StopPattern(stopTimes);
    var pattern = TripPattern
      .of(trip.getId())
      .withStopPattern(stopPattern)
      .withRoute(ROUTE)
      .build();
    pattern
      .getScheduledTimetable()
      .addTripTimes(new TripTimes(trip, stopTimes, new Deduplicator()));
    return pattern;
  }

  private static VehiclePosition vehiclePosition(String tripId1) {
    return VehiclePosition
      .newBuilder()
      .setTrip(TripDescriptor.newBuilder().setTripId(tripId1).setStartDate("20220314").build())
      .setStopId("stop-1")
      .setPosition(
        GtfsRealtime.Position.newBuilder().setLatitude(1).setLongitude(1).setBearing(30).build()
      )
      .build();
  }
}
