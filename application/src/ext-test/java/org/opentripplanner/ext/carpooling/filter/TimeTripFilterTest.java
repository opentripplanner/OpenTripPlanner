package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTripWithTimes;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createStopAt;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithStops;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByAccess;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByDirect;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByEgress;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterAccess;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterDirect;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterEgress;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterWithNoTime;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.ext.carpooling.CarpoolGraphPathBuilder;
import org.opentripplanner.ext.carpooling.RoutableCarpoolTripTestData;
import org.opentripplanner.ext.carpooling.routing.RoutableCarpoolTrip;
import org.opentripplanner.ext.carpooling.util.CarReachableVertexSnapper.SnapResult;

/**
 * The trip starts at 10:00+01:00 and its one leg is routed to 50 minutes; with the destination's
 * default 10-minute deviation budget that puts {@code tripEnd} at 11:00. The passenger walks 5
 * minutes to the pickup and 5 from the dropoff ({@code Wp}, {@code Wd}); {@code J} is 24 h and the
 * search window 30 min (see {@code CarpoolingRequestTestData}).
 */
class TimeTripFilterTest {

  private final TimeTripFilter filter = new TimeTripFilter();

  private static final ZonedDateTime TRIP_START = ZonedDateTime.parse("2024-01-15T10:00:00+01:00");
  private static final ZonedDateTime TRIP_END = TRIP_START.plusHours(1);
  private static final Duration WALK = Duration.ofMinutes(5);

  @Test
  void noRequestedDateTime_returnsTrue() {
    assertTrue(accepts(trip(), departAfterWithNoTime()));
  }

  // ===========================================================================
  // Depart-after
  // ===========================================================================

  @Test
  void departAfterDirect_within_returnsTrue() {
    assertTrue(accepts(trip(), departAfterDirect(at(-20))));
  }

  @Test
  void departAfterDirect_tripEndAtEDTPlusWp_returnsTrue() {
    // EDT + Wp = tripEnd, boundary accept (isBefore is strict).
    assertTrue(accepts(trip(), departAfterDirect(at(55))));
  }

  @Test
  void departAfterDirect_tripEndBeforeEDTPlusWp_returnsFalse() {
    assertFalse(accepts(trip(), departAfterDirect(at(56))));
  }

  @Test
  void departAfterDirect_tripStartAtLDTPlusWp_returnsTrue() {
    // EDT = tripStart − 35 min → LDT + Wp = tripStart, boundary accept.
    assertTrue(accepts(trip(), departAfterDirect(at(-35))));
  }

  @Test
  void departAfterDirect_tripStartPastLDTPlusWp_returnsFalse() {
    assertFalse(accepts(trip(), departAfterDirect(at(-36))));
  }

  @Test
  void departAfterAccess_tripStartPastLDTPlusWp_returnsFalse() {
    // Access is bounded like direct on the LDT side.
    assertFalse(accepts(trip(), departAfterAccess(at(-36))));
  }

  @Test
  void departAfterEgress_withinJ_returnsTrue() {
    // Past direct/access LDT + Wp, but well within LDT + J − Wd.
    assertTrue(accepts(trip(), departAfterEgress(at(-120))));
  }

  @Test
  void departAfterEgress_tripStartAtLDTPlusJMinusWd_returnsTrue() {
    // EDT = tripStart − (24 h + 25 min) → LDT + J − Wd = tripStart, boundary accept.
    assertTrue(accepts(trip(), departAfterEgress(at(-24 * 60 - 25))));
  }

  @Test
  void departAfterEgress_tripStartPastLDTPlusJMinusWd_returnsFalse() {
    assertFalse(accepts(trip(), departAfterEgress(at(-24 * 60 - 26))));
  }

  @Test
  void departAfterEgress_tripEndBeforeEDT_returnsFalse() {
    // The egress pickup is at a transit stop: no walk moves the EDT side.
    assertTrue(accepts(trip(), departAfterEgress(at(60))));
    assertFalse(accepts(trip(), departAfterEgress(at(61))));
  }

  // ===========================================================================
  // Arrive-by
  // ===========================================================================

  @Test
  void arriveByDirect_within_returnsTrue() {
    assertTrue(accepts(trip(), arriveByDirect(TRIP_END.plusMinutes(30).toInstant())));
  }

  @Test
  void arriveByDirect_tripStartAtLATMinusWd_returnsTrue() {
    // LAT − Wd = tripStart, boundary accept (isAfter is strict).
    assertTrue(accepts(trip(), arriveByDirect(at(5))));
  }

  @Test
  void arriveByDirect_tripStartAfterLATMinusWd_returnsFalse() {
    assertFalse(accepts(trip(), arriveByDirect(at(4))));
  }

