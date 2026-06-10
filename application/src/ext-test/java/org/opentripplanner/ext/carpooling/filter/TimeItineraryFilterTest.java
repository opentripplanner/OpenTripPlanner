package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolItineraryTestData.at;
import static org.opentripplanner.ext.carpooling.CarpoolItineraryTestData.driveItinerary;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByDirect;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterDirect;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.departAfterWithNoTime;

import org.junit.jupiter.api.Test;

/**
 * Itinerary departs at 11:00 UTC, arrives at 11:55 UTC on {@code SERVICE_DAY}. The filter does
 * not read {@code accessOrEgress}, so direct/access/egress tests would all exercise the same code
 * path — covering direct alone is sufficient.
 */
class TimeItineraryFilterTest {

  private static final TimeItineraryFilter FILTER = new TimeItineraryFilter();

  @Test
  void noRequestedDateTime_returnsTrue() {
    assertTrue(FILTER.isValidItinerary(driveItinerary(), departAfterWithNoTime()));
  }

  // ===========================================================================
  // Depart-after — itinerary startTime must lie in [EDT, LDT].
  // ===========================================================================

  @Test
  void departAfter_itineraryDepartsAtEDT_returnsTrue() {
    assertTrue(FILTER.isValidItinerary(driveItinerary(), departAfterDirect(at(11, 0))));
  }

  @Test
  void departAfter_itineraryDepartsBeforeEDT_returnsFalse() {
    assertFalse(FILTER.isValidItinerary(driveItinerary(), departAfterDirect(at(11, 1))));
  }

  @Test
  void departAfter_itineraryDepartsAtLDT_returnsTrue() {
    // EDT 10:30 + searchWindow 30 min = LDT 11:00 = itinerary departure.
    assertTrue(FILTER.isValidItinerary(driveItinerary(), departAfterDirect(at(10, 30))));
  }

  @Test
  void departAfter_itineraryDepartsPastLDT_returnsFalse() {
    // EDT 10:29 → LDT 10:59, one minute before itinerary departure.
    assertFalse(FILTER.isValidItinerary(driveItinerary(), departAfterDirect(at(10, 29))));
  }

  // ===========================================================================
  // Arrive-by — itinerary endTime must lie in [EAT, LAT].
  // ===========================================================================

  @Test
  void arriveBy_itineraryArrivesAtLAT_returnsTrue() {
    assertTrue(FILTER.isValidItinerary(driveItinerary(), arriveByDirect(at(11, 55))));
  }

  @Test
  void arriveBy_itineraryArrivesAfterLAT_returnsFalse() {
    assertFalse(FILTER.isValidItinerary(driveItinerary(), arriveByDirect(at(11, 54))));
  }

  @Test
  void arriveBy_itineraryArrivesAtEAT_returnsTrue() {
    // LAT 12:25 → EAT 11:55 = itinerary arrival.
    assertTrue(FILTER.isValidItinerary(driveItinerary(), arriveByDirect(at(12, 25))));
  }

  @Test
  void arriveBy_itineraryArrivesBeforeEAT_returnsFalse() {
    // LAT 12:26 → EAT 11:56, one minute after itinerary arrival.
    assertFalse(FILTER.isValidItinerary(driveItinerary(), arriveByDirect(at(12, 26))));
  }
}
