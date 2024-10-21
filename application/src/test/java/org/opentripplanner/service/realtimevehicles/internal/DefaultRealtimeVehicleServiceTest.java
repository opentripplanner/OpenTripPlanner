package org.opentripplanner.service.realtimevehicles.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.framework.geometry.WgsCoordinate.GREENWICH;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.route;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.tripPattern;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;

class DefaultRealtimeVehicleServiceTest {

  private static final Route ROUTE = route("r1").build();
  private static final TimetableRepositoryForTest MODEL = TimetableRepositoryForTest.of();
  private static final StopPattern STOP_PATTERN = TimetableRepositoryForTest.stopPattern(
    MODEL.stop("1").build(),
    MODEL.stop("2").build()
  );
  private static final TripPattern ORIGINAL = tripPattern("original", ROUTE)
    .withStopPattern(STOP_PATTERN)
    .build();
  private static final Instant TIME = Instant.ofEpochSecond(1000);
  private static final List<RealtimeVehicle> VEHICLES = List.of(
    RealtimeVehicle.builder().withTime(TIME).withCoordinates(GREENWICH).build()
  );

  @Test
  void originalPattern() {
    var service = new DefaultRealtimeVehicleService(
      new DefaultTransitService(new TimetableRepository())
    );
    service.setRealtimeVehicles(ORIGINAL, VEHICLES);
    var updates = service.getRealtimeVehicles(ORIGINAL);
    assertEquals(VEHICLES, updates);
  }

  @Test
  void realtimeAddedPattern() {
    var service = new DefaultRealtimeVehicleService(
      new DefaultTransitService(new TimetableRepository())
    );
    var realtimePattern = tripPattern("realtime-added", ROUTE)
      .withStopPattern(STOP_PATTERN)
      .withOriginalTripPattern(ORIGINAL)
      .withCreatedByRealtimeUpdater(true)
      .build();
    service.setRealtimeVehicles(realtimePattern, VEHICLES);
    var updates = service.getRealtimeVehicles(ORIGINAL);
    assertEquals(VEHICLES, updates);
  }
}
