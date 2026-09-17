package org.opentripplanner.utils.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UnitsTest {

  @Test
  void reluctance() {
    assertEquals(0.0, Units.reluctance(0.0));
    assertEquals(1.1, Units.reluctance(1.1234));
    assertEquals(2.1, Units.reluctance(2.1234));
    assertEquals(10.0, Units.reluctance(10.1234));
    var ex = assertThrows(IllegalArgumentException.class, () -> Units.reluctance(-0.01));
    assertEquals("The value is not in range[0.0, 1.7976931348623157E308]: -0.01", ex.getMessage());
  }

  @Test
  void normalizedFactor() {
    assertEquals(0.0, Units.normalizedFactor(0.0, 0.0, 8.0));
    // Step 0.1 below 3.0.
    assertEquals(1.9, Units.normalizedFactor(1.94, 0.0, 10.0));
    // Tie at 1.95: HALF_EVEN -> 20 (even) -> 2.0.
    assertEquals(2.0, Units.normalizedFactor(1.95, 0.0, 10.0));
    // Tie at 1.85: HALF_EVEN -> 18 (even) -> 1.8.
    assertEquals(1.8, Units.normalizedFactor(1.85, 0.0, 10.0));
    // Step 0.5 in [3.0, 10.0).
    assertEquals(3.0, Units.normalizedFactor(3.2, 0.0, 10.0));
    assertEquals(3.5, Units.normalizedFactor(3.3, 0.0, 10.0));
    // Step 1.0 at and above 10.0.
    assertEquals(10.0, Units.normalizedFactor(10.4, 0.0, 100.0));
    // Tie at 10.5: HALF_EVEN -> 10 (even).
    assertEquals(10.0, Units.normalizedFactor(10.5, 0.0, 100.0));
    // Tie at 11.5: HALF_EVEN -> 12 (even).
    assertEquals(12.0, Units.normalizedFactor(11.5, 0.0, 100.0));
    // BigDecimal-pinned rounding: 2.05 / 0.1 is exactly 20.5, rounds to 20 (even) -> 2.0.
    assertEquals(2.0, Units.normalizedFactor(2.05, 0.0, 10.0));
    // Negative values: step is picked from absolute value, ties still go to even.
    assertEquals(-1.9, Units.normalizedFactor(-1.94, -10.0, 0.0));
    assertEquals(-2.0, Units.normalizedFactor(-2.05, -10.0, 0.0));
    assertEquals(-3.5, Units.normalizedFactor(-3.3, -10.0, 0.0));
    assertEquals(-10.0, Units.normalizedFactor(-10.5, -100.0, 0.0));
    var ex = assertThrows(IllegalArgumentException.class, () ->
      Units.normalizedFactor(0.999, 1.0, 8.0)
    );
    assertEquals("The value is not in range[1.0, 8.0]: 1.0", ex.getMessage());
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
    // Step 0.05 below 2 m/s.
    assertEquals(1.10, Units.speed(1.1234));
    assertEquals(1.40, Units.speed(1.38));
    assertEquals(1.40, Units.speed(1.42));
    // Tie at 1.425: 1.425 / 0.05 = 28.5, HALF_EVEN -> 28 (even) -> 1.40.
    assertEquals(1.40, Units.speed(1.425));
    // Tie at 1.475: 1.475 / 0.05 = 29.5, HALF_EVEN -> 30 (even) -> 1.50.
    assertEquals(1.50, Units.speed(1.475));
    // Step 0.1 in [2, 10).
    assertEquals(2.1, Units.speed(2.1234));
    // Step 1.0 at and above 10 m/s.
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
