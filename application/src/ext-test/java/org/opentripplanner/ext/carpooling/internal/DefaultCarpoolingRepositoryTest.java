package org.opentripplanner.ext.carpooling.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTripWithVerticesTestData.withDummyVertices;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolTripTestData;
import org.opentripplanner.ext.carpooling.model.CarpoolStop;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.model.CarpoolTripBuilder;
import org.opentripplanner.street.geometry.WgsCoordinate;

class DefaultCarpoolingRepositoryTest {

  private static final ZonedDateTime NOON = ZonedDateTime.of(
    2026,
    6,
    5,
    12,
    0,
    0,
    0,
    ZoneId.of("Europe/Oslo")
  );

  @Test
  void removesTripsThatEndedBeforeTheThreshold() {
    var repository = new DefaultCarpoolingRepository();
    var ended = withDummyVertices(tripEndingAt(NOON));
    var ongoing = withDummyVertices(tripEndingAt(NOON.plusHours(2)));
    repository.upsertCarpoolTrip(ended);
    repository.upsertCarpoolTrip(ongoing);

    int removed = repository.removeExpiredTrips(NOON.plusHours(1).toInstant(), Duration.ZERO);

    assertThat(removed).isEqualTo(1);
    assertThat(repository.getCarpoolTrips()).containsExactly(ongoing);
  }

  @Test
  void keepsTripsEndingExactlyAtTheThreshold() {
    var repository = new DefaultCarpoolingRepository();
    var trip = withDummyVertices(tripEndingAt(NOON));
    repository.upsertCarpoolTrip(trip);

    int removed = repository.removeExpiredTrips(NOON.toInstant(), Duration.ZERO);

    assertThat(removed).isEqualTo(0);
    assertThat(repository.getCarpoolTrips()).containsExactly(trip);
  }

  @Test
  void returnsStoredTripById() {
    var repository = new DefaultCarpoolingRepository();
    var trip = withDummyVertices(tripEndingAt(NOON));
    repository.upsertCarpoolTrip(trip);

    assertThat(repository.getCarpoolTrip(trip.trip().getId())).isSameInstanceAs(trip);
    assertThat(repository.getCarpoolTrip(FeedScopedId.ofNullable("TEST", "unknown"))).isNull();
  }

  @Test
  void appliesTheExpiryDurationToTheCutOff() {
    var repository = new DefaultCarpoolingRepository();
    var ended = withDummyVertices(tripEndingAt(NOON));
    var ongoing = withDummyVertices(tripEndingAt(NOON.plusHours(2)));
    repository.upsertCarpoolTrip(ended);
    repository.upsertCarpoolTrip(ongoing);

    // Cut-off is NOON.plusHours(3) - 2h = NOON.plusHours(1), so only the trip ending at noon expires.
    int removed = repository.removeExpiredTrips(NOON.plusHours(3).toInstant(), Duration.ofHours(2));

    assertThat(removed).isEqualTo(1);
    assertThat(repository.getCarpoolTrips()).containsExactly(ongoing);
  }

  @Test
  void throttlesSweepsToOncePerInterval() {
    var repository = new DefaultCarpoolingRepository();
    repository.upsertCarpoolTrip(withDummyVertices(tripEndingAt(NOON)));

    // First sweep runs and purges the expired trip.
    assertThat(
      repository.removeExpiredTrips(NOON.plusHours(1).toInstant(), Duration.ZERO)
    ).isEqualTo(1);

    // A second, already-expired trip is added shortly after.
    var addedAfterSweep = withDummyVertices(tripEndingAt(NOON));
    repository.upsertCarpoolTrip(addedAfterSweep);

    // A call within the sweep interval is throttled: nothing is scanned or removed.
    assertThat(
      repository.removeExpiredTrips(NOON.plusHours(1).plusMinutes(30).toInstant(), Duration.ZERO)
    ).isEqualTo(0);
    assertThat(repository.getCarpoolTrips()).containsExactly(addedAfterSweep);

    // Once the interval has elapsed the next call sweeps again.
    assertThat(
      repository.removeExpiredTrips(NOON.plusHours(2).toInstant(), Duration.ZERO)
    ).isEqualTo(1);
    assertThat(repository.getCarpoolTrips()).isEmpty();
  }

  @Test
  void keepsCachedRoutingAcrossASameGeometryUpsert() {
    var repository = new DefaultCarpoolingRepository();
    var trip = tripWithCoordinates("kept", OSLO_CENTER, OSLO_EAST, NOON.plusHours(1));
    repository.upsertCarpoolTrip(withDummyVertices(trip));
    repository.cacheBaselineRouting(trip, new Duration[] { Duration.ofMinutes(12) });

    // A budget- or time-only update keeps the same route points, so the cache survives.
    var updated = tripWithCoordinates("kept", OSLO_CENTER, OSLO_EAST, NOON.plusHours(2));
    repository.upsertCarpoolTrip(withDummyVertices(updated));

    var cached = repository.cachedBaselineRouting(updated);
    assertThat(cached).isNotNull();
    assertThat(cached.legDurations()).asList().containsExactly(Duration.ofMinutes(12));
  }

