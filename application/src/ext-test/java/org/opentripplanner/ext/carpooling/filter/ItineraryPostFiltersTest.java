package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner._support.time.ZoneIds.UTC;
import static org.opentripplanner.ext.carpooling.CarpoolItineraryTestData.SERVICE_DAY;
import static org.opentripplanner.ext.carpooling.CarpoolItineraryTestData.driveItinerary;

import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class ItineraryPostFiltersTest {

  @Test
  void isValidItinerary_arriveByRequest_acceptedReturnsTrue() {
    // Trip arrives at 11:55, passenger requests arrive-by 12:00 — 5 minutes early.
    var request = new CarpoolingRequestBuilder()
      .withArriveBy(true)
      .withRequestedDateTime(ZonedDateTime.of(SERVICE_DAY, LocalTime.of(12, 0, 0), UTC).toInstant())
      .build();

    assertTrue(ItineraryPostFilters.defaults().isValidItinerary(driveItinerary(), request, null));
  }

  @Test
  void isValidItinerary_arriveByRequestTripArrivesTooLate_rejectedReturnsFalse() {
    // Trip arrives at 11:55, passenger requests arrive-by 11:00 — trip is too late.
    var request = new CarpoolingRequestBuilder()
      .withArriveBy(true)
      .withRequestedDateTime(ZonedDateTime.of(SERVICE_DAY, LocalTime.of(11, 0, 0), UTC).toInstant())
      .build();

    assertFalse(ItineraryPostFilters.defaults().isValidItinerary(driveItinerary(), request, null));
  }

  @Test
  void isValidItinerary_isNotAnArriveByRequest_acceptedReturnsTrue() {
    var request = new CarpoolingRequestBuilder().withArriveBy(false).build();

    assertTrue(ItineraryPostFilters.defaults().isValidItinerary(driveItinerary(), request, null));
  }

  @Test
  void isValidItinerary_departAfterRequest_tripDepartsOnTime_acceptedReturnsTrue() {
    // Itinerary departs at 11:00, passenger requests depart-after 11:00 — exactly on time.
    var request = new CarpoolingRequestBuilder()
      .withArriveBy(false)
      .withRequestedDateTime(ZonedDateTime.of(SERVICE_DAY, LocalTime.of(11, 0, 0), UTC).toInstant())
      .build();

    assertTrue(ItineraryPostFilters.defaults().isValidItinerary(driveItinerary(), request, null));
  }

  @Test
  void isValidItinerary_departAfterRequest_tripDepartsTooEarly_rejectedReturnsFalse() {
    // Itinerary departs at 11:00, passenger requests depart-after 11:30 — departed 30 min too early.
    var request = new CarpoolingRequestBuilder()
      .withArriveBy(false)
      .withRequestedDateTime(
        ZonedDateTime.of(SERVICE_DAY, LocalTime.of(11, 30, 0), UTC).toInstant()
      )
      .build();

    assertFalse(ItineraryPostFilters.defaults().isValidItinerary(driveItinerary(), request, null));
  }
}
