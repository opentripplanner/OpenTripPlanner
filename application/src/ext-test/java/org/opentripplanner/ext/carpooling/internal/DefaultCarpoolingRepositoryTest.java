package org.opentripplanner.ext.carpooling.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTripWithVerticesTestData.withDummyVertices;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.ext.carpooling.CarpoolTripTestData;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;

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

  private static CarpoolTrip tripEndingAt(ZonedDateTime endTime) {
    return CarpoolTripTestData.createSimpleTripWithTimes(
      OSLO_CENTER,
      OSLO_EAST,
      endTime.minusHours(1),
      endTime
    );
  }
}
