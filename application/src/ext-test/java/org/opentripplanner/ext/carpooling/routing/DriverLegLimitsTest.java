package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTHEAST;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createStopAt;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithStops;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class DriverLegLimitsTest {

  @Test
  void eachLegMayGrowByTheSmallestBudgetOfTheStopsAfterIt() {
    // Budgets: 5 and 40 min at the intermediate stops, 20 min at the destination.
    var trip = createTripWithStops(
      OSLO_CENTER,
      List.of(createStopAt(OSLO_EAST, minutes(5)), createStopAt(OSLO_NORTHEAST, minutes(40))),
      OSLO_NORTH,
      minutes(20)
    );

    var limits = DriverLegLimits.legLimits(trip, new Duration[] {
      minutes(60),
      minutes(100),
      minutes(30),
    });

    // The middle leg is bounded by the destination's 20 min, not the next stop's 40 min.
    assertArrayEquals(new Duration[] { minutes(65), minutes(120), minutes(50) }, limits);
  }

  @Test
  void aNegativeBudgetAllowsNoDetour() {
    var trip = createTripWithStops(OSLO_CENTER, List.of(), OSLO_NORTH, minutes(-5));

    var limits = DriverLegLimits.legLimits(trip, new Duration[] { minutes(60) });

    assertArrayEquals(new Duration[] { minutes(60) }, limits);
  }

  private static Duration minutes(long minutes) {
    return Duration.ofMinutes(minutes);
  }
}
