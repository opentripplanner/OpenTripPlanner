package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTripWithTime;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByAccess;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByDirect;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByEgress;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterAccess;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterDirect;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterEgress;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterWithNoTime;

import java.time.Duration;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class DepartAfterTripFilterTest {

  private final DepartAfterTripFilter filter = new DepartAfterTripFilter();

  // Trip: departs 10:00+01:00, ends 11:00+01:00.
  private static final ZonedDateTime TRIP_DEPARTURE = ZonedDateTime.parse(
    "2024-01-15T10:00:00+01:00"
  );
  private static final Duration WINDOW = Duration.ofMinutes(30);

  // ---------------------------------------------------------------------------
  // Filter does not act on these requests — always returns true
  // ---------------------------------------------------------------------------

  @Test
  void isCandidateTrip_ignores_arriveByDirectRequest_returnsTrue() {
    assertTrue(filter.isCandidateTrip(trip(), arriveByDirect(TRIP_DEPARTURE.toInstant()), WINDOW));
  }

  @Test
  void isCandidateTrip_ignores_arriveByAccessRequest_returnsTrue() {
    assertTrue(filter.isCandidateTrip(trip(), arriveByAccess(TRIP_DEPARTURE.toInstant()), WINDOW));
  }

  @Test
  void isCandidateTrip_ignores_arriveByEgressRequest_returnsTrue() {
    assertTrue(filter.isCandidateTrip(trip(), arriveByEgress(TRIP_DEPARTURE.toInstant()), WINDOW));
  }

  @Test
  void isCandidateTrip_ignores_departAfterRequestWithNoTime_returnsTrue() {
    assertTrue(filter.isCandidateTrip(trip(), departAfterWithNoTime(), WINDOW));
  }

  // ---------------------------------------------------------------------------
  // Depart After, direct routing
  // ---------------------------------------------------------------------------

  @Test
  void isCandidateTrip_departAfterDirect_returnsTrue() {
    // Trip starts 20 min after T and is still running — a straightforward candidate.
    assertTrue(filter.isCandidateTrip(trip(), departAfterDirect(at(-20)), WINDOW));
  }

  @Test
  void isCandidateTrip_departAfterDirect_tripUnderwayAtT_returnsTrue() {
    // Trip started 15 min before T but is still running — mid-route pickup is viable.
    assertTrue(filter.isCandidateTrip(trip(), departAfterDirect(at(15)), WINDOW));
  }

  @Test
  void isCandidateTrip_departAfterDirect_tripFinishedBeforeT_returnsFalse() {
    // Trip ended before T — no pickup possible.
    assertFalse(filter.isCandidateTrip(trip(), departAfterDirect(at(90)), WINDOW));
  }

  @Test
  void isCandidateTrip_departAfterDirect_tripStartsAtUpperBound_returnsTrue() {
    // trip.startTime = T + 30 + 15 = T + 45 min — exactly at the upper bound.
    assertTrue(filter.isCandidateTrip(trip(), departAfterDirect(at(-45)), WINDOW));
  }

  @Test
  void isCandidateTrip_departAfterDirect_tripStartsJustPastUpperBound_returnsFalse() {
    // trip.startTime = T + 46 min — one minute past the upper bound.
    assertFalse(filter.isCandidateTrip(trip(), departAfterDirect(at(-46)), WINDOW));
  }

  @Test
  void isCandidateTrip_departAfterDirect_nullSearchWindow_returnsTrue() {
    // Without a search window the upper bound is not enforced.
    assertTrue(filter.isCandidateTrip(trip(), departAfterDirect(at(-180)), null));
  }

  // ---------------------------------------------------------------------------
  // Depart After, access — same tight upper bound as direct
  // ---------------------------------------------------------------------------

  @Test
  void isCandidateTrip_departAfterAccess_returnsTrue() {
    assertTrue(filter.isCandidateTrip(trip(), departAfterAccess(at(-20)), WINDOW));
  }

  @Test
  void isCandidateTrip_departAfterAccess_tripStartsJustPastUpperBound_returnsFalse() {
    // Access uses the same tight bound as direct — no egress slack.
    assertFalse(filter.isCandidateTrip(trip(), departAfterAccess(at(-46)), WINDOW));
  }

  // ---------------------------------------------------------------------------
  // Depart After, egress — upper bound extended by 24 h
  // ---------------------------------------------------------------------------

  @Test
  void isCandidateTrip_departAfterEgress_returnsTrue() {
    // Trip starts 2 h after T — past the direct/access bound (T+45min) but within the egress
    // 24 h slack.
    assertTrue(filter.isCandidateTrip(trip(), departAfterEgress(at(-120)), WINDOW));
  }

  @Test
  void isCandidateTrip_departAfterEgress_tripPastEgressSlack_returnsFalse() {
    // Trip starts beyond T + searchWindow + maxWalkTime + 24 h.
    assertFalse(
      filter.isCandidateTrip(
        trip(),
        departAfterEgress(TRIP_DEPARTURE.minusHours(24).minusMinutes(46).toInstant()),
        WINDOW
      )
    );
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  /** T expressed as minutes relative to TRIP_DEPARTURE (negative = before the trip departs). */
  private static java.time.Instant at(int minutesFromTripDeparture) {
    return TRIP_DEPARTURE.plusMinutes(minutesFromTripDeparture).toInstant();
  }

  private static org.opentripplanner.ext.carpooling.model.CarpoolTrip trip() {
    return createSimpleTripWithTime(OSLO_CENTER, OSLO_NORTH, TRIP_DEPARTURE);
  }
}
