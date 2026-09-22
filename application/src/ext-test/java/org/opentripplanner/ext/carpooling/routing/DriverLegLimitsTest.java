package org.opentripplanner.ext.carpooling.routing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_CENTER;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_EAST;
import static org.opentripplanner.ext.carpooling.CarpoolTestCoordinates.OSLO_NORTH;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createStopAt;
import static org.opentripplanner.ext.carpooling.CarpoolTripTestData.createTripWithStops;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class DriverLegLimitsTest {

  @Test
  void eachLegMayGrowByTheSmallestBudgetOfTheStopsAfterIt() {
    // Budgets: 5 min at the intermediate stop, 40 min at the destination.
    var trip = createTripWithStops(
      OSLO_CENTER,
      List.of(createStopAt(OSLO_EAST, minutes(5))),
      OSLO_NORTH,
      minutes(40)
    );

    var limits = DriverLegLimits.legLimits(trip, new Duration[] { minutes(60), minutes(100) });

    assertArrayEquals(new Duration[] { minutes(65), minutes(140) }, limits);
  }

  private static Duration minutes(long minutes) {
    return Duration.ofMinutes(minutes);
  }
}
