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
import org.opentripplanner.ext.carpooling.RoutableCarpoolTripTestData;

class TripPreFiltersTest {

  @Test
  void anyRejectingFilter_failsTheTrip() {
    CarpoolTripFilter accept = (trip, passenger) -> true;
    CarpoolTripFilter reject = (trip, passenger) -> false;
    var trip = RoutableCarpoolTripTestData.withDummyVertices(
      createSimpleTrip(OSLO_CENTER, OSLO_NORTH)
    );
    var passenger = new SnappedPassenger(directRequest(OSLO_EAST, OSLO_WEST), null, null);

    assertTrue(new TripPreFilters(List.of(accept, accept)).isCandidateTrip(trip, passenger));
    assertFalse(new TripPreFilters(List.of(accept, reject)).isCandidateTrip(trip, passenger));
  }

  @Test
  void noFilters_acceptsAll() {
    var preFilter = new TripPreFilters(List.of());

    assertTrue(
      preFilter.isCandidateTrip(
        RoutableCarpoolTripTestData.withDummyVertices(createSimpleTrip(OSLO_CENTER, OSLO_NORTH)),
        new SnappedPassenger(directRequest(OSLO_EAST, OSLO_WEST), null, null)
      )
    );
  }
}
