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

  private static final Duration MAX_TRIP_DURATION = Duration.ofHours(3);

  @Test
  void eachLegMayGrowByTheSmallestBudgetOfTheStopsAfterIt() {
    // Budgets: 5 min at the intermediate stop, 40 min at the destination.
    var trip = createTripWithStops(
      OSLO_CENTER,
      List.of(createStopAt(OSLO_EAST, minutes(5))),
      OSLO_NORTH,
      minutes(40)
    );

    var limits = DriverLegLimits.legLimits(
      trip,
      new Duration[] { minutes(60), minutes(100) },
      MAX_TRIP_DURATION
    );

    assertArrayEquals(new Duration[] { minutes(65), minutes(140) }, limits);
  }

  @Test
  void aLimitNeverExceedsTheMaximumTripDuration() {
    var trip = createTripWithStops(OSLO_CENTER, List.of(), OSLO_NORTH, minutes(10));

    var limits = DriverLegLimits.legLimits(
      trip,
      new Duration[] { Duration.ofHours(4) },
      MAX_TRIP_DURATION
    );

    assertArrayEquals(new Duration[] { MAX_TRIP_DURATION }, limits);
  }

  private static Duration minutes(long minutes) {
    return Duration.ofMinutes(minutes);
  }
}
