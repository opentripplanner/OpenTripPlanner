package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.Distance;

public class DistanceTest {

  private static final Distance ONE_THOUSAND_FIVE_HUNDRED_METERS = Distance.ofMetersBoxed(
    1500d,
    ignore -> {}
  ).orElse(null);
  private static final Distance ONE_POINT_FIVE_KILOMETERS = Distance.ofKilometersBoxed(
    1.5d,
    ignore -> {}
  ).orElse(null);
  private static final Distance TWO_KILOMETERS = Distance.ofKilometersBoxed(
    2d,
    ignore -> {}
  ).orElse(null);
  private static final Distance ONE_HUNDRED_METERS = Distance.ofMetersBoxed(
    100d,
    ignore -> {}
  ).orElse(null);
  private static final Distance POINT_ONE_KILOMETER = Distance.ofKilometersBoxed(
    0.1d,
    ignore -> {}
  ).orElse(null);
  private static final Distance ONE_HUNDRED_POINT_FIVE_METERS = Distance.ofMetersBoxed(
    100.5d,
    ignore -> {}
  ).orElse(null);

  @Test
  void equals() {
    assertEquals(ONE_THOUSAND_FIVE_HUNDRED_METERS, ONE_POINT_FIVE_KILOMETERS);
    assertEquals(POINT_ONE_KILOMETER, ONE_HUNDRED_METERS);
    assertNotEquals(ONE_HUNDRED_POINT_FIVE_METERS, ONE_HUNDRED_METERS);
    assertNotEquals(TWO_KILOMETERS, ONE_POINT_FIVE_KILOMETERS);
  }

  @Test
  void testHashCode() {
    assertEquals(
      Distance.ofMetersBoxed(5d, ignore -> {}).hashCode(),
      Distance.ofMetersBoxed(5d, ignore -> {}).hashCode()
    );
  }
}
