package org.opentripplanner.service.realtimevehicles.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.opentripplanner.transit.model.timetable.OccupancyStatus.FEW_SEATS_AVAILABLE;
import static org.opentripplanner.transit.model.timetable.OccupancyStatus.MANY_SEATS_AVAILABLE;
import static org.opentripplanner.transit.model.timetable.OccupancyStatus.NO_DATA_AVAILABLE;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import com.google.common.collect.ImmutableListMultimap;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.siri.SiriTestHelper;

class DefaultRealtimeVehicleServiceTest implements RealtimeTestConstants {

  private static final Instant TIME = Instant.ofEpochSecond(1000);

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of();
  private final RegularStop stopA = envBuilder.stop(STOP_A_ID);
  private final RegularStop stopB = envBuilder.stopAtStation(STOP_B_ID, STATION_OMEGA_ID);
  private final RegularStop stopC = envBuilder.stopAtStation(STOP_C_ID, STATION_OMEGA_ID);

  private final TripInput tripInput = TripInput.of(TRIP_1_ID)
    .withWithTripOnServiceDate(TRIP_1_ID)
    .addStop(stopA, "0:00:10", "0:00:11")
    .addStop(stopB, "0:00:20", "0:00:21");

  private final DefaultRealtimeVehicleRepository repository =
    new DefaultRealtimeVehicleRepository();

  @Test
  void realtimePatternLookup() {
    var env = envBuilder.addTrip(tripInput).build();
    applyQuayChange(env);

    var tripData = env.tripData(TRIP_1_ID);
    var scheduledPattern = tripData.scheduledTripPattern();
    var realtimePattern = tripData.tripPattern();
    assertNotEquals(scheduledPattern, realtimePattern);

    var vehicle = vehicle(tripData.trip(), TIME, FEW_SEATS_AVAILABLE);
    repository.setRealtimeVehiclesForFeed(
      env.feedId(),
      ImmutableListMultimap.of(realtimePattern, vehicle)
    );
    var service = new DefaultRealtimeVehicleService(repository, env.transitService());

    // the vehicle can be looked up with either the scheduled or the realtime pattern
    assertEquals(List.of(vehicle), service.getRealtimeVehicles(scheduledPattern));
    assertEquals(List.of(vehicle), service.getRealtimeVehicles(realtimePattern));
  }

  @Test
  void occupancyStatusOfLatestVehicle() {
    var env = envBuilder.addTrip(tripInput).build();
    var tripData = env.tripData(TRIP_1_ID);
    var trip = tripData.trip();
    var pattern = tripData.scheduledTripPattern();

    var earlier = vehicle(trip, TIME, MANY_SEATS_AVAILABLE);
    var latest = vehicle(trip, TIME.plusSeconds(60), FEW_SEATS_AVAILABLE);
    repository.setRealtimeVehiclesForFeed(
      env.feedId(),
      ImmutableListMultimap.of(pattern, earlier, pattern, latest)
    );
    var service = new DefaultRealtimeVehicleService(repository, env.transitService());

    assertEquals(FEW_SEATS_AVAILABLE, service.getVehicleOccupancyStatus(trip));
  }

  @Test
  void occupancyStatusWhenNoVehicleExists() {
    var env = envBuilder.addTrip(tripInput).build();
    var trip = env.tripData(TRIP_1_ID).trip();
    var service = new DefaultRealtimeVehicleService(repository, env.transitService());

    assertEquals(NO_DATA_AVAILABLE, service.getVehicleOccupancyStatus(trip));
  }

  @Test
  void occupancyStatusWhenVehicleHasNoOccupancyData() {
    var env = envBuilder.addTrip(tripInput).build();
    var tripData = env.tripData(TRIP_1_ID);
    var trip = tripData.trip();

    var vehicle = RealtimeVehicle.builder().withTrip(trip).withTime(TIME).build();
    repository.setRealtimeVehiclesForFeed(
      env.feedId(),
      ImmutableListMultimap.of(tripData.scheduledTripPattern(), vehicle)
    );
    var service = new DefaultRealtimeVehicleService(repository, env.transitService());

    assertEquals(NO_DATA_AVAILABLE, service.getVehicleOccupancyStatus(trip));
  }

  @Test
  void occupancyStatusOnRealtimeModifiedPattern() {
    var env = envBuilder.addTrip(tripInput).build();
    applyQuayChange(env);

    var tripData = env.tripData(TRIP_1_ID);
    var trip = tripData.trip();
    var vehicle = vehicle(trip, TIME, FEW_SEATS_AVAILABLE);
    repository.setRealtimeVehiclesForFeed(
      env.feedId(),
      ImmutableListMultimap.of(tripData.tripPattern(), vehicle)
    );
    var service = new DefaultRealtimeVehicleService(repository, env.transitService());

    assertEquals(FEW_SEATS_AVAILABLE, service.getVehicleOccupancyStatus(trip));
  }

  /**
   * Change the quay of the second call (from B to C in the same station) so that a realtime
   * pattern replaces the scheduled one.
   */
  private void applyQuayChange(TransitTestEnvironment env) {
    var siri = SiriTestHelper.of(env);
    var updates = siri
      .etBuilder()
      .withDatedVehicleJourneyRef(TRIP_1_ID)
      .withRecordedCalls(builder -> builder.call(stopA).departAimedActual("00:00:11", "00:00:15"))
      .withEstimatedCalls(builder ->
        builder.call(stopC).arriveAimedExpected("00:00:20", "00:00:33")
      )
      .buildEstimatedTimetableDeliveries();
    assertSuccess(siri.applyEstimatedTimetable(updates));
  }

  private static RealtimeVehicle vehicle(Trip trip, Instant time, OccupancyStatus occupancy) {
    return RealtimeVehicle.builder()
      .withTrip(trip)
      .withTime(time)
      .withOccupancyStatus(occupancy)
      .build();
  }
}
