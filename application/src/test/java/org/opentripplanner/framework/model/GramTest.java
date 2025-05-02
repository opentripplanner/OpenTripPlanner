package org.opentripplanner.framework.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GramTest {

  private static final double VALUE = 7.5;
  private static final double VALUE_ONE = 1.0;

  private Gram one = Gram.of(VALUE_ONE);
  private Gram subject = Gram.of(VALUE);

  @Test
  void plus() {
    assertEquals(Gram.of(VALUE), subject.plus(Gram.ZERO));
    assertEquals(Gram.of(VALUE + VALUE_ONE), subject.plus(one));
  }

  @Test
  void multiply() {
    assertEquals(Gram.of(VALUE * 2), subject.multiply(2.0));
  }

  @Test
  void dividedBy() {
    assertEquals(Gram.of(VALUE / 2), subject.dividedBy(2.0));
  }

  @Test
  void testEqualsAndHashCode() {
    var same = Gram.of(VALUE);

    assertEquals(same, subject);
    assertEquals(same.hashCode(), subject.hashCode());
  }

  @Test
  void compareTo() {
    var same = Gram.of(VALUE);
    var less = Gram.of(VALUE - 0.000001);

    assertTrue(subject.compareTo(same) == 0);
    assertTrue(less.compareTo(subject) < 0);
    assertTrue(subject.compareTo(less) > 0);
  }

  @Test
  void testToString() {
    assertEquals("7.5g", subject.toString());
  }

  @Test
  void asDouble() {
    assertEquals(VALUE, subject.asDouble());
  }

  @Test
  void isZero() {
    assertTrue(Gram.ZERO.isZero());
    assertTrue(Gram.of(0).isZero());
    assertFalse(subject.isZero());
  }
}
