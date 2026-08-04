package org.opentripplanner.service.realtimevehicles.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.opentripplanner.street.geometry.WgsCoordinate.GREENWICH;

import com.google.common.collect.ImmutableListMultimap;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.service.realtimevehicles.model.RealtimeVehicle;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.updater.trip.RealtimeTestConstants;

class DefaultRealtimeVehicleRepositoryTest implements RealtimeTestConstants {

  private static final RealtimeVehicle VEHICLE = RealtimeVehicle.builder()
    .withTime(Instant.ofEpochSecond(1000))
    .withCoordinates(GREENWICH)
    .build();

  private final DefaultRealtimeVehicleRepository repository =
    new DefaultRealtimeVehicleRepository();
  private final TripPattern pattern;
  private final TripPattern patternInOtherFeed;
  private final String feedId;

  DefaultRealtimeVehicleRepositoryTest() {
    var envBuilder = TransitTestEnvironment.of();
    var tripInput = TripInput.of(TRIP_1_ID)
      .addStop(envBuilder.stop(STOP_A_ID), "12:00:00")
      .addStop(envBuilder.stop(STOP_B_ID), "12:30:00");
    var env = envBuilder.addTrip(tripInput).build();
    pattern = env.tripData(TRIP_1_ID).scheduledTripPattern();
    patternInOtherFeed = pattern
      .copy()
      .withId(new FeedScopedId("f2", "p2"))
      .withRoute(pattern.getRoute().copy().withId(new FeedScopedId("f2", "r2")).build())
      .build();
    feedId = pattern.getFeedId();
  }

  @Test
  void empty() {
    assertThat(vehicles(pattern)).isEmpty();
  }

  @Test
  void lookupByPattern() {
    repository.setRealtimeVehiclesForFeed(feedId, ImmutableListMultimap.of(pattern, VEHICLE));
    assertThat(vehicles(pattern)).containsExactly(VEHICLE);
  }

  @Test
  void clearFeed() {
    repository.setRealtimeVehiclesForFeed(feedId, ImmutableListMultimap.of(pattern, VEHICLE));
    repository.setRealtimeVehiclesForFeed(feedId, ImmutableListMultimap.of());
    assertThat(vehicles(pattern)).isEmpty();
  }

  @Test
  void keepOtherFeeds() {
    repository.setRealtimeVehiclesForFeed(feedId, ImmutableListMultimap.of(pattern, VEHICLE));
    repository.setRealtimeVehiclesForFeed(
      patternInOtherFeed.getFeedId(),
      ImmutableListMultimap.of(patternInOtherFeed, VEHICLE)
    );
    repository.setRealtimeVehiclesForFeed(feedId, ImmutableListMultimap.of());
    assertThat(vehicles(patternInOtherFeed)).containsExactly(VEHICLE);
  }

  @Test
  void realtimeAddedPattern() {
    var realtimePattern = pattern
      .copy()
      .withId(new FeedScopedId(feedId, "realtime-added"))
      .withOriginalTripPattern(pattern)
      .withRealTimeStopPatternModified()
      .build();
    repository.setRealtimeVehiclesForFeed(
      feedId,
      ImmutableListMultimap.of(realtimePattern, VEHICLE)
    );
    assertThat(vehicles(pattern)).containsExactly(VEHICLE);
  }

  @Test
  void snapshotIsNotAffectedByLaterWrites() {
    repository.setRealtimeVehiclesForFeed(feedId, ImmutableListMultimap.of(pattern, VEHICLE));
    var snapshot = repository.createSnapshot();

    repository.setRealtimeVehiclesForFeed(feedId, ImmutableListMultimap.of());

    assertThat(vehicles(pattern)).isEmpty();
    assertThat(snapshot.getRealtimeVehicles(pattern)).containsExactly(VEHICLE);
  }

  @Test
  void lifecycle() {
    var lifecycle = new RealtimeVehicleRepositoryLifecycle();
    repository.setRealtimeVehiclesForFeed(feedId, ImmutableListMultimap.of(pattern, VEHICLE));

    var snapshot = lifecycle.freeze(repository);
    assertThat(snapshot.getRealtimeVehicles(pattern)).containsExactly(VEHICLE);

    // copy-on-write creates a new repository initialized with the state of the snapshot
    var copy = lifecycle.copyOnWrite(snapshot);
    assertNotSame(repository, copy);
    assertThat(lifecycle.freeze(copy).getRealtimeVehicles(pattern)).containsExactly(VEHICLE);
  }

  @Test
  void editsToCopyDoNotAffectSnapshot() {
    var lifecycle = new RealtimeVehicleRepositoryLifecycle();
    repository.setRealtimeVehiclesForFeed(feedId, ImmutableListMultimap.of(pattern, VEHICLE));
    var snapshot = lifecycle.freeze(repository);

    var copy = lifecycle.copyOnWrite(snapshot);
    copy.setRealtimeVehiclesForFeed(feedId, ImmutableListMultimap.of());

    assertThat(snapshot.getRealtimeVehicles(pattern)).containsExactly(VEHICLE);
  }

  private List<RealtimeVehicle> vehicles(TripPattern pattern) {
    return repository.createSnapshot().getRealtimeVehicles(pattern);
  }
}
