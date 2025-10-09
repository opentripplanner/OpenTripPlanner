package org.opentripplanner.ext.carpooling.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.*;
import static org.opentripplanner.ext.carpooling.TestFixtures.*;

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
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(), OSLO_NORTH);

    assertTrue(filter.accepts(trip, OSLO_EAST, OSLO_WEST));
  }

  @Test
  void accepts_tripAtFullCapacity_returnsTrue() {
    // CapacityFilter only checks configured capacity, not actual occupancy
    // Detailed capacity validation happens in the validator layer
    var stop1 = createStop(0, +4); // All 4 seats taken
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1), OSLO_NORTH);

    // Filter accepts because trip has capacity configured (even if currently full)
    assertTrue(filter.accepts(trip, OSLO_EAST, OSLO_WEST));
  }

  @Test
  void accepts_tripWithOneOpenSeat_returnsTrue() {
    var stop1 = createStop(0, +3); // 3 of 4 seats taken
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1), OSLO_NORTH);

    assertTrue(filter.accepts(trip, OSLO_EAST, OSLO_WEST));
  }

  @Test
  void accepts_zeroCapacityTrip_returnsFalse() {
    var trip = createTripWithCapacity(0, OSLO_CENTER, List.of(), OSLO_NORTH);

    assertFalse(filter.accepts(trip, OSLO_EAST, OSLO_WEST));
  }

  @Test
  void accepts_passengerCoordinatesIgnored() {
    // Filter only checks if ANY capacity exists, not position-specific
    var trip = createTripWithCapacity(2, OSLO_CENTER, List.of(), OSLO_NORTH);

    // Should accept regardless of passenger coordinates
    assertTrue(filter.accepts(trip, OSLO_SOUTH, OSLO_EAST));
    assertTrue(filter.accepts(trip, OSLO_NORTH, OSLO_SOUTH));
  }

  @Test
  void accepts_tripWithFluctuatingCapacity_checksOverallAvailability() {
    var stop1 = createStop(0, +2); // 2 passengers
    var stop2 = createStop(1, -2); // Dropoff 2
    var stop3 = createStop(2, +1); // Pickup 1
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1, stop2, stop3), OSLO_NORTH);

    // At some point there's capacity (positions 0, 2+)
    assertTrue(filter.accepts(trip, OSLO_EAST, OSLO_WEST));
  }

  @Test
  void accepts_tripAlwaysAtCapacity_returnsTrue() {
    // CapacityFilter only checks configured capacity, not actual occupancy
    var stop1 = createStop(0, +4); // Fill to capacity
    var stop2 = createStop(1, -1); // Drop 1
    var stop3 = createStop(2, +1); // Pick 1 (back to full)
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1, stop2, stop3), OSLO_NORTH);

    // Filter accepts because trip has capacity configured
    // The validator will determine if there's actual room for insertion
    assertTrue(filter.accepts(trip, OSLO_EAST, OSLO_WEST));
  }
}
