package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_WEST;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createDestinationStop;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createOriginStop;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createSimpleTrip;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createTripWithCapacity;

import java.util.List;
import org.junit.jupiter.api.Test;

class FilterChainTest {

  @Test
  void accepts_allFiltersAccept_returnsTrue() {
    TripFilter filter1 = (trip, pickup, dropoff) -> true;
    TripFilter filter2 = (trip, pickup, dropoff) -> true;
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    var chain = new FilterChain(List.of(filter1, filter2));

    assertTrue(chain.accepts(trip, OSLO_EAST, OSLO_WEST));
  }

  @Test
  void accepts_oneFilterRejects_returnsFalse() {
    TripFilter filter1 = (trip, pickup, dropoff) -> true;
    TripFilter filter2 = (trip, pickup, dropoff) -> false;
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    var chain = new FilterChain(List.of(filter1, filter2));

    assertFalse(chain.accepts(trip, OSLO_EAST, OSLO_WEST));
  }

  @Test
  void accepts_shortCircuits_afterFirstRejection() {
    var filter3Called = new boolean[] { false };

    TripFilter filter1 = (trip, pickup, dropoff) -> true;
    TripFilter filter2 = (trip, pickup, dropoff) -> false;
    TripFilter filter3 = (trip, pickup, dropoff) -> {
      filter3Called[0] = true;
      return true;
    };
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    var chain = new FilterChain(List.of(filter1, filter2, filter3));
    chain.accepts(trip, OSLO_EAST, OSLO_WEST);

    assertFalse(filter3Called[0], "Filter3 should not have been called due to short-circuit");
  }

  @Test
  void accepts_firstFilterRejects_doesNotCallOthers() {
    var filter2Called = new boolean[] { false };

    TripFilter filter1 = (trip, pickup, dropoff) -> false;
    TripFilter filter2 = (trip, pickup, dropoff) -> {
      filter2Called[0] = true;
      return true;
    };
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    var chain = new FilterChain(List.of(filter1, filter2));
    chain.accepts(trip, OSLO_EAST, OSLO_WEST);

    assertFalse(filter2Called[0], "Filter2 should not have been called due to short-circuit");
  }

  @Test
  void standard_includesAllStandardFilters() {
    var chain = FilterChain.standard();

    // Should contain CapacityFilter and DirectionalCompatibilityFilter
    // Verify by testing behavior with a trip that has no capacity
    var stops = List.of(createOriginStop(OSLO_CENTER), createDestinationStop(OSLO_NORTH, 1));
    var emptyTrip = createTripWithCapacity(0, stops);

    // Should reject due to capacity filter
    assertFalse(chain.accepts(emptyTrip, OSLO_EAST, OSLO_WEST));
  }

  @Test
  void standard_checksDirectionalCompatibility() {
    var chain = FilterChain.standard();

    // Trip going north, passenger going south
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Should reject due to directional filter
    assertFalse(chain.accepts(trip, OSLO_EAST, OSLO_CENTER));
  }

  @Test
  void emptyChain_acceptsAll() {
    var chain = new FilterChain(List.of());
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Empty chain accepts everything
    assertTrue(chain.accepts(trip, OSLO_EAST, OSLO_WEST));
  }

  @Test
  void singleFilter_behavesCorrectly() {
    TripFilter filter = (trip, pickup, dropoff) -> true;

    var chain = new FilterChain(List.of(filter));
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    assertTrue(chain.accepts(trip, OSLO_EAST, OSLO_WEST));
  }
}
