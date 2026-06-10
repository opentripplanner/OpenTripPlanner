package org.opentripplanner.ext.carpooling.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createSimpleTrip;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createStop;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithStops;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for capacity checking methods on {@link CarpoolTrip}.
 * <p>
 * All trips created via {@code createTripWithStops} have totalCapacity=5.
 * The method wraps intermediate stops with an Origin (onboard=1) at the front
 * and a Destination (onboard=1) at the end.
 * <p>
 * {@code pickupPosition} and {@code dropoffPosition} in {@code hasCapacityForInsertion}
 * are 0-based indices of the passenger's stops in the modified route (the route after the
 * passenger's pickup and dropoff have been inserted into the carpool trip).
 */
class CarpoolTripCapacityTest {

  // -- getPassengerCountAtDepartureOfStop tests --

  @Test
  void getPassengerCountAtDepartureOfStop_driverOnly() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);

    assertEquals(1, trip.getPassengerCountAtDepartureOfStop(0));
    assertEquals(1, trip.getPassengerCountAtDepartureOfStop(1));
  }

  @Test
  void getPassengerCountAtDepartureOfStop_withIntermediateStops() {
    // Stops: [Origin(1), A(2), B(1), Destination(1)]
    var trip = createTripWithStops(OSLO_CENTER, List.of(createStop(2), createStop(1)), OSLO_NORTH);

    assertEquals(1, trip.getPassengerCountAtDepartureOfStop(0));
    assertEquals(2, trip.getPassengerCountAtDepartureOfStop(1));
    assertEquals(1, trip.getPassengerCountAtDepartureOfStop(2));
    assertEquals(1, trip.getPassengerCountAtDepartureOfStop(3));
  }

  @Test
  void getPassengerCountAtDepartureOfStop_negativeIndex_throwsException() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    assertThrows(IllegalArgumentException.class, () -> trip.getPassengerCountAtDepartureOfStop(-1));
  }

  @Test
  void getPassengerCountAtDepartureOfStop_indexTooLarge_throwsException() {
    var trip = createSimpleTrip(OSLO_CENTER, OSLO_NORTH);
    // Trip has 2 stops (Origin, Destination), valid indices are 0 and 1
    assertThrows(IllegalArgumentException.class, () -> trip.getPassengerCountAtDepartureOfStop(2));
  }

  @Test
  void adjacentPositions_onlyChecksStopBeforePickup() {
    // Original stops: [Origin(1), A(5), Destination(1)]   totalCapacity=5
    var trip = createTripWithStops(OSLO_CENTER, List.of(createStop(5)), OSLO_NORTH);

    // Modified route: [Origin, Pickup, Dropoff, A, Destination]
    //                     0       1       2     3      4
    // Checked original stops: Origin (index 0, onboard=1). A is NOT checked.
    assertTrue(trip.hasCapacityForInsertion(1, 2, 4));
  }

  @Test
  void adjacentPositions_fullAtStopBeforePickup() {
    // Original stops: [Origin(1), A(5), Destination(1)]   totalCapacity=5
    var trip = createTripWithStops(OSLO_CENTER, List.of(createStop(5)), OSLO_NORTH);

    // Modified route: [Origin, A, Pickup, Dropoff, Destination]
    //                     0     1    2       3        4
    // Checked original stops: A (index 1, onboard=5). No room.
    assertFalse(trip.hasCapacityForInsertion(2, 3, 1));
  }

  @Test
  void widerGap_checksAllOriginalStopsBetweenPickupAndDropoff() {
    // Original stops: [Origin(1), A(2), B(4), C(1), Destination(1)]   totalCapacity=5
    var trip = createTripWithStops(
      OSLO_CENTER,
      List.of(createStop(2), createStop(4), createStop(1)),
      OSLO_NORTH
    );

    // Modified route: [Origin, Pickup, A, B, Dropoff, C, Destination]
    //                     0       1    2  3     4     5      6
    // Checked original stops: Origin(1), A(2), B(4). Max is 4, room for 1.
    assertTrue(trip.hasCapacityForInsertion(1, 4, 1));
    assertFalse(trip.hasCapacityForInsertion(1, 4, 2));
  }

  @Test
  void stopAfterDropoff_isNotChecked() {
    // Original stops: [Origin(1), A(1), B(5), Destination(1)]   totalCapacity=5
    // B is full, but the passenger is dropped off before B.
    var trip = createTripWithStops(OSLO_CENTER, List.of(createStop(1), createStop(5)), OSLO_NORTH);

    // Modified route: [Origin, Pickup, A, Dropoff, B, Destination]
    //                     0       1    2     3     4      5
    // Checked original stops: Origin(1), A(1). B is after the dropoff.
    assertTrue(trip.hasCapacityForInsertion(1, 3, 3));
  }

  @Test
  void pickupNearEnd_limitedByStopBeforePickup() {
    // Original stops: [Origin(1), A(4), Destination(1)]   totalCapacity=5
    var trip = createTripWithStops(OSLO_CENTER, List.of(createStop(4)), OSLO_NORTH);

    // Modified route: [Origin, A, Pickup, Dropoff, Destination]
    //                     0     1    2       3        4
    // Checked original stops: A (index 1, onboard=4). Room for 1.
    assertTrue(trip.hasCapacityForInsertion(2, 3, 1));
    assertFalse(trip.hasCapacityForInsertion(2, 3, 2));
  }

  @Test
  void fullRangeInsertion_checksAllOriginalStops() {
    // Original stops: [Origin(2), A(2), Destination(2)]   totalCapacity=5
    var trip = createTripWithStops(OSLO_CENTER, List.of(createStop(2)), OSLO_NORTH);

    // Modified route: [Origin, Pickup, A, Dropoff, Destination]
    //                     0       1    2     3        4
    // Checked original stops: Origin(2), A(2). Both onboard=2, room for 3.
    assertTrue(trip.hasCapacityForInsertion(1, 3, 3));
    assertFalse(trip.hasCapacityForInsertion(1, 3, 4));
  }

  @Test
  void fullStopBeforePickup_notInCheckedRange() {
    // Original stops: [Origin(1), A(5), Destination(1)]   totalCapacity=5
    // A is full, but pickup and dropoff are inserted after A.
    var trip = createTripWithStops(OSLO_CENTER, List.of(createStop(5)), OSLO_NORTH);

    // Modified route: [Origin, A, Destination, Pickup, Dropoff]
    //                     0     1      2          3       4
    // firstOriginalStop = 3 - 1 = 2, lastOriginalStop = 4 - 2 = 2
    // Checked original stop: Destination (index 2, onboard=1). A is outside the range.
    assertTrue(trip.hasCapacityForInsertion(3, 4, 4));
    assertFalse(trip.hasCapacityForInsertion(3, 4, 5));
  }

  @Test
  void bottleneckInMiddle_limitsCapacity() {
    // Original stops: [Origin(1), A(1), B(4), C(1), D(1), Destination(1)]   totalCapacity=5
    var trip = createTripWithStops(
      OSLO_CENTER,
      List.of(createStop(1), createStop(4), createStop(1), createStop(1)),
      OSLO_NORTH
    );

    // Modified route: [Origin, Pickup, A, B, C, Dropoff, D, Destination]
    //                     0       1    2  3  4     5     6      7
    // Checked original stops: Origin(1), A(1), B(4), C(1). Max is 4 at B, room for 1.
    assertTrue(trip.hasCapacityForInsertion(1, 5, 1));
    assertFalse(trip.hasCapacityForInsertion(1, 5, 2));
  }
}
