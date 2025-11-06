package org.opentripplanner.ext.carpooling.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createSimpleTrip;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createStop;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createTripWithCapacity;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.createTripWithStops;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for capacity checking methods on {@link CarpoolTrip}.
 */
class CarpoolTripCapacityTest {

  @Test
  void getPassengerCountAtPosition_noStops_allZeros() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    // Boarding
    assertEquals(0, trip.getPassengerCountAtPosition(0));
    // Beyond stops
    assertEquals(0, trip.getPassengerCountAtPosition(1));
  }

  @Test
  void getPassengerCountAtPosition_onePickupStop_incrementsAtStop() {
    // Pickup 1 passenger, then drop off 1 passenger
    var stop1 = createStop(0, 1);
    var stop2 = createStop(1, -1);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);

    // Position 0: Before origin stop
    assertEquals(0, trip.getPassengerCountAtPosition(0));
    // Position 1: After origin stop (passengerDelta=0)
    assertEquals(0, trip.getPassengerCountAtPosition(1));
    // Position 2: After pickup stop (passengerDelta=1)
    assertEquals(1, trip.getPassengerCountAtPosition(2));
    // Position 3: After dropoff stop (passengerDelta=-1)
    assertEquals(0, trip.getPassengerCountAtPosition(3));
    // Position 4: After destination stop (passengerDelta=0)
    assertEquals(0, trip.getPassengerCountAtPosition(4));
  }

  @Test
  void getPassengerCountAtPosition_pickupAndDropoff_incrementsThenDecrements() {
    // Pickup 2 passengers
    var stop1 = createStop(0, 2);
    // Dropoff 1 passenger
    var stop2 = createStop(1, -1);
    // Dropoff remaining passenger
    var stop3 = createStop(2, -1);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2, stop3), OSLO_NORTH);

    // Position 0: Before origin stop
    assertEquals(0, trip.getPassengerCountAtPosition(0));
    // Position 1: After origin stop (passengerDelta=0)
    assertEquals(0, trip.getPassengerCountAtPosition(1));
    // Position 2: After first intermediate stop (passengerDelta=2)
    assertEquals(2, trip.getPassengerCountAtPosition(2));
    // Position 3: After second intermediate stop (passengerDelta=-1)
    assertEquals(1, trip.getPassengerCountAtPosition(3));
    // Position 4: After third intermediate stop (passengerDelta=-1)
    assertEquals(0, trip.getPassengerCountAtPosition(4));
    // Position 5: After destination stop (passengerDelta=0)
    assertEquals(0, trip.getPassengerCountAtPosition(5));
  }

  @Test
  void getPassengerCountAtPosition_multipleStops_cumulativeCount() {
    var stop1 = createStop(0, 1);
    var stop2 = createStop(1, 2);
    var stop3 = createStop(2, -1);
    var stop4 = createStop(3, 1);
    var stop5 = createStop(4, -3);
    var trip = createTripWithStops(
      OSLO_CENTER,
      List.of(stop1, stop2, stop3, stop4, stop5),
      OSLO_NORTH
    );

    // Position 0: Before origin
    assertEquals(0, trip.getPassengerCountAtPosition(0));
    // Position 1: After origin (passengerDelta=0)
    assertEquals(0, trip.getPassengerCountAtPosition(1));
    // Position 2: After stop1 (0 + 1)
    assertEquals(1, trip.getPassengerCountAtPosition(2));
    // Position 3: After stop2 (1 + 2)
    assertEquals(3, trip.getPassengerCountAtPosition(3));
    // Position 4: After stop3 (3 - 1)
    assertEquals(2, trip.getPassengerCountAtPosition(4));
    // Position 5: After stop4 (2 + 1)
    assertEquals(3, trip.getPassengerCountAtPosition(5));
    // Position 6: After stop5 (3 - 3)
    assertEquals(0, trip.getPassengerCountAtPosition(6));
    // Position 7: After destination (passengerDelta=0)
    assertEquals(0, trip.getPassengerCountAtPosition(7));
  }

  @Test
  void getPassengerCountAtPosition_negativePosition_throwsException() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    assertThrows(IllegalArgumentException.class, () -> trip.getPassengerCountAtPosition(-1));
  }

  @Test
  void getPassengerCountAtPosition_positionTooLarge_throwsException() {
    var stop1 = createStop(0, 1);
    var stop2 = createStop(1, 1);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);

    // Trip has: origin (0), stop1 (1), stop2 (2), destination (3) = 4 stops total
    // Valid positions are 0 to 4 (0 to stops.size())
    // Position 5 should throw
    assertThrows(IllegalArgumentException.class, () -> trip.getPassengerCountAtPosition(5));
    // Position 999 should also throw
    assertThrows(IllegalArgumentException.class, () -> trip.getPassengerCountAtPosition(999));
  }

  @Test
  void hasCapacityForInsertion_noPassengers_hasCapacity() {
    var trip = createTripWithStops(OSLO_CENTER, List.of(), OSLO_NORTH);

    assertTrue(trip.hasCapacityForInsertion(1, 2, 1));
    // Can fit all 4 seats
    assertTrue(trip.hasCapacityForInsertion(1, 2, 4));
  }

  @Test
  void hasCapacityForInsertion_fullCapacity_noCapacity() {
    // Fill all 4 seats
    var stop1 = createStop(0, 4);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1), OSLO_NORTH);

    // No room for additional passenger after stop 1
    assertFalse(trip.hasCapacityForInsertion(2, 3, 1));
  }

  @Test
  void hasCapacityForInsertion_partialCapacity_hasCapacityForOne() {
    // 3 of 4 seats taken
    var stop1 = createStop(0, 3);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1), OSLO_NORTH);

    // Room for 1
    assertTrue(trip.hasCapacityForInsertion(2, 3, 1));
    // No room for 2
    assertFalse(trip.hasCapacityForInsertion(2, 3, 2));
  }

  @Test
  void hasCapacityForInsertion_acrossMultiplePositions_checksAll() {
    var stop1 = createStop(0, 2);
    // Total 3 passengers at position 3
    var stop2 = createStop(1, 1);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);

    // Trip positions: 0 (before origin), 1 (after origin=0), 2 (after stop1=2), 3 (after stop2=3), 4 (after dest=0)
    // Range 2-4 includes position 3 with 3 passengers, so only 1 seat available
    assertTrue(trip.hasCapacityForInsertion(2, 4, 1));
    assertFalse(trip.hasCapacityForInsertion(2, 4, 2));
  }

  @Test
  void hasCapacityForInsertion_rangeBeforeStop_usesInitialCapacity() {
    // Fill capacity at position 1
    var stop1 = createStop(0, 4);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1), OSLO_NORTH);

    // Pickup at position 1, dropoff at position 1 - only checks capacity at boarding (position 0)
    // At boarding there are no passengers yet, so we have full capacity
    assertTrue(trip.hasCapacityForInsertion(1, 1, 4));
  }

  @Test
  void hasCapacityForInsertion_capacityFreesUpInRange_checksMaxInRange() {
    // 3 passengers
    var stop1 = createStop(0, 3);
    // 2 dropoff, leaving 1
    var stop2 = createStop(1, -2);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);

    // Range includes both positions - max passengers is 3 (at position 1)
    // 4 total - 3 max = 1 available
    assertTrue(trip.hasCapacityForInsertion(1, 3, 1));
    // Not enough
    assertFalse(trip.hasCapacityForInsertion(1, 3, 2));
  }
}
