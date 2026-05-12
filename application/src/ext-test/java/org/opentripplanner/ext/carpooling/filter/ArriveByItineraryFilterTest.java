package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner._support.time.ZoneIds.UTC;
import static org.opentripplanner.ext.carpooling.CarpoolItineraryTestData.SERVICE_DAY;
import static org.opentripplanner.ext.carpooling.CarpoolItineraryTestData.driveItinerary;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByAccess;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByDirect;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByEgress;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByWithNoTime;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterAccess;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterDirect;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterEgress;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class ArriveByItineraryFilterTest {

  // Itinerary departs at 11:00 UTC, arrives at 11:55 UTC (on SERVICE_DAY = 2020-02-02).
  static final ArriveByItineraryFilter FILTER = new ArriveByItineraryFilter();

  // ---------------------------------------------------------------------------
  // Filter does not act on these requests — always returns true
  // ---------------------------------------------------------------------------

  @Test
  void isValidItinerary_ignores_departAfterDirectRequest_returnsTrue() {
    // T=11:00 would trigger an arrive-by rejection, but depart-after is ignored.
    assertTrue(FILTER.isValidItinerary(driveItinerary(), departAfterDirect(at(11, 0)), null));
  }

  @Test
  void isValidItinerary_ignores_departAfterAccessRequest_returnsTrue() {
    assertTrue(FILTER.isValidItinerary(driveItinerary(), departAfterAccess(at(11, 0)), null));
  }

  @Test
  void isValidItinerary_ignores_departAfterEgressRequest_returnsTrue() {
    assertTrue(FILTER.isValidItinerary(driveItinerary(), departAfterEgress(at(11, 0)), null));
  }

  @Test
  void isValidItinerary_ignores_arriveByRequestWithNoTime_returnsTrue() {
    assertTrue(FILTER.isValidItinerary(driveItinerary(), arriveByWithNoTime(), null));
  }

  // ---------------------------------------------------------------------------
  // Arrive By, direct routing — deadline: itinerary.endTime <= T
  // ---------------------------------------------------------------------------

  @Test
  void isValidItinerary_arriveByDirect_returnsTrue() {
    // Itinerary arrives at 11:55, deadline 12:00 — comfortably before.
    assertTrue(FILTER.isValidItinerary(driveItinerary(), arriveByDirect(at(12, 0)), null));
  }

  @Test
  void isValidItinerary_arriveByDirect_itineraryArrivesAtDeadline_returnsTrue() {
    assertTrue(FILTER.isValidItinerary(driveItinerary(), arriveByDirect(at(11, 55)), null));
  }

  @Test
  void isValidItinerary_arriveByDirect_itineraryArrivesAfterDeadline_returnsFalse() {
    // Itinerary arrives at 11:55, deadline 11:54 — one minute too late.
    assertFalse(FILTER.isValidItinerary(driveItinerary(), arriveByDirect(at(11, 54)), null));
  }

  @Test
  void isValidItinerary_arriveByDirect_searchWindowNotUsed_returnsTrue() {
    // Search window must not affect arrive-by filtering.
    assertTrue(
      FILTER.isValidItinerary(driveItinerary(), arriveByDirect(at(12, 0)), Duration.ofMinutes(30))
    );
  }

  // ---------------------------------------------------------------------------
  // Arrive By, access — same tight deadline as direct
  // ---------------------------------------------------------------------------

  @Test
  void isValidItinerary_arriveByAccess_returnsTrue() {
    assertTrue(FILTER.isValidItinerary(driveItinerary(), arriveByAccess(at(12, 0)), null));
  }

  @Test
  void isValidItinerary_arriveByAccess_itineraryArrivesAfterDeadline_returnsFalse() {
    // Access uses the same tight deadline as direct — no egress slack.
    assertFalse(FILTER.isValidItinerary(driveItinerary(), arriveByAccess(at(11, 54)), null));
  }

  // ---------------------------------------------------------------------------
  // Arrive By, egress — deadline extended by 24 h
  // ---------------------------------------------------------------------------

  @Test
  void isValidItinerary_arriveByEgress_returnsTrue() {
    // T=11:00 → direct deadline = 11:00 < 11:55 (would reject for direct), but egress deadline =
    // next day 11:00 > 11:55.
    assertTrue(FILTER.isValidItinerary(driveItinerary(), arriveByEgress(at(11, 0)), null));
  }

  @Test
  void isValidItinerary_arriveByEgress_itineraryArrivesAfterEgressDeadline_returnsFalse() {
    // T = 25 h before the itinerary arrival → egress deadline = T + 24 h = 1 h before arrival.
    var T = ZonedDateTime.of(SERVICE_DAY, LocalTime.of(11, 55), UTC).minusHours(25).toInstant();
    assertFalse(FILTER.isValidItinerary(driveItinerary(), arriveByEgress(T), null));
  }

  // ---------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------

  private static Instant at(int hour, int minute) {
    return ZonedDateTime.of(SERVICE_DAY, LocalTime.of(hour, minute), UTC).toInstant();
  }
}
