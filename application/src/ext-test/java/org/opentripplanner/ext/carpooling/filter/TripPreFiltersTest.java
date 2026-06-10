package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_WEST;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTrip;
import static org.opentripplanner.ext.carpooling.CarpoolingRequestTestData.directRequest;

import java.util.List;
import org.junit.jupiter.api.Test;

class TripPreFiltersTest {

  @Test
  void anyRejectingFilter_failsTheTrip() {
    CarpoolTripFilter accept = (trip, request) -> true;
    CarpoolTripFilter reject = (trip, request) -> false;
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var request = directRequest(OSLO_EAST, OSLO_WEST);

    assertTrue(new TripPreFilters(List.of(accept, accept)).isCandidateTrip(trip, request));
    assertFalse(new TripPreFilters(List.of(accept, reject)).isCandidateTrip(trip, request));
  }

  @Test
  void noFilters_acceptsAll() {
    var preFilter = new TripPreFilters(List.of());

    assertTrue(
      preFilter.isCandidateTrip(
        createSimpleTrip(OSLO_CENTER, OSLO_NORTH),
        directRequest(OSLO_EAST, OSLO_WEST)
      )
    );
  }
}
