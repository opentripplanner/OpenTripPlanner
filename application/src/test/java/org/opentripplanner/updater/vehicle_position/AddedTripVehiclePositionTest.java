package org.opentripplanner.updater.vehicle_position;

import static com.google.common.truth.Truth.assertThat;
import static com.google.transit.realtime.GtfsRealtime.TripDescriptor.ScheduleRelationship.ADDED;
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
import org.junit.jupiter.api.Test;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleRepository;
import org.opentripplanner.service.realtimevehicles.internal.DefaultRealtimeVehicleService;
import org.opentripplanner.service.realtimevehicles.internal.RealtimeVehicleRepositoryLifecycle;
import org.opentripplanner.standalone.config.routerconfig.updaters.VehiclePositionsUpdaterConfig;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.timetable.OccupancyStatus;
import org.opentripplanner.updater.trip.RealtimeTestConstants;
import org.opentripplanner.updater.trip.gtfs.GtfsRtTestHelper;
import org.opentripplanner.utils.time.ServiceDateUtils;

/**
 * GTFS-RT extra journeys ({@code ADDED}/{@code NEW}) have no scheduled trip times. Vehicle
 * positions must fall back to the realtime timetable or they are rejected as
 * {@code TRIP_NOT_FOUND_IN_PATTERN} (#6176).
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
    var matcher = new RealtimeVehiclePatternMatcher(
      env.feedId(),
      snapshot::getTrip,
      snapshot::findPattern,
      snapshot::findPattern,
      snapshot.getTripCalendars()::listServiceDates,
      repository,
      env.timeZone(),
      null,
      FEATURES,
      snapshot::resolve
    );

    var position = VehiclePosition.newBuilder()
      .setTrip(
        TripDescriptor.newBuilder()
          .setTripId(ADDED_TRIP_ID)
          .setStartDate(ServiceDateUtils.asCompactString(env.defaultServiceDate()))
          .setScheduleRelationship(TripDescriptor.ScheduleRelationship.ADDED)
          .build()
      )
      .setStopId(STOP_A_ID)
      .setOccupancyStatus(VehiclePosition.OccupancyStatus.FEW_SEATS_AVAILABLE)
      .setPosition(
        GtfsRealtime.Position.newBuilder().setLatitude(1).setLongitude(1).setBearing(30).build()
      )
      .build();

    assertSuccess(matcher.applyRealtimeVehicleUpdates(List.of(position)));

    var vehicles = new RealtimeVehicleRepositoryLifecycle()
      .freeze(repository)
      .getRealtimeVehicles(addedPattern);
    assertThat(vehicles).hasSize(1);
    var vehicle = vehicles.get(0);
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
}
