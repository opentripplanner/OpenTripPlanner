package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RelaxTest {

  @Test
  void testConstructor() {
    new Relax(0.9951, 0);
    new Relax(4.0, 2_000_000_000);
    assertThrows(IllegalArgumentException.class, () -> new Relax(0.9949, 0));
    assertThrows(IllegalArgumentException.class, () -> new Relax(4.01, 300));
    assertThrows(IllegalArgumentException.class, () -> new Relax(2.1, -1));
  }

  @Test
  void hasNoEffect() {
    assertTrue(new Relax(1.0049, 0).hasNoEffect());
    assertFalse(new Relax(1.01, 0).hasNoEffect());
  }

  @Test
  void hasEffect() {
    assertTrue(new Relax(1.01, 0).hasEffect());
    assertFalse(new Relax(1.0049, 0).hasEffect());
  }

  @Test
  void testToString() {
    assertEquals("0 + 1.00 * x", Relax.NORMAL.toString());
    assertEquals("300 + 2.10 * x", new Relax(2.1, 300).toString());
  }

  @Test
  void ratio() {
    assertEquals(2.0, new Relax(2.0, 333).ratio());
  }

  @Test
  void slack() {
    assertEquals(333, new Relax(2.0, 333).slack());
  }
}
