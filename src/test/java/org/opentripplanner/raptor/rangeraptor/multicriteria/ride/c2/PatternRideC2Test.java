package org.opentripplanner.raptor.rangeraptor.multicriteria.ride.c2;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PatternRideC2Test {

  @Test
  public void testParetoComparatorRelativeCost() {
    final var LOW_COST = 100;
    final var HIGH_COST = 500;
    final var TRIP_SORT_INDEX_1 = 1;
    final var TRIP_SORT_INDEX_2 = 2;
    var comparator = PatternRideC2.paretoComparatorRelativeCost((l1, l2) -> l1 > l2);

    assertTrue(
      comparator.leftDominanceExist(
        new PatternRideC2<>(null, 0, 0, 0, LOW_COST, LOW_COST, LOW_COST, TRIP_SORT_INDEX_1, null),
        new PatternRideC2<>(null, 0, 0, 0, LOW_COST, LOW_COST, LOW_COST, TRIP_SORT_INDEX_2, null)
      )
    );
    assertFalse(
      comparator.leftDominanceExist(
        new PatternRideC2<>(null, 0, 0, 0, LOW_COST, LOW_COST, LOW_COST, TRIP_SORT_INDEX_1, null),
        new PatternRideC2<>(null, 0, 0, 0, LOW_COST, LOW_COST, LOW_COST, TRIP_SORT_INDEX_1, null)
      )
    );

    assertTrue(
      comparator.leftDominanceExist(
        new PatternRideC2<>(null, 0, 0, 0, LOW_COST, LOW_COST, LOW_COST, TRIP_SORT_INDEX_1, null),
        new PatternRideC2<>(null, 0, 0, 0, HIGH_COST, HIGH_COST, LOW_COST, TRIP_SORT_INDEX_1, null)
      )
    );

    assertFalse(
      comparator.leftDominanceExist(
        new PatternRideC2<>(null, 0, 0, 0, HIGH_COST, HIGH_COST, LOW_COST, TRIP_SORT_INDEX_1, null),
        new PatternRideC2<>(null, 0, 0, 0, LOW_COST, LOW_COST, LOW_COST, TRIP_SORT_INDEX_1, null)
      )
    );

    assertTrue(
      comparator.leftDominanceExist(
        new PatternRideC2<>(null, 0, 0, 0, LOW_COST, LOW_COST, HIGH_COST, TRIP_SORT_INDEX_1, null),
        new PatternRideC2<>(null, 0, 0, 0, LOW_COST, LOW_COST, LOW_COST, TRIP_SORT_INDEX_1, null)
      )
    );

    assertFalse(
      comparator.leftDominanceExist(
        new PatternRideC2<>(null, 0, 0, 0, LOW_COST, LOW_COST, LOW_COST, TRIP_SORT_INDEX_1, null),
        new PatternRideC2<>(null, 0, 0, 0, LOW_COST, LOW_COST, HIGH_COST, TRIP_SORT_INDEX_1, null)
      )
    );
  }

  @Test
  public void testUpdateC2() {
    final var BOARD_STOP_INDEX = 10;
    final var BOARD_POS = 11;
    final var BOARD_TIME = 12;
    final var BOARD_C1 = 13;
    final var RELATIVE_C1 = 14;
    final var NEW_C2 = 15;

    var originalRide = new PatternRideC2<>(
      null,
      BOARD_STOP_INDEX,
      BOARD_POS,
      BOARD_TIME,
      BOARD_C1,
      RELATIVE_C1,
      0,
      0,
      null
    );

    var updatedRide = originalRide.updateC2(NEW_C2);

    assertAll(
      () -> assertEquals(BOARD_STOP_INDEX, updatedRide.boardStopIndex()),
      () -> assertEquals(BOARD_POS, updatedRide.boardPos()),
      () -> assertEquals(BOARD_TIME, updatedRide.boardTime()),
      () -> assertEquals(BOARD_C1, updatedRide.boardC1()),
      () -> assertEquals(RELATIVE_C1, updatedRide.relativeC1()),
      () -> assertEquals(NEW_C2, updatedRide.c2())
    );
  }
}
