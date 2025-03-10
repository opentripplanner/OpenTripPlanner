package org.opentripplanner.service.realtimevehicles.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.framework.geometry.WgsCoordinate.GREENWICH;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.FEED_ID;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.route;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.tripPattern;

import com.google.common.collect.ImmutableListMultimap;
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
  private static final RealtimeVehicle VEHICLE = RealtimeVehicle.builder()
    .withTime(TIME)
    .withCoordinates(GREENWICH)
    .build();

  private static final String FEED_ID = ORIGINAL.getFeedId();

  @Test
  void originalPattern() {
    var service = new DefaultRealtimeVehicleService(
      new DefaultTransitService(new TimetableRepository())
    );

    service.setRealtimeVehicles(FEED_ID, ImmutableListMultimap.of(ORIGINAL, VEHICLE));
    var updates = service.getRealtimeVehicles(ORIGINAL);
    assertEquals(List.of(VEHICLE), updates);
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
    service.setRealtimeVehicles(FEED_ID, ImmutableListMultimap.of(realtimePattern, VEHICLE));
    var updates = service.getRealtimeVehicles(ORIGINAL);
    assertEquals(List.of(VEHICLE), updates);
  }
}
