package org.opentripplanner.routing.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.basic.Distance;

public class DistanceTest {

  private static final Distance oneThousandFiveHundredMeters = Distance.ofMeters(1500d);
  private static final Distance onePointFiveKilometers = Distance.ofKilometers(1.5d);
  private static final Distance twoKilometers = Distance.ofKilometers(2d);
  private static final Distance oneHundredMeters = Distance.ofMeters(100d);
  private static final Distance pointOneKilometer = Distance.ofKilometers(0.1d);
  private static final Distance oneHundredPointFiveMeters = Distance.ofMeters(100.5d);

  @Test
  void equals() {
    assertEquals(oneThousandFiveHundredMeters, onePointFiveKilometers);
    assertEquals(pointOneKilometer, oneHundredMeters);
    assertNotEquals(oneHundredPointFiveMeters, oneHundredMeters);
    assertNotEquals(twoKilometers, onePointFiveKilometers);
  }

  @Test
  void greaterThan() {
    assertTrue(oneHundredPointFiveMeters.greaterThan(oneHundredMeters));
    assertTrue(twoKilometers.greaterThan(oneThousandFiveHundredMeters));
    assertFalse(oneThousandFiveHundredMeters.greaterThan(onePointFiveKilometers));
  }

  @Test
  void lessThan() {
    assertTrue(oneThousandFiveHundredMeters.lessThan(twoKilometers));
    assertTrue(pointOneKilometer.lessThan(oneHundredPointFiveMeters));
    assertFalse(oneHundredPointFiveMeters.lessThan(oneHundredMeters));
  }

  @Test
  void equalHashCode() {
    assertEquals(Distance.ofMeters(5d).hashCode(), Distance.ofMeters(5d).hashCode());
    assertNotEquals(Distance.ofMeters(5d).hashCode(), Double.valueOf(5d).hashCode());
  }
}
