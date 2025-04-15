package org.opentripplanner.service.realtimevehicles.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.framework.geometry.WgsCoordinate.GREENWICH;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.route;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.tripPattern;

import com.google.common.collect.ImmutableListMultimap;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.framework.FeedScopedId;
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
  private static final TripPattern PATTERN1 = tripPattern("p1", ROUTE)
    .withStopPattern(STOP_PATTERN)
    .build();
  private static final TripPattern PATTERN2 = tripPattern(
    "p2",
    ROUTE.copy().withId(new FeedScopedId("f2", "r2")).build()
  )
    .withId(new FeedScopedId("f2", "p2"))
    .withStopPattern(STOP_PATTERN)
    .build();
  private static final Instant TIME = Instant.ofEpochSecond(1000);
  private static final RealtimeVehicle VEHICLE = RealtimeVehicle.builder()
    .withTime(TIME)
    .withCoordinates(GREENWICH)
    .build();

  private static final String FEED_ID = PATTERN1.getFeedId();

  @Test
  void empty() {
    var service = service();
    assertThat(service.getRealtimeVehicles(PATTERN1)).isEmpty();
  }

  @Test
  void clearFeed() {
    var service = service();
    service.setRealtimeVehiclesForFeed(FEED_ID, ImmutableListMultimap.of(PATTERN1, VEHICLE));
    service.setRealtimeVehiclesForFeed(FEED_ID, ImmutableListMultimap.of());
    assertThat(service.getRealtimeVehicles(PATTERN1)).isEmpty();
  }

  @Test
  void keepOtherFeeds() {
    var service = service();
    service.setRealtimeVehiclesForFeed(FEED_ID, ImmutableListMultimap.of(PATTERN1, VEHICLE));
    service.setRealtimeVehiclesForFeed(
      PATTERN2.getFeedId(),
      ImmutableListMultimap.of(PATTERN2, VEHICLE)
    );
    service.setRealtimeVehiclesForFeed(FEED_ID, ImmutableListMultimap.of());
    assertEquals(List.of(VEHICLE), service.getRealtimeVehicles(PATTERN2));
  }

  @Test
  void originalPattern() {
    var service = service();

    service.setRealtimeVehiclesForFeed(FEED_ID, ImmutableListMultimap.of(PATTERN1, VEHICLE));
    var updates = service.getRealtimeVehicles(PATTERN1);
    assertEquals(List.of(VEHICLE), updates);
  }

  @Test
  void realtimeAddedPattern() {
    var service = service();
    var realtimePattern = tripPattern("realtime-added", ROUTE)
      .withStopPattern(STOP_PATTERN)
      .withOriginalTripPattern(PATTERN1)
      .withCreatedByRealtimeUpdater(true)
      .build();
    service.setRealtimeVehiclesForFeed(FEED_ID, ImmutableListMultimap.of(realtimePattern, VEHICLE));
    var updates = service.getRealtimeVehicles(PATTERN1);
    assertEquals(List.of(VEHICLE), updates);
  }

  private static DefaultRealtimeVehicleService service() {
    return new DefaultRealtimeVehicleService(new DefaultTransitService(new TimetableRepository()));
  }
}
