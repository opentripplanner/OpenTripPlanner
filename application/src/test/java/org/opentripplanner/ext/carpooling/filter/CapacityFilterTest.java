package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_SOUTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_WEST;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createDestinationStop;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createOriginStop;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createStop;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createTripWithCapacity;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createTripWithStops;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CapacityFilterTest {

  private CapacityFilter filter;

  @BeforeEach
  void setup() {
    filter = new CapacityFilter();
  }

  @Test
  void accepts_tripWithCapacity_returnsTrue() {
    var trip = createTripWithStops(OSLO_CENTER, List.of(), OSLO_NORTH);

    assertTrue(filter.accepts(trip, OSLO_EAST, OSLO_WEST));
  }

  @Test
  void accepts_tripAtFullCapacity_returnsTrue() {
    // CapacityFilter only checks configured capacity, not actual occupancy
    // Detailed capacity validation happens in the validator layer
    // All 4 seats taken
    var stop1 = createStop(0, 4);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1), OSLO_NORTH);

    // Filter accepts because trip has capacity configured (even if currently full)
    assertTrue(filter.accepts(trip, OSLO_EAST, OSLO_WEST));
  }

  @Test
  void accepts_tripWithOneOpenSeat_returnsTrue() {
    // 3 of 4 seats taken
    var stop1 = createStop(0, 3);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1), OSLO_NORTH);

    assertTrue(filter.accepts(trip, OSLO_EAST, OSLO_WEST));
  }

  @Test
  void accepts_zeroCapacityTrip_returnsFalse() {
    var stops = List.of(createOriginStop(OSLO_CENTER), createDestinationStop(OSLO_NORTH, 1));
    var trip = createTripWithCapacity(0, stops);

    assertFalse(filter.accepts(trip, OSLO_EAST, OSLO_WEST));
  }

  @Test
  void accepts_passengerCoordinatesIgnored() {
    // Filter only checks if ANY capacity exists, not position-specific
    var stops = List.of(createOriginStop(OSLO_CENTER), createDestinationStop(OSLO_NORTH, 1));
    var trip = createTripWithCapacity(2, stops);

    // Should accept regardless of passenger coordinates
    assertTrue(filter.accepts(trip, OSLO_SOUTH, OSLO_EAST));
    assertTrue(filter.accepts(trip, OSLO_NORTH, OSLO_SOUTH));
  }

  @Test
  void accepts_tripWithFluctuatingCapacity_checksOverallAvailability() {
    // 2 passengers
    var stop1 = createStop(0, 2);
    // Dropoff 2
    var stop2 = createStop(1, -2);
    // Pickup 1
    var stop3 = createStop(2, 1);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2, stop3), OSLO_NORTH);

    // At some point there's capacity (positions 0, 2+)
    assertTrue(filter.accepts(trip, OSLO_EAST, OSLO_WEST));
  }

  @Test
  void accepts_tripAlwaysAtCapacity_returnsTrue() {
    // CapacityFilter only checks configured capacity, not actual occupancy
    // Fill to capacity
    var stop1 = createStop(0, 4);
    // Drop 1
    var stop2 = createStop(1, -1);
    // Pick 1 (back to full)
    var stop3 = createStop(2, 1);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2, stop3), OSLO_NORTH);

    // Filter accepts because trip has capacity configured
    // The validator will determine if there's actual room for insertion
    assertTrue(filter.accepts(trip, OSLO_EAST, OSLO_WEST));
  }
}
