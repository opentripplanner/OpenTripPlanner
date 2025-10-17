package org.opentripplanner.ext.carpooling.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.*;
import static org.opentripplanner.ext.carpooling.TestFixtures.*;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for capacity checking methods on {@link CarpoolTrip}.
 */
class CarpoolTripCapacityTest {

  @Test
  void getPassengerCountAtPosition_noStops_allZeros() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    assertEquals(0, trip.getPassengerCountAtPosition(0)); // Boarding
    assertEquals(0, trip.getPassengerCountAtPosition(1)); // Beyond stops
  }

  @Test
  void getPassengerCountAtPosition_onePickupStop_incrementsAtStop() {
    var stop1 = createStop(0, +1); // Pickup 1 passenger
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1), OSLO_NORTH);

    assertEquals(0, trip.getPassengerCountAtPosition(0)); // Before stop
    assertEquals(1, trip.getPassengerCountAtPosition(1)); // After stop
    assertEquals(0, trip.getPassengerCountAtPosition(2)); // Alighting
  }

  @Test
  void getPassengerCountAtPosition_pickupAndDropoff_incrementsThenDecrements() {
    var stop1 = createStop(0, +2); // Pickup 2 passengers
    var stop2 = createStop(1, -1); // Dropoff 1 passenger
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);

    assertEquals(0, trip.getPassengerCountAtPosition(0)); // Before any stops
    assertEquals(2, trip.getPassengerCountAtPosition(1)); // After first pickup
    assertEquals(1, trip.getPassengerCountAtPosition(2)); // After dropoff
    assertEquals(0, trip.getPassengerCountAtPosition(3)); // Alighting
  }

  @Test
  void getPassengerCountAtPosition_multipleStops_cumulativeCount() {
    var stop1 = createStop(0, +1);
    var stop2 = createStop(1, +2);
    var stop3 = createStop(2, -1);
    var stop4 = createStop(3, +1);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2, stop3, stop4), OSLO_NORTH);

    assertEquals(0, trip.getPassengerCountAtPosition(0));
    assertEquals(1, trip.getPassengerCountAtPosition(1)); // 0 + 1
    assertEquals(3, trip.getPassengerCountAtPosition(2)); // 1 + 2
    assertEquals(2, trip.getPassengerCountAtPosition(3)); // 3 - 1
    assertEquals(3, trip.getPassengerCountAtPosition(4)); // 2 + 1
    assertEquals(0, trip.getPassengerCountAtPosition(5)); // Alighting
  }

  @Test
  void getPassengerCountAtPosition_negativePosition_throwsException() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    assertThrows(IllegalArgumentException.class, () -> trip.getPassengerCountAtPosition(-1));
  }

  @Test
  void hasCapacityForInsertion_noPassengers_hasCapacity() {
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(), OSLO_NORTH);

    assertTrue(trip.hasCapacityForInsertion(1, 2, 1));
    assertTrue(trip.hasCapacityForInsertion(1, 2, 4)); // Can fit all 4 seats
  }

  @Test
  void hasCapacityForInsertion_fullCapacity_noCapacity() {
    var stop1 = createStop(0, +4); // Fill all 4 seats
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1), OSLO_NORTH);

    // No room for additional passenger after stop 1
    assertFalse(trip.hasCapacityForInsertion(2, 3, 1));
  }

  @Test
  void hasCapacityForInsertion_partialCapacity_hasCapacityForOne() {
    var stop1 = createStop(0, +3); // 3 of 4 seats taken
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1), OSLO_NORTH);

    assertTrue(trip.hasCapacityForInsertion(2, 3, 1)); // Room for 1
    assertFalse(trip.hasCapacityForInsertion(2, 3, 2)); // No room for 2
  }

  @Test
  void hasCapacityForInsertion_acrossMultiplePositions_checksAll() {
    var stop1 = createStop(0, +2);
    var stop2 = createStop(1, +1); // Total 3 passengers at position 2
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);

    // Range 1-3 includes position with 3 passengers, so only 1 seat available
    assertTrue(trip.hasCapacityForInsertion(1, 3, 1));
    assertFalse(trip.hasCapacityForInsertion(1, 3, 2));
  }

  @Test
  void hasCapacityForInsertion_rangeBeforeStop_usesInitialCapacity() {
    var stop1 = createStop(0, +4); // Fill capacity at position 1
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1), OSLO_NORTH);

    // Pickup at position 1, dropoff at position 1 - only checks capacity at boarding (position 0)
    // At boarding there are no passengers yet, so we have full capacity
    assertTrue(trip.hasCapacityForInsertion(1, 1, 4));
  }

  @Test
  void hasCapacityForInsertion_capacityFreesUpInRange_checksMaxInRange() {
    var stop1 = createStop(0, +3); // 3 passengers
    var stop2 = createStop(1, -2); // 2 dropoff, leaving 1
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);

    // Range includes both positions - max passengers is 3 (at position 1)
    assertTrue(trip.hasCapacityForInsertion(1, 3, 1)); // 4 total - 3 max = 1 available
    assertFalse(trip.hasCapacityForInsertion(1, 3, 2)); // Not enough
  }
}
