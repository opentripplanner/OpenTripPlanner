package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.Distance;

public class DistanceTest {

  private static final Distance ONE_THOUSAND_FIVE_HUNDRED_METERS = Distance.ofMeters(1500d);
  private static final Distance ONE_POINT_FIVE_KILOMETERS = Distance.ofKilometers(1.5d);
  private static final Distance TWO_KILOMETERS = Distance.ofKilometers(2d);
  private static final Distance ONE_HUNDRED_METERS = Distance.ofMeters(100d);
  private static final Distance POINT_ONE_KILOMETER = Distance.ofKilometers(0.1d);
  private static final Distance ONE_HUNDRED_POINT_FIVE_METERS = Distance.ofMeters(100.5d);

  @Test
  void equals() {
    assertEquals(ONE_THOUSAND_FIVE_HUNDRED_METERS, ONE_POINT_FIVE_KILOMETERS);
    assertEquals(POINT_ONE_KILOMETER, ONE_HUNDRED_METERS);
    assertNotEquals(ONE_HUNDRED_POINT_FIVE_METERS, ONE_HUNDRED_METERS);
    assertNotEquals(TWO_KILOMETERS, ONE_POINT_FIVE_KILOMETERS);
  }

  @Test
  void greaterThan() {
    assertTrue(ONE_HUNDRED_POINT_FIVE_METERS.greaterThan(ONE_HUNDRED_METERS));
    assertTrue(TWO_KILOMETERS.greaterThan(ONE_THOUSAND_FIVE_HUNDRED_METERS));
    assertFalse(ONE_THOUSAND_FIVE_HUNDRED_METERS.greaterThan(ONE_POINT_FIVE_KILOMETERS));
  }

  @Test
  void lessThan() {
    assertTrue(ONE_THOUSAND_FIVE_HUNDRED_METERS.lessThan(TWO_KILOMETERS));
    assertTrue(POINT_ONE_KILOMETER.lessThan(ONE_HUNDRED_POINT_FIVE_METERS));
    assertFalse(ONE_HUNDRED_POINT_FIVE_METERS.lessThan(ONE_HUNDRED_METERS));
  }

  @Test
  void equalHashCode() {
    assertEquals(Distance.ofMeters(5d).hashCode(), Distance.ofMeters(5d).hashCode());
    assertNotEquals(Distance.ofMeters(5d).hashCode(), Double.valueOf(5d).hashCode());
  }
}
