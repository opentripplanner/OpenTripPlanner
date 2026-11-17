package org.opentripplanner.updater.vehicle_position;

import static com.google.common.truth.Truth.assertThat;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.REPLACEMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.core.model.id.FeedScopedIdForTestFactory.id;
import static org.opentripplanner.standalone.config.routerconfig.updaters.VehiclePositionsUpdaterConfig.VehiclePositionFeature.OCCUPANCY;
import static org.opentripplanner.standalone.config.routerconfig.updaters.VehiclePositionsUpdaterConfig.VehiclePositionFeature.POSITION;
import static org.opentripplanner.standalone.config.routerconfig.updaters.VehiclePositionsUpdaterConfig.VehiclePositionFeature.STOP_POSITION;
import static org.opentripplanner.updater.spi.UpdateResultAssertions.assertSuccess;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleService;
import org.opentripplanner.service.realtimevehicles.internal.RealtimeVehicleRepositoryLifecycle;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.standalone.config.routerconfig.updaters.VehiclePositionsUpdaterConfig;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.transit.repository.TimetableRepositorySnapshot;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.gtfs.GtfsRtTestHelper;
import org.opentripplanner.utils.time.ServiceDateUtils;

/**
 * Vehicle positions for GTFS-RT extra journeys (ADDED, REPLACEMENT) look up trip times on the
 * resolved realtime timetable (#6176).
 */
class AddedTripVehiclePositionTest implements RealtimeTestConstants {

  private static final Set<VehiclePositionsUpdaterConfig.VehiclePositionFeature> FEATURES = Set.of(
    POSITION,
    STOP_POSITION,
    OCCUPANCY
  );

  private final TransitTestEnvironmentBuilder envBuilder = TransitTestEnvironment.of();
  private final RegularStop STOP_A = envBuilder.stop(STOP_A_ID);
  private final RegularStop STOP_B = envBuilder.stop(STOP_B_ID);
  private final RegularStop STOP_C = envBuilder.stop(STOP_C_ID);

  private final TransitTestEnvironment env = envBuilder
    .addTrip(
      TripInput.of(TRIP_1_ID)
        .withServiceDates(
          envBuilder.defaultServiceDate().minusDays(1),
          envBuilder.defaultServiceDate().plusDays(1)
        )
        .addStop(STOP_A, "12:00", "12:00")
        .addStop(STOP_B, "12:10", "12:10")
        .addStop(STOP_C, "12:20", "12:20")
    )
    .addStops(STOP_A_ID, STOP_B_ID, STOP_C_ID)
    .build();
  private final GtfsRtTestHelper gtfsRt = GtfsRtTestHelper.of(env);

  @Test
  void vehiclePositionAndOccupancyForAddedTrip() {
    var tripUpdate = gtfsRt
      .tripUpdate(ADDED_TRIP_ID, ADDED)
      .addStopTime(STOP_A_ID, "00:30")
      .addStopTime(STOP_B_ID, "00:40")
      .addStopTime(STOP_C_ID, "00:55")
      .build();
    assertSuccess(gtfsRt.applyTripUpdate(tripUpdate));

    var snapshot = env.timetableSnapshot();
    var addedTrip = env.transitService().getTrip(id(ADDED_TRIP_ID));
    var addedPattern = env.tripData(ADDED_TRIP_ID).tripPattern();
    assertThat(addedPattern.getScheduledTimetable().getTripTimes(addedTrip)).isNull();

    var repository = new DefaultRealtimeVehicleRepository();
    var matcher = matcher(env, snapshot, repository);

    var position = VehiclePosition.newBuilder()
      .setTrip(
        TripDescriptor.newBuilder()
          .setTripId(ADDED_TRIP_ID)
          .setStartDate(ServiceDateUtils.asCompactString(env.defaultServiceDate()))
          .setScheduleRelationship(ADDED)
          .build()
      )
      .setStopId(STOP_A_ID)
      .setOccupancyStatus(VehiclePosition.OccupancyStatus.FEW_SEATS_AVAILABLE)
      .setPosition(
        GtfsRealtime.Position.newBuilder().setLatitude(1).setLongitude(1).setBearing(30).build()
      )
      .build();

    assertSuccess(matcher.applyRealtimeVehicleUpdates(List.of(position)));

    var vehicle = vehicleOn(repository, addedPattern);
    assertEquals(ADDED_TRIP_ID, vehicle.trip().getId().getId());
    assertEquals(id(STOP_A_ID), vehicle.stop().orElseThrow().stop().getId());
    assertEquals(OccupancyStatus.FEW_SEATS_AVAILABLE, vehicle.occupancyStatus().orElseThrow());

    var occupancyService = new DefaultRealtimeVehicleService(
      new RealtimeVehicleRepositoryLifecycle().freeze(repository),
      env.transitService()
    );
    assertEquals(
      OccupancyStatus.FEW_SEATS_AVAILABLE,
      occupancyService.getVehicleOccupancyStatus(addedTrip)
    );
  }

