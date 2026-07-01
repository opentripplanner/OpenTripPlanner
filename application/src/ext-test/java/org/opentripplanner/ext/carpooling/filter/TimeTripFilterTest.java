package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTripWithTimes;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByAccess;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByDirect;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByEgress;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterAccess;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterDirect;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterEgress;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterWithNoTime;

import java.time.Instant;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;

/**
 * Trip runs from 10:00+01:00 to 11:00+01:00. Slack constants used in the filter:
 * {@code W = DEFAULT_MAX_WALK_TIME = 15 min}, {@code T = MAX_TOTAL_TRAVEL_TIME = 24 h}.
 * The search window is {@code DEFAULT_SEARCH_WINDOW = 30 min}.
 * <p>
 * Coverage strategy: direct exercises the {@code W}-slack branches; access and egress have tests
 * only on the cells where they use the {@code T} fallback instead of {@code W} (the cells where
 * their behaviour is unique).
 */
class TimeTripFilterTest {

  private final TimeTripFilter filter = new TimeTripFilter();

  private static final ZonedDateTime TRIP_START = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
  private static final ZonedDateTime TRIP_END = TRIP_START.plusHours(1);

  @Test
  void noRequestedDateTime_returnsTrue() {
    assertTrue(filter.isCandidateTrip(trip(), departAfterWithNoTime()));
  }

  // ===========================================================================
  // Depart-after
  // ===========================================================================

  @Test
  void departAfterDirect_within_returnsTrue() {
    // EDT 20 min before tripStart.
    assertTrue(filter.isCandidateTrip(trip(), departAfterDirect(at(-20))));
  }

  @Test
  void departAfterDirect_tripEndedBeforeEDT_returnsFalse() {
    // EDT 90 min after tripStart — tripEnd (= +60) is before EDT.
    assertFalse(filter.isCandidateTrip(trip(), departAfterDirect(at(90))));
  }

  @Test
  void departAfterDirect_tripStartAtLDTPlusW_returnsTrue() {
    // EDT = tripStart − 45 min → LDT + W = tripStart, boundary accept.
    assertTrue(filter.isCandidateTrip(trip(), departAfterDirect(at(-45))));
  }

  @Test
  void departAfterDirect_tripStartPastLDTPlusW_returnsFalse() {
    // EDT = tripStart − 46 min → one minute past LDT + W.
    assertFalse(filter.isCandidateTrip(trip(), departAfterDirect(at(-46))));
  }

  @Test
  void departAfterAccess_tripStartPastLDTPlusW_returnsFalse() {
    // Access uses W on the LDT side, same as direct — no T fallback here.
    assertFalse(filter.isCandidateTrip(trip(), departAfterAccess(at(-46))));
  }

  @Test
  void departAfterEgress_within_T_returnsTrue() {
    // Past direct/access LDT + W, but well within LDT + T (24 h fallback).
    assertTrue(filter.isCandidateTrip(trip(), departAfterEgress(at(-120))));
  }

  @Test
  void departAfterEgress_pastLDTPlusT_returnsFalse() {
    // EDT = tripStart − (24 h + 31 min) → LDT + T = tripStart − 1 min.
    assertFalse(
      filter.isCandidateTrip(
        trip(),
        departAfterEgress(TRIP_START.minusHours(24).minusMinutes(31).toInstant())
      )
    );
  }

  // ===========================================================================
  // Arrive-by
  // ===========================================================================

  @Test
  void arriveByDirect_within_returnsTrue() {
    assertTrue(
      filter.isCandidateTrip(trip(), arriveByDirect(TRIP_END.plusMinutes(30).toInstant()))
    );
  }

  @Test
  void arriveByDirect_tripStartAtLAT_returnsTrue() {
    // LAT == tripStart, boundary accept (isAfter is strict).
    assertTrue(filter.isCandidateTrip(trip(), arriveByDirect(TRIP_START.toInstant())));
  }

  @Test
  void arriveByDirect_tripStartAfterLAT_returnsFalse() {
    assertFalse(
      filter.isCandidateTrip(trip(), arriveByDirect(TRIP_START.minusMinutes(1).toInstant()))
    );
  }

  @Test
  void arriveByDirect_tripEndsBeforeEATMinusW_returnsFalse() {
    // LAT 3 h after tripEnd → EAT − W is 135 min after tripEnd, tripEnd is way before.
    assertFalse(filter.isCandidateTrip(trip(), arriveByDirect(TRIP_END.plusHours(3).toInstant())));
  }

  @Test
  void arriveByAccess_tripEndsWithinEATMinusT_returnsTrue() {
    // Same input that fails for direct (above) — access uses the 24h T fallback instead of W.
    assertTrue(filter.isCandidateTrip(trip(), arriveByAccess(TRIP_END.plusHours(3).toInstant())));
  }

  @Test
  void arriveByAccess_tripEndsBeforeEATMinusT_returnsFalse() {
    // LAT 25 h after tripEnd → EAT − T = 30 min after tripEnd, reject as too early.
    assertFalse(filter.isCandidateTrip(trip(), arriveByAccess(TRIP_END.plusHours(25).toInstant())));
  }

  @Test
  void arriveByEgress_tripStartAfterLAT_returnsFalse() {
    // Egress uses no slack on the LAT side — same as direct.
    assertFalse(
      filter.isCandidateTrip(trip(), arriveByEgress(TRIP_START.minusMinutes(1).toInstant()))
    );
  }

  /** Instant expressed as minutes relative to TRIP_START (positive = after tripStart). */
  private static Instant at(int minutesFromTripStart) {
    return TRIP_START.plusMinutes(minutesFromTripStart).toInstant();
  }

  private static CarpoolTrip trip() {
    return createSimpleTripWithTimes(OSLO_CENTER, OSLO_NORTH, TRIP_START, TRIP_END);
  }
}
