package org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c1;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor.spi.RaptorCostCalculator.ZERO_COST;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.rangeraptor.multicriteria.arrivals.util.TestRaptorTripSchedule;

public class PatternRideC1Test {

  @Test
  public void testParetoComparatorRelativeCost() {
    final var LOW_COST = 100;
    final var HIGH_COST = 500;
    final var TRIP_SORT_INDEX_1 = 1;
    final var TRIP_SORT_INDEX_2 = 2;

    var comparator = PatternRideC1.paretoComparatorRelativeCost();

    assertTrue(
      comparator.leftDominanceExist(
        new PatternRideC1<>(null, 0, 0, 0, LOW_COST, LOW_COST, TRIP_SORT_INDEX_1, null),
        new PatternRideC1<>(null, 0, 0, 0, LOW_COST, LOW_COST, TRIP_SORT_INDEX_2, null)
      )
    );

    assertFalse(
      comparator.leftDominanceExist(
        new PatternRideC1<>(null, 0, 0, 0, LOW_COST, LOW_COST, TRIP_SORT_INDEX_1, null),
        new PatternRideC1<>(null, 0, 0, 0, LOW_COST, LOW_COST, TRIP_SORT_INDEX_1, null)
      )
    );

    assertTrue(
      comparator.leftDominanceExist(
        new PatternRideC1<>(null, 0, 0, 0, LOW_COST, LOW_COST, TRIP_SORT_INDEX_1, null),
        new PatternRideC1<>(null, 0, 0, 0, HIGH_COST, HIGH_COST, TRIP_SORT_INDEX_1, null)
      )
    );

    assertFalse(
      comparator.leftDominanceExist(
        new PatternRideC1<>(null, 0, 0, 0, HIGH_COST, HIGH_COST, TRIP_SORT_INDEX_1, null),
        new PatternRideC1<>(null, 0, 0, 0, LOW_COST, LOW_COST, TRIP_SORT_INDEX_1, null)
      )
    );
  }

  @Test
  public void testUpdateC2() {
    var originalRide = new PatternRideC1<TestRaptorTripSchedule>(null, 0, 0, 0, 0, 0, 0, null);

    var updatedRide = originalRide.updateC2(1);

    assertAll(
      () -> assertSame(originalRide, updatedRide),
      () -> assertEquals(ZERO_COST, updatedRide.c2())
    );
  }
}
