package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_WEST;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTrip;

import java.util.List;
import org.junit.jupiter.api.Test;

class TripPreFiltersTest {

  @Test
  void isCandidateTrip_allFiltersAccept_returnsTrue() {
    CarpoolTripFilter filter1 = (trip, request, searchWindow) -> true;
    CarpoolTripFilter filter2 = (trip, request, searchWindow) -> true;
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    var preFilter = new TripPreFilters(List.of(filter1, filter2));
    var request = new CarpoolingRequestBuilder()
      .withPassengerPickup(OSLO_EAST)
      .withPassengerDropoff(OSLO_WEST)
      .build();

    assertTrue(preFilter.isCandidateTrip(trip, request, null));
  }

  @Test
  void isCandidateTrip_oneFilterRejects_returnsFalse() {
    CarpoolTripFilter filter1 = (trip, request, searchWindow) -> true;
    CarpoolTripFilter filter2 = (trip, request, searchWindow) -> false;
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    var preFilter = new TripPreFilters(List.of(filter1, filter2));
    var request = new CarpoolingRequestBuilder()
      .withPassengerPickup(OSLO_EAST)
      .withPassengerDropoff(OSLO_WEST)
      .build();

    assertFalse(preFilter.isCandidateTrip(trip, request, null));
  }

  @Test
  void isCandidateTrip_shortCircuits_afterFirstRejection() {
    var filter3Called = new boolean[] { false };

    CarpoolTripFilter filter1 = (trip, request, searchWindow) -> true;
    CarpoolTripFilter filter2 = (trip, request, searchWindow) -> false;
    CarpoolTripFilter filter3 = (trip, request, searchWindow) -> {
      filter3Called[0] = true;
      return true;
    };
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var request = new CarpoolingRequestBuilder()
      .withPassengerPickup(OSLO_EAST)
      .withPassengerDropoff(OSLO_WEST)
      .build();

    var preFilter = new TripPreFilters(List.of(filter1, filter2, filter3));
    preFilter.isCandidateTrip(trip, request, null);

    assertFalse(filter3Called[0], "Filter3 should not have been called due to short-circuit");
  }

  @Test
  void isCandidateTrip_firstFilterRejects_doesNotCallOthers() {
    var filter2Called = new boolean[] { false };

    CarpoolTripFilter filter1 = (trip, request, searchWindow) -> false;
    CarpoolTripFilter filter2 = (trip, request, searchWindow) -> {
      filter2Called[0] = true;
      return true;
    };
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var request = new CarpoolingRequestBuilder()
      .withPassengerPickup(OSLO_EAST)
      .withPassengerDropoff(OSLO_WEST)
      .build();

    var preFilter = new TripPreFilters(List.of(filter1, filter2));
    preFilter.isCandidateTrip(trip, request, null);

    assertFalse(filter2Called[0], "Filter2 should not have been called due to short-circuit");
  }

  @Test
  void emptyTripPreFilters_acceptsAll() {
    var preFilter = new TripPreFilters(List.of());
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var request = new CarpoolingRequestBuilder()
      .withPassengerPickup(OSLO_EAST)
      .withPassengerDropoff(OSLO_WEST)
      .build();

    assertTrue(preFilter.isCandidateTrip(trip, request, null));
  }

  @Test
  void singleFilter_behavesCorrectly() {
    CarpoolTripFilter filter = (trip, request, searchWindow) -> true;

    var preFilter = new TripPreFilters(List.of(filter));
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var request = new CarpoolingRequestBuilder()
      .withPassengerPickup(OSLO_EAST)
      .withPassengerDropoff(OSLO_WEST)
      .build();

    assertTrue(preFilter.isCandidateTrip(trip, request, null));
  }
}