  @Test
  @DisplayName(
    "current_stop_sequence on a REPLACEMENT trip maps through the replacement's own sequences"
  )
  void vehiclePositionStopSequenceForReplacementTrip() {
    var builder = TransitTestEnvironment.of();
    var stopA = builder.stop(STOP_A_ID);
    var stopB = builder.stop(STOP_B_ID);
    builder.stop(STOP_C_ID);
    var replacementEnv = builder
      .addTrip(
        TripInput.of(TRIP_1_ID)
          .addStop(stopA, "8:30:00", "8:30:00")
          .addStop(stopB, "8:40:00", "8:40:00")
      )
      .build();
    var rt = GtfsRtTestHelper.of(replacementEnv);

    var tripUpdate = rt
      .tripUpdate(TRIP_1_ID, REPLACEMENT)
      .addStopTime(STOP_A_ID, "00:30", 10)
      .addStopTime(STOP_B_ID, "00:40", 20)
      .addStopTime(STOP_C_ID, "00:55", 30)
      .build();
    assertSuccess(rt.applyTripUpdate(tripUpdate));

    var snapshot = replacementEnv.timetableSnapshot();
    var tripData = replacementEnv.tripData(TRIP_1_ID);
    var repository = new DefaultRealtimeVehicleRepository();
    var matcher = matcher(replacementEnv, snapshot, repository);

    var position = VehiclePosition.newBuilder()
      .setTrip(
        TripDescriptor.newBuilder()
          .setTripId(TRIP_1_ID)
          .setStartDate(ServiceDateUtils.asCompactString(replacementEnv.defaultServiceDate()))
          .setScheduleRelationship(REPLACEMENT)
          .build()
      )
      .setCurrentStopSequence(20)
      .build();

    assertSuccess(matcher.applyRealtimeVehicleUpdates(List.of(position)));

    // Modified patterns are stored under the original trip pattern.
    var vehicle = vehicleOn(repository, tripData.scheduledTripPattern());
    assertEquals(TRIP_1_ID, vehicle.trip().getId().getId());
    assertEquals(id(STOP_B_ID), vehicle.stop().orElseThrow().stop().getId());
  }

  private RealtimeVehiclePatternMatcher matcher(
    TransitTestEnvironment testEnv,
    TimetableRepositorySnapshot snapshot,
    DefaultRealtimeVehicleRepository repository
  ) {
    return new RealtimeVehiclePatternMatcher(
      testEnv.feedId(),
      snapshot::getTrip,
      snapshot::findPattern,
      snapshot.getTripCalendars()::listServiceDates,
      repository,
      testEnv.timeZone(),
      null,
      FEATURES,
      snapshot::resolve
    );
  }

  private static RealtimeVehicle vehicleOn(
    DefaultRealtimeVehicleRepository repository,
    TripPattern pattern
  ) {
    var vehicles = new RealtimeVehicleRepositoryLifecycle()
      .freeze(repository)
      .getRealtimeVehicles(pattern);
    assertThat(vehicles).hasSize(1);
    return vehicles.get(0);
  }
}
