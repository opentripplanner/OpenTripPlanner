package org.opentripplanner.ext.carpooling.util;

import static org.junit.jupiter.api.Assertions.*;
import static org.opentripplanner.ext.carpooling.TestCarpoolTripBuilder.*;
import static org.opentripplanner.ext.carpooling.TestFixtures.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class PassengerCountTimelineTest {

  @Test
  void build_noStops_allZeros() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    // No stops = no passengers along route
    assertEquals(0, timeline.getPassengerCount(0));
  }

  @Test
  void build_onePickupStop_incrementsAtStop() {
    var stop1 = createStop(0, +1); // Pickup 1 passenger
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1), OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    assertEquals(0, timeline.getPassengerCount(0)); // Before stop
    assertEquals(1, timeline.getPassengerCount(1)); // After stop
  }

  @Test
  void build_pickupAndDropoff_incrementsThenDecrements() {
    var stop1 = createStop(0, +2); // Pickup 2 passengers
    var stop2 = createStop(1, -1); // Dropoff 1 passenger
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    assertEquals(0, timeline.getPassengerCount(0)); // Before any stops
    assertEquals(2, timeline.getPassengerCount(1)); // After first pickup
    assertEquals(1, timeline.getPassengerCount(2)); // After dropoff
  }

  @Test
  void build_multipleStops_cumulativeCount() {
    var stop1 = createStop(0, +1);
    var stop2 = createStop(1, +2);
    var stop3 = createStop(2, -1);
    var stop4 = createStop(3, +1);
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2, stop3, stop4), OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    assertEquals(0, timeline.getPassengerCount(0));
    assertEquals(1, timeline.getPassengerCount(1)); // 0 + 1
    assertEquals(3, timeline.getPassengerCount(2)); // 1 + 2
    assertEquals(2, timeline.getPassengerCount(3)); // 3 - 1
    assertEquals(3, timeline.getPassengerCount(4)); // 2 + 1
  }

  @Test
  void build_negativePassengerDelta_handlesDropoffs() {
    var stop1 = createStop(0, +3); // Pickup 3
    var stop2 = createStop(1, -3); // Dropoff all 3
    var trip = createTripWithStops(OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    assertEquals(0, timeline.getPassengerCount(0));
    assertEquals(3, timeline.getPassengerCount(1));
    assertEquals(0, timeline.getPassengerCount(2)); // Back to zero
  }

  @Test
  void hasCapacityInRange_noPassengers_hasCapacity() {
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(), OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    assertTrue(timeline.hasCapacityInRange(0, 1, 1));
    assertTrue(timeline.hasCapacityInRange(0, 1, 4)); // Can fit all 4 seats
  }

  @Test
  void hasCapacityInRange_fullCapacity_noCapacity() {
    var stop1 = createStop(0, +4); // Fill all 4 seats
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1), OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    // No room for additional passenger after stop 1
    assertFalse(timeline.hasCapacityInRange(1, 2, 1));
  }

  @Test
  void hasCapacityInRange_partialCapacity_hasCapacityForOne() {
    var stop1 = createStop(0, +3); // 3 of 4 seats taken
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1), OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    assertTrue(timeline.hasCapacityInRange(1, 2, 1)); // Room for 1
    assertFalse(timeline.hasCapacityInRange(1, 2, 2)); // No room for 2
  }

  @Test
  void hasCapacityInRange_acrossMultiplePositions_checksAll() {
    var stop1 = createStop(0, +2);
    var stop2 = createStop(1, +1); // Total 3 passengers
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    // Range 1-3 includes position with 3 passengers, so only 1 seat available
    assertTrue(timeline.hasCapacityInRange(1, 3, 1));
    assertFalse(timeline.hasCapacityInRange(1, 3, 2));
  }

  @Test
  void hasCapacityInRange_rangeBeforeStop_usesInitialCapacity() {
    var stop1 = createStop(0, +4); // Fill capacity at position 1
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1), OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    // Before stop, should have full capacity
    assertTrue(timeline.hasCapacityInRange(0, 1, 4));
  }

  @Test
  void hasCapacityInRange_capacityFreesUpInRange_checksMaxInRange() {
    var stop1 = createStop(0, +3); // 3 passengers
    var stop2 = createStop(1, -2); // 2 dropoff, leaving 1
    var trip = createTripWithCapacity(4, OSLO_CENTER, List.of(stop1, stop2), OSLO_NORTH);
    var timeline = PassengerCountTimeline.build(trip);

    // Range includes both positions - max passengers is 3 (at position 1)
    assertTrue(timeline.hasCapacityInRange(1, 3, 1)); // 4 total - 3 max = 1 available
    assertFalse(timeline.hasCapacityInRange(1, 3, 2)); // Not enough
  }
}
