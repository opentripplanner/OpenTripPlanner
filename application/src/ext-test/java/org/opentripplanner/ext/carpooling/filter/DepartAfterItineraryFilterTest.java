package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner._support.time.ZoneIds.UTC;
import static org.opentripplanner.ext.carpooling.CarpoolItineraryTestData.SERVICE_DAY;
import static org.opentripplanner.ext.carpooling.CarpoolItineraryTestData.driveItinerary;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByAccess;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByDirect;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByEgress;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterAccess;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterDirect;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterEgress;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterWithNoTime;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class DepartAfterItineraryFilterTest {

  // Itinerary departs at 11:00 UTC, arrives at 11:55 UTC (on SERVICE_DAY = 2020-02-02).
  static final Duration WINDOW = Duration.ofMinutes(30);
  static final DepartAfterItineraryFilter FILTER = new DepartAfterItineraryFilter();

  // ---------------------------------------------------------------------------
  // Filter does not act on these requests — always returns true
  // ---------------------------------------------------------------------------

  @Test
  void isValidItinerary_ignores_arriveByDirectRequest_returnsTrue() {
    // T=11:30 would trigger a lower-bound rejection for depart-after, but arrive-by is ignored.
    assertTrue(FILTER.isValidItinerary(driveItinerary(), arriveByDirect(at(11, 30)), WINDOW));
  }

  @Test
  void isValidItinerary_ignores_arriveByAccessRequest_returnsTrue() {
    assertTrue(FILTER.isValidItinerary(driveItinerary(), arriveByAccess(at(11, 30)), WINDOW));
  }

  @Test
  void isValidItinerary_ignores_arriveByEgressRequest_returnsTrue() {
    assertTrue(FILTER.isValidItinerary(driveItinerary(), arriveByEgress(at(11, 30)), WINDOW));
  }

  @Test
  void isValidItinerary_ignores_departAfterRequestWithNoTime_returnsTrue() {
    assertTrue(FILTER.isValidItinerary(driveItinerary(), departAfterWithNoTime(), WINDOW));
  }

  // ---------------------------------------------------------------------------
  // Depart After, direct routing — window: [T, T + searchWindow]
  // ---------------------------------------------------------------------------

  @Test
  void isValidItinerary_departAfterDirect_returnsTrue() {
    // Itinerary departs 30 min after T — comfortably within the T+30 min upper bound.
    assertTrue(FILTER.isValidItinerary(driveItinerary(), departAfterDirect(at(10, 30)), WINDOW));
  }

  @Test
  void isValidItinerary_departAfterDirect_itineraryDepartsExactlyAtT_returnsTrue() {
    assertTrue(FILTER.isValidItinerary(driveItinerary(), departAfterDirect(at(11, 0)), WINDOW));
  }

  @Test
  void isValidItinerary_departAfterDirect_itineraryDepartsBeforeT_returnsFalse() {
    // Itinerary departed 1 min before T.
    assertFalse(FILTER.isValidItinerary(driveItinerary(), departAfterDirect(at(11, 1)), WINDOW));
  }

  @Test
  void isValidItinerary_departAfterDirect_itineraryDepartsAtUpperBound_returnsTrue() {
    // T=10:30 → upper bound = 10:30 + 30 = 11:00 = itinerary departure.
    assertTrue(FILTER.isValidItinerary(driveItinerary(), departAfterDirect(at(10, 30)), WINDOW));
  }

  @Test
  void isValidItinerary_departAfterDirect_itineraryDepartsJustPastUpperBound_returnsFalse() {
    // T=10:29 → upper bound = 10:59 < 11:00.
    assertFalse(FILTER.isValidItinerary(driveItinerary(), departAfterDirect(at(10, 29)), WINDOW));
  }

  @Test
  void isValidItinerary_departAfterDirect_nullSearchWindow_returnsTrue() {
    // Without a search window the upper bound is not enforced.
    assertTrue(FILTER.isValidItinerary(driveItinerary(), departAfterDirect(at(9, 0)), null));
  }

  // ---------------------------------------------------------------------------
  // Depart After, access — same tight upper bound as direct
  // ---------------------------------------------------------------------------

  @Test
  void isValidItinerary_departAfterAccess_returnsTrue() {
    assertTrue(FILTER.isValidItinerary(driveItinerary(), departAfterAccess(at(10, 30)), WINDOW));
  }

  @Test
  void isValidItinerary_departAfterAccess_itineraryDepartsPastTightBound_returnsFalse() {
    // Access uses the same tight bound as direct — no egress slack.
    assertFalse(FILTER.isValidItinerary(driveItinerary(), departAfterAccess(at(10, 14)), WINDOW));
  }

  // ---------------------------------------------------------------------------
  // Depart After, egress — same upper bound as direct
  // ---------------------------------------------------------------------------

  @Test
  void isValidItinerary_departAfterEgress_returnsTrue() {
    // T=10:30 → upper bound = 11:00 = itinerary departure.
    assertTrue(FILTER.isValidItinerary(driveItinerary(), departAfterEgress(at(10, 30)), WINDOW));
  }

  @Test
  void isValidItinerary_departAfterEgress_itineraryPastUpperBound_returnsFalse() {
    // T = 25 h before the itinerary departure → upper = T + 30 min = far before itinerary start.
    var T = ZonedDateTime.of(SERVICE_DAY, LocalTime.of(11, 0), UTC).minusHours(25).toInstant();
    assertFalse(FILTER.isValidItinerary(driveItinerary(), departAfterEgress(T), WINDOW));
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static Instant at(int hour, int minute) {
    return ZonedDateTime.of(SERVICE_DAY, LocalTime.of(hour, minute), UTC).toInstant();
  }
}
