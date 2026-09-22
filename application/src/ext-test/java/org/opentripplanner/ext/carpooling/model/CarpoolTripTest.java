package org.opentripplanner.ext.carpooling.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_SOUTH;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createStopAt;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithStops;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class CarpoolTripTest {

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
