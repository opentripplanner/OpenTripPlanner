package org.opentripplanner.raptor.api.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class RelaxFunctionTest {

  @Test
  void relax() {
    assertEquals(100, new RelaxFunction(1.0, 0).relax(100));
    assertEquals(75, new RelaxFunction(1.5, 0).relax(50));
    assertEquals(100, new RelaxFunction(1.0, 50).relax(50));
    assertEquals(150, new RelaxFunction(1.25, 25).relax(100));
    assertEquals(16, new RelaxFunction(32.99/32.0, 0).relax(16));
    assertEquals(17, new RelaxFunction(33.01/32.0, 0).relax(16));
  }

  @Test
  void testToString() {
    assertEquals("f()=16/16 * v + 12", new RelaxFunction(1.0, 12).toString());
  }

  @Test
  void testEqualsAndHashCode() {
    var a = new RelaxFunction(1.0, 12);
    var same = new RelaxFunction(1.0, 12);
    var diffRatio = new RelaxFunction(17.0/16.0, 12);
    var diffSlack = new RelaxFunction(1.0, 13);

    assertEquals(a, a);
    assertEquals(a, same);
    assertEquals(a.hashCode(), same.hashCode());
    assertNotEquals(a, diffRatio);
    assertNotEquals(a.hashCode(), diffRatio.hashCode());
    assertNotEquals(a, diffSlack);
    assertNotEquals(a.hashCode(), diffSlack.hashCode());
  }
}