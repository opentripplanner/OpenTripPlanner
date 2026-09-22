package org.opentripplanner.ext.carpooling.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
}