  @Test
  void arriveByDirect_tripEndAtEATMinusWd_returnsTrue() {
    // LAT = tripEnd + 35 min → EAT − Wd = tripEnd, boundary accept.
    assertTrue(accepts(trip(), arriveByDirect(TRIP_END.plusMinutes(35).toInstant())));
  }

  @Test
  void arriveByDirect_tripEndBeforeEATMinusWd_returnsFalse() {
    assertFalse(accepts(trip(), arriveByDirect(TRIP_END.plusMinutes(36).toInstant())));
  }

  @Test
  void arriveByAccess_tripEndsWithinEATMinusJ_returnsTrue() {
    // Same input that fails for direct (above): access is bounded by J instead.
    assertTrue(accepts(trip(), arriveByAccess(TRIP_END.plusMinutes(36).toInstant())));
  }

  @Test
  void arriveByAccess_tripEndBeforeEATMinusJPlusWp_returnsFalse() {
    // LAT = tripEnd + 24 h 26 min → EAT − J + Wp = tripEnd + 1 min.
    assertTrue(accepts(trip(), arriveByAccess(TRIP_END.plusHours(24).plusMinutes(25).toInstant())));
    assertFalse(
      accepts(trip(), arriveByAccess(TRIP_END.plusHours(24).plusMinutes(26).toInstant()))
    );
  }

  @Test
  void arriveByAccess_tripStartAfterLAT_returnsFalse() {
    // The access pickup precedes the arrival, with no walk in between.
    assertTrue(accepts(trip(), arriveByAccess(at(0))));
    assertFalse(accepts(trip(), arriveByAccess(at(-1))));
  }

  @Test
  void arriveByEgress_tripStartAfterLATMinusWd_returnsFalse() {
    // Egress is bounded like direct on the LAT side.
    assertFalse(accepts(trip(), arriveByEgress(at(4))));
  }

  // ===========================================================================
  // The trip's end, as the insertion evaluation times it
  // ===========================================================================

  @Test
  void aRouteSlowerThanTheScheduleEndsLater() {
    // Scheduled to arrive at 10:30, but routed to 50 minutes: a pickup at 10:55 is still possible.
    var trip = createSimpleTripWithTimes(
      OSLO_CENTER,
      OSLO_NORTH,
      TRIP_START,
      TRIP_START.plusMinutes(30)
    );
    var routed = RoutableCarpoolTripTestData.withRoutedLegs(trip, Duration.ofMinutes(50));

    assertTrue(accepts(routed, departAfterDirect(at(50))));
  }

  @Test
  void aRouteFasterThanTheScheduleEndsEarlier() {
    // Scheduled to arrive at 11:00, but routed to 20 minutes: no pickup can happen after 10:30.
    var routed = RoutableCarpoolTripTestData.withRoutedLegs(
      createSimpleTripWithTimes(OSLO_CENTER, OSLO_NORTH, TRIP_START, TRIP_END),
      Duration.ofMinutes(20)
    );

    assertFalse(accepts(routed, departAfterDirect(at(30))));
  }

  @Test
  void tripEndCountsTheDwellAtEveryIntermediateStop() {
    var trip = createTripWithStops(
      OSLO_CENTER,
      List.of(createStopAt(OSLO_EAST, Duration.ofMinutes(20))),
      OSLO_NORTH,
      Duration.ofMinutes(10)
    );
    var routed = RoutableCarpoolTripTestData.withRoutedLegs(
      trip,
      Duration.ofMinutes(20),
      Duration.ofMinutes(20)
    );

    assertEquals(
      trip
        .startTime()
        .toInstant()
        .plus(Duration.ofMinutes(20 + 20 + 1 + 10)),
      TimeTripFilter.tripEnd(routed, Duration.ofMinutes(1))
    );
  }

  /** Instant expressed as minutes relative to TRIP_START (positive = after tripStart). */
  private static Instant at(int minutesFromTripStart) {
    return TRIP_START.plusMinutes(minutesFromTripStart).toInstant();
  }

  private static RoutableCarpoolTrip trip() {
    return RoutableCarpoolTripTestData.withRoutedLegs(
      createSimpleTripWithTimes(OSLO_CENTER, OSLO_NORTH, TRIP_START, TRIP_END),
      Duration.ofMinutes(50)
    );
  }

  /** The request's passenger, snapped with a {@link #WALK} to the pickup and from the dropoff. */
  private boolean accepts(RoutableCarpoolTrip trip, CarpoolingRequest request) {
    var pickup = request.isEgressRequest() ? null : snap();
    var dropoff = request.isAccessRequest() ? null : snap();
    return filter.isCandidateTrip(trip, new SnappedPassenger(request, pickup, dropoff));
  }

  private static SnapResult snap() {
    var walk = CarpoolGraphPathBuilder.createGraphPath(WALK);
    return new SnapResult(walk.states.getLast().getVertex(), walk);
  }
}
