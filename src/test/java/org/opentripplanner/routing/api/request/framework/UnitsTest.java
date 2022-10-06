package org.opentripplanner.routing.api.request.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UnitsTest {

  @Test
  void cost() {
    assertEquals(0, Units.cost(0));
    assertEquals(10_000, Units.cost(10_000));
    assertThrows(IllegalArgumentException.class, () -> Units.cost(-1));
  }

  @Test
  void reluctance() {
    assertEquals(0.0, Units.reluctance(0.0));
    assertEquals(1.12, Units.reluctance(1.1234));
    assertEquals(2.1, Units.reluctance(2.1234));
    assertEquals(10.0, Units.reluctance(10.1234));
    assertThrows(IllegalArgumentException.class, () -> Units.reluctance(-0.01));
  }

  @Test
  void duration() {
    assertEquals(0, Units.duration(0));
    assertEquals(10_000, Units.duration(10_000));
    assertThrows(IllegalArgumentException.class, () -> Units.duration(-1));
  }

  @Test
  void speed() {
    assertEquals(0.1, Units.speed(0.0));
    assertEquals(1.12, Units.speed(1.1234));
    assertEquals(2.1, Units.speed(2.1234));
    assertEquals(10.0, Units.speed(10.1234));
    assertThrows(IllegalArgumentException.class, () -> Units.speed(-0.01));
  }

  @Test
  void acceleration() {
    assertEquals(0.1, Units.acceleration(0.0));
    assertEquals(9.8, Units.acceleration(9.78888));
    assertThrows(IllegalArgumentException.class, () -> Units.acceleration(-0.01));
  }

  @Test
  void ratio() {
    assertEquals(0.0, Units.ratio(0.0));
    assertEquals(0.556, Units.ratio(0.555555));
    assertEquals(1.0, Units.ratio(1.0));
    assertThrows(IllegalArgumentException.class, () -> Units.ratio(-0.01));
    assertThrows(IllegalArgumentException.class, () -> Units.ratio(1.01));
  }

  @Test
  void count() {
    assertEquals(0, Units.count(0, 10));
    assertEquals(10_000, Units.count(10_000, 10_000));
    assertThrows(IllegalArgumentException.class, () -> Units.count(-1, 10));
    assertThrows(IllegalArgumentException.class, () -> Units.count(11, 10));
  }
}
