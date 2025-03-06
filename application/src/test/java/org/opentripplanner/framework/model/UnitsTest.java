package org.opentripplanner.framework.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UnitsTest {

  @Test
  void reluctance() {
    assertEquals(0.0, Units.reluctance(0.0));
    assertEquals(1.12, Units.reluctance(1.1234));
    assertEquals(2.1, Units.reluctance(2.1234));
    assertEquals(10.0, Units.reluctance(10.1234));
    var ex = assertThrows(IllegalArgumentException.class, () -> Units.reluctance(-0.01));
    assertEquals("The value is not in range[0.0, 1.7976931348623157E308]: -0.01", ex.getMessage());
  }

  @Test
  void normalizedFactor() {
    assertEquals(0.0, Units.normalizedFactor(0.0, 0.0, 8.0));
    var ex = assertThrows(IllegalArgumentException.class, () ->
      Units.normalizedFactor(0.999, 1.0, 8.0)
    );
    assertEquals("The value is not in range[1.0, 8.0]: 1.0", ex.getMessage());
  }

  @Test
  void normalizedOptionalFactor() {
    assertNull(Units.normalizedOptionalFactor(null, 0.0, 8.0));
    assertEquals(1.0, Units.normalizedOptionalFactor(1.0, 0.0, 8.0));
  }

  @Test
  void duration() {
    assertEquals(0, Units.duration(0));
    assertEquals(10_000, Units.duration(10_000));
    var ex = assertThrows(IllegalArgumentException.class, () -> Units.duration(-1));
    assertEquals("Negative value not expected for value: -1", ex.getMessage());
  }

  @Test
  void speed() {
    assertEquals(0.1, Units.speed(0.0));
    assertEquals(1.12, Units.speed(1.1234));
    assertEquals(2.1, Units.speed(2.1234));
    assertEquals(10.0, Units.speed(10.1234));
    var ex = assertThrows(IllegalArgumentException.class, () -> Units.speed(-0.01));
    assertEquals("Negative speed not expected: -0.01 m/s", ex.getMessage());
  }

  @Test
  void acceleration() {
    assertEquals(0.1, Units.acceleration(0.0));
    assertEquals(9.8, Units.acceleration(9.78888));
    var ex = assertThrows(IllegalArgumentException.class, () -> Units.acceleration(-0.01));
    assertEquals("Negative acceleration or deceleration not expected: -0.01", ex.getMessage());
  }

  @Test
  void ratio() {
    assertEquals(0.0, Units.ratio(0.0));
    assertEquals(0.556, Units.ratio(0.555555));
    assertEquals(1.0, Units.ratio(1.0));
    assertThrows(IllegalArgumentException.class, () -> Units.ratio(-0.01));
    var ex = assertThrows(IllegalArgumentException.class, () -> Units.ratio(1.01));
    assertEquals("The value is not in range[0.0, 1.0]: 1.01", ex.getMessage());
  }

  @Test
  void count() {
    assertEquals(0, Units.count(0, 10));
    assertEquals(10_000, Units.count(10_000, 10_000));
    assertThrows(IllegalArgumentException.class, () -> Units.count(-1, 10));
    var ex = assertThrows(IllegalArgumentException.class, () -> Units.count(11, 10));
    assertEquals("The value is not in range[0, 10]: 11", ex.getMessage());
  }
}
