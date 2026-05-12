package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTripWithTimes;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByAccess;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByDirect;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByEgress;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByWithNoTime;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterAccess;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterDirect;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterEgress;

import java.time.Duration;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class ArriveByTripFilterTest {

  private final ArriveByTripFilter filter = new ArriveByTripFilter();

  // Trip: departs 10:00+01:00, ends 11:00+01:00.
  private static final ZonedDateTime TRIP_START = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
  private static final ZonedDateTime TRIP_END = TRIP_START.plusHours(1);
  private static final Duration WINDOW = Duration.ofMinutes(30);

  // ---------------------------------------------------------------------------
  // Filter does not act on these requests — always returns true
  // ---------------------------------------------------------------------------

  @Test
  void isCandidateTrip_ignores_departAfterDirectRequest_returnsTrue() {
    assertTrue(
      filter.isCandidateTrip(
        trip(),
        departAfterDirect(TRIP_END.plusMinutes(15).toInstant()),
        WINDOW
      )
    );
  }

  @Test
  void isCandidateTrip_ignores_departAfterAccessRequest_returnsTrue() {
    assertTrue(
      filter.isCandidateTrip(
        trip(),
        departAfterAccess(TRIP_END.plusMinutes(15).toInstant()),
        WINDOW
      )
    );
  }

  @Test
  void isCandidateTrip_ignores_departAfterEgressRequest_returnsTrue() {
    assertTrue(
      filter.isCandidateTrip(
        trip(),
        departAfterEgress(TRIP_END.plusMinutes(15).toInstant()),
        WINDOW
      )
    );
  }

  @Test
  void isCandidateTrip_ignores_arriveByRequestWithNoTime_returnsTrue() {
    assertTrue(filter.isCandidateTrip(trip(), arriveByWithNoTime(), WINDOW));
  }

  // ---------------------------------------------------------------------------
  // Arrive By, direct routing — deadline: trip.startTime <= T
  // ---------------------------------------------------------------------------

  @Test
  void isCandidateTrip_arriveByDirect_returnsTrue() {
    // Trip started well before T — a straightforward candidate.
    assertTrue(
      filter.isCandidateTrip(trip(), arriveByDirect(TRIP_END.plusMinutes(30).toInstant()), WINDOW)
    );
  }

  @Test
  void isCandidateTrip_arriveByDirect_tripStartsAtDeadline_returnsTrue() {
    // trip.startTime = T exactly — at the boundary.
    assertTrue(filter.isCandidateTrip(trip(), arriveByDirect(TRIP_START.toInstant()), WINDOW));
  }

  @Test
  void isCandidateTrip_arriveByDirect_tripStartsAfterDeadline_returnsFalse() {
    // Driver hasn't started yet when the passenger needs to arrive — impossible.
    assertFalse(
      filter.isCandidateTrip(trip(), arriveByDirect(TRIP_START.minusMinutes(1).toInstant()), WINDOW)
    );
  }

  @Test
  void isCandidateTrip_arriveByDirect_driverContinuesPastDeadline_returnsTrue() {
    // Driver ends after T, but started before — passenger can be dropped off mid-route.
    assertTrue(
      filter.isCandidateTrip(trip(), arriveByDirect(TRIP_START.plusMinutes(30).toInstant()), WINDOW)
    );
  }

  // ---------------------------------------------------------------------------
  // Arrive By, access — same tight deadline as direct
  // ---------------------------------------------------------------------------

  @Test
  void isCandidateTrip_arriveByAccess_returnsTrue() {
    assertTrue(
      filter.isCandidateTrip(trip(), arriveByAccess(TRIP_END.plusMinutes(30).toInstant()), WINDOW)
    );
  }

  @Test
  void isCandidateTrip_arriveByAccess_tripStartsAfterDeadline_returnsFalse() {
    // Access uses the same tight deadline as direct — no egress slack.
    assertFalse(
      filter.isCandidateTrip(trip(), arriveByAccess(TRIP_START.minusMinutes(1).toInstant()), WINDOW)
    );
  }

  // ---------------------------------------------------------------------------
  // Arrive By, egress — deadline extended by 24 h
  // ---------------------------------------------------------------------------

  @Test
  void isCandidateTrip_arriveByEgress_returnsTrue() {
    // Trip starts 1 h after T — past the direct/access deadline but within the 24 h egress slack.
    assertTrue(
      filter.isCandidateTrip(trip(), arriveByEgress(TRIP_START.minusHours(1).toInstant()), WINDOW)
    );
  }

  @Test
  void isCandidateTrip_arriveByEgress_tripPastEgressSlack_returnsFalse() {
    // Trip starts 25 h after T — beyond the 24 h egress slack.
    assertFalse(
      filter.isCandidateTrip(trip(), arriveByEgress(TRIP_START.minusHours(25).toInstant()), WINDOW)
    );
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private org.opentripplanner.ext.carpooling.model.CarpoolTrip trip() {
    return createSimpleTripWithTimes(OSLO_CENTER, OSLO_NORTH, TRIP_START, TRIP_END);
  }
}
