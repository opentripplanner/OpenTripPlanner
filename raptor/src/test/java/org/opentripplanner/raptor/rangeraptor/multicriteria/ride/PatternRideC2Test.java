package org.opentripplanner.raptor.rangeraptor.multicriteria.ride;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.raptor.api.model.RelaxFunction;

public class PatternRideC2Test {

  @Test
  public void testParetoComparatorRelativeCost() {
    final var C1_LOW = 100;
    final var C1_HIGH = 500;
    final var TRIP_SORT_INDEX_1 = 1;
    final var TRIP_SORT_INDEX_2 = 2;
    final RelaxFunction relacC1 = c -> c + 3;

    var comparator = PatternRideC2.comparatorRelaxedC1IfC2IsOptimal(relacC1, (l1, l2) -> l1 > l2);

    assertTrue(
      comparator.leftDominanceExist(
        new PatternRideC2<>(null, 0, 0, 0, C1_LOW, C1_LOW, C1_LOW, TRIP_SORT_INDEX_1, null),
        new PatternRideC2<>(null, 0, 0, 0, C1_LOW, C1_LOW, C1_LOW, TRIP_SORT_INDEX_2, null)
      )
    );
    assertFalse(
      comparator.leftDominanceExist(
        new PatternRideC2<>(null, 0, 0, 0, C1_LOW, C1_LOW, C1_LOW, TRIP_SORT_INDEX_1, null),
        new PatternRideC2<>(null, 0, 0, 0, C1_LOW, C1_LOW, C1_LOW, TRIP_SORT_INDEX_1, null)
      )
    );

    // Left sort index higher than right: should NOT dominate (tripSortIndex is not a dominance
    // criterion in this direction — different trips are incomparable, only lower index dominates)
    assertFalse(
      comparator.leftDominanceExist(
        new PatternRideC2<>(null, 0, 0, 0, C1_LOW, C1_LOW, C1_LOW, TRIP_SORT_INDEX_2, null),
        new PatternRideC2<>(null, 0, 0, 0, C1_LOW, C1_LOW, C1_LOW, TRIP_SORT_INDEX_1, null)
      )
    );

    assertTrue(
      comparator.leftDominanceExist(
        new PatternRideC2<>(null, 0, 0, 0, C1_LOW, C1_LOW, C1_LOW, TRIP_SORT_INDEX_1, null),
        new PatternRideC2<>(null, 0, 0, 0, C1_HIGH, C1_HIGH, C1_LOW, TRIP_SORT_INDEX_1, null)
      )
    );

    assertFalse(
      comparator.leftDominanceExist(
        new PatternRideC2<>(null, 0, 0, 0, C1_HIGH, C1_HIGH, C1_LOW, TRIP_SORT_INDEX_1, null),
        new PatternRideC2<>(null, 0, 0, 0, C1_LOW, C1_LOW, C1_LOW, TRIP_SORT_INDEX_1, null)
      )
    );

    assertTrue(
      comparator.leftDominanceExist(
        new PatternRideC2<>(null, 0, 0, 0, C1_LOW, C1_LOW, C1_HIGH, TRIP_SORT_INDEX_1, null),
        new PatternRideC2<>(null, 0, 0, 0, C1_LOW, C1_LOW, C1_LOW, TRIP_SORT_INDEX_1, null)
      )
    );

    assertFalse(
      comparator.leftDominanceExist(
        new PatternRideC2<>(null, 0, 0, 0, C1_LOW, C1_LOW, C1_LOW, TRIP_SORT_INDEX_1, null),
        new PatternRideC2<>(null, 0, 0, 0, C1_LOW, C1_LOW, C1_HIGH, TRIP_SORT_INDEX_1, null)
      )
    );
  }
}
