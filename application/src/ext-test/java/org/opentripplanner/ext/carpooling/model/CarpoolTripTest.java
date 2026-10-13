package org.opentripplanner.ext.carpooling.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_SOUTH;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createStopAt;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithCapacity;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithStops;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class CarpoolTripTest {

  // -- hasFreeSeat tests --

  @Test
  void hasFreeSeat_onlyOnALegThatIsNotFull() {
    // Room for two: a seat free on the first leg, none on the second.
    var trip = createTripWithCapacity(
      2,
      List.of(
        createStopAt(1, OSLO_SOUTH),
        createStopAt(2, OSLO_CENTER),
        createStopAt(0, OSLO_NORTH)
      )
    );

    assertTrue(trip.hasFreeSeat(0));
    assertFalse(trip.hasFreeSeat(1));
  }

  // -- legDeviationBudget tests --

  @Test
  void aLegHasTheBudgetOfTheStopItEndsAt() {
    var stop = createStopAt(OSLO_CENTER, Duration.ofMinutes(5));
    var trip = createTripWithStops(OSLO_SOUTH, List.of(stop), OSLO_NORTH, Duration.ofMinutes(20));

    assertEquals(Duration.ofMinutes(5), trip.legDeviationBudget(0));
    assertEquals(Duration.ofMinutes(20), trip.legDeviationBudget(1));
  }

  @Test
  void aLaterStopWithASmallerBudgetCapsTheLegsBeforeIt() {
    var stop = createStopAt(OSLO_CENTER, Duration.ofMinutes(10));
    var trip = createTripWithStops(OSLO_SOUTH, List.of(stop), OSLO_NORTH, Duration.ofMinutes(3));

    assertEquals(Duration.ofMinutes(3), trip.legDeviationBudget(0));
    assertEquals(Duration.ofMinutes(3), trip.legDeviationBudget(1));
  }
}