  @Test
  void cachesAnUnroutableBaseline() {
    var repository = new DefaultCarpoolingRepository();
    var trip = tripWithCoordinates("unroutable", OSLO_CENTER, OSLO_EAST, NOON.plusHours(1));
    repository.upsertCarpoolTrip(withDummyVertices(trip));
    repository.cacheBaselineRouting(trip, null);

    // A cached failure is a hit (so the trip is not re-routed) whose durations are absent.
    var cached = repository.cachedBaselineRouting(trip);
    assertThat(cached).isNotNull();
    assertThat(cached.legDurations()).isNull();
  }

  @Test
  void ignoresCachedRoutingWhenQueriedWithDifferentGeometry() {
    var repository = new DefaultCarpoolingRepository();
    var trip = tripWithCoordinates("guard", OSLO_CENTER, OSLO_EAST, NOON.plusHours(1));
    repository.upsertCarpoolTrip(withDummyVertices(trip));
    repository.cacheBaselineRouting(trip, new Duration[] { Duration.ofMinutes(12) });

    // A concurrent re-route can leave an entry under this id while the trip already has new
    // geometry. Validating against the trip's route points treats that as a miss rather than
    // serving a stale verdict.
    var moved = tripWithCoordinates("guard", OSLO_CENTER, new WgsCoordinate(60.0, 11.0), NOON);
    assertThat(repository.cachedBaselineRouting(moved)).isNull();
  }

  @Test
  void dropsCachedRoutingWhenTripRemoved() {
    var repository = new DefaultCarpoolingRepository();
    var trip = tripWithCoordinates("removed", OSLO_CENTER, OSLO_EAST, NOON.plusHours(1));
    repository.upsertCarpoolTrip(withDummyVertices(trip));
    repository.cacheBaselineRouting(trip, new Duration[] { Duration.ofMinutes(12) });

    repository.removeCarpoolTrip(trip.getId());

    assertThat(repository.cachedBaselineRouting(trip)).isNull();
  }

  @Test
  void dropsCachedRoutingWhenTripExpires() {
    var repository = new DefaultCarpoolingRepository();
    var trip = tripWithCoordinates("expired", OSLO_CENTER, OSLO_EAST, NOON);
    repository.upsertCarpoolTrip(withDummyVertices(trip));
    repository.cacheBaselineRouting(trip, new Duration[] { Duration.ofMinutes(12) });

    repository.removeExpiredTrips(NOON.plusHours(1).toInstant(), Duration.ZERO);

    assertThat(repository.cachedBaselineRouting(trip)).isNull();
  }

  @Test
  void returnsADefensiveCopyOfCachedDurations() {
    var repository = new DefaultCarpoolingRepository();
    var trip = tripWithCoordinates("copy", OSLO_CENTER, OSLO_EAST, NOON.plusHours(1));
    repository.upsertCarpoolTrip(withDummyVertices(trip));
    var stored = new Duration[] { Duration.ofMinutes(12) };
    repository.cacheBaselineRouting(trip, stored);

    // Neither mutating the stored source array nor the returned array may corrupt the cache.
    stored[0] = Duration.ofMinutes(99);
    repository.cachedBaselineRouting(trip).legDurations()[0] = Duration.ofMinutes(7);

    assertThat(repository.cachedBaselineRouting(trip).legDurations())
      .asList()
      .containsExactly(Duration.ofMinutes(12));
  }

  private static CarpoolTrip tripWithCoordinates(
    String id,
    WgsCoordinate origin,
    WgsCoordinate destination,
    ZonedDateTime endTime
  ) {
    return new CarpoolTripBuilder(FeedScopedId.ofNullable("TEST", id))
      .withStops(
        List.of(
          CarpoolStop.of(FeedScopedId.ofNullable("TEST", id + "-origin"))
            .withCoordinate(origin)
            .withOnboardCount(1)
            .build(),
          CarpoolStop.of(FeedScopedId.ofNullable("TEST", id + "-destination"))
            .withCoordinate(destination)
            .withOnboardCount(1)
            .build()
        )
      )
      .withTotalCapacity(CarpoolTrip.DEFAULT_TOTAL_CAPACITY)
      .withStartTime(NOON)
      .withEndTime(endTime)
      .build();
  }

  private static CarpoolTrip tripEndingAt(ZonedDateTime endTime) {
    return CarpoolTripTestData.createSimpleTripWithTimes(
      OSLO_CENTER,
      OSLO_EAST,
      endTime.minusHours(1),
      endTime
    );
  }
}
