package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolItineraryTestData.at;
import static org.opentripplanner.ext.carpooling.CarpoolItineraryTestData.driveItinerary;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.arriveByDirect;

import org.junit.jupiter.api.Test;

/**
 * Smoke tests confirming {@link ItineraryPostFilters#defaults()} wires up
 * {@link TimeItineraryFilter}. Exhaustive time-boundary coverage lives in
 * {@code TimeItineraryFilterTest}.
 */
class ItineraryPostFiltersTest {

  @Test
  void defaults_acceptValidItinerary() {
    // Trip arrives 11:55; LAT 12:00 — comfortable margin.
    assertTrue(
      ItineraryPostFilters.defaults().isValidItinerary(driveItinerary(), arriveByDirect(at(12, 0)))
    );
  }

  @Test
  void defaults_rejectsItineraryOutsideWindow() {
    // Trip arrives 11:55; LAT 11:00 — far too late.
    assertFalse(
      ItineraryPostFilters.defaults().isValidItinerary(driveItinerary(), arriveByDirect(at(11, 0)))
    );
  }
}
