package org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor.spi.RaptorCostCalculator.ZERO_COST;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

public class PatternRideC1Test {

  @Test
  public void testParetoComparatorRelativeCost() {
    final var C1_LOW = 100;
    final var C1_HIGH = 500;
    final var TRIP_SORT_INDEX_1 = 1;
    final var TRIP_SORT_INDEX_2 = 2;

    var comparator = PatternRideC1.paretoComparatorRelativeCost();

    assertTrue(
      comparator.leftDominanceExist(
        new PatternRideC1<>(null, 0, 0, 0, C1_LOW, C1_LOW, TRIP_SORT_INDEX_1, null),
        new PatternRideC1<>(null, 0, 0, 0, C1_LOW, C1_LOW, TRIP_SORT_INDEX_2, null)
      )
    );

    assertFalse(
      comparator.leftDominanceExist(
        new PatternRideC1<>(null, 0, 0, 0, C1_LOW, C1_LOW, TRIP_SORT_INDEX_1, null),
        new PatternRideC1<>(null, 0, 0, 0, C1_LOW, C1_LOW, TRIP_SORT_INDEX_1, null)
      )
    );

    assertTrue(
      comparator.leftDominanceExist(
        new PatternRideC1<>(null, 0, 0, 0, C1_LOW, C1_LOW, TRIP_SORT_INDEX_1, null),
        new PatternRideC1<>(null, 0, 0, 0, C1_HIGH, C1_HIGH, TRIP_SORT_INDEX_1, null)
      )
    );

    assertFalse(
      comparator.leftDominanceExist(
        new PatternRideC1<>(null, 0, 0, 0, C1_HIGH, C1_HIGH, TRIP_SORT_INDEX_1, null),
        new PatternRideC1<>(null, 0, 0, 0, C1_LOW, C1_LOW, TRIP_SORT_INDEX_1, null)
      )
    );
  }

  @Test
  public void testUpdateC2() {
    var originalRide = new PatternRideC1<RaptorTripSchedule>(null, 0, 0, 0, 0, 0, 0, null);

    var updatedRide = originalRide.updateC2(1);

    assertSame(originalRide, updatedRide);
    assertEquals(ZERO_COST, updatedRide.c2());
  }
}
