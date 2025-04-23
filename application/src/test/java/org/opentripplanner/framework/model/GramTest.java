package org.opentripplanner.framework.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GramTest {

  private static final double VALUE = 7.5;
  private static final int VALUE_ONE = 1;

  private Gram one = Gram.of(VALUE_ONE);
  private Gram subject = Gram.of(VALUE);

  @Test
  void ofNullable() {
    assertEquals(Gram.ZERO, Gram.ofNullable(null));
    assertEquals(subject, Gram.ofNullable(VALUE));
  }

  @Test
  void plus() {
    assertEquals(Gram.of(VALUE), subject.plus(Gram.ZERO));
    assertEquals(Gram.of(VALUE + VALUE_ONE), subject.plus(one));
  }

  @Test
  void multiply() {
    assertEquals(Gram.of(VALUE * 2), subject.multiply(2.0));
    assertEquals(Gram.of(VALUE * 2), subject.multiply(2));
  }

  @Test
  void dividedBy() {
    assertEquals(Gram.of(VALUE / 2), subject.dividedBy(2));
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
    // Within precission (100)
    var less = Gram.of(VALUE - 0.001);

    assertTrue(subject.compareTo(same) == 0);
    assertTrue(less.compareTo(subject) < 0);
    assertTrue(subject.compareTo(less) > 0);

    // The delta is to small (precission is 100)
    var sameValue = Gram.of(VALUE - 0.00049);
    assertEquals(subject, sameValue);
    assertTrue(sameValue.compareTo(subject) == 0);
  }

  @Test
  void testToString() {
    assertEquals("7.5g", subject.toString());
    assertEquals("1mg", Gram.of(0.001).toString());
    assertEquals("999mg", Gram.of(0.999).toString());
    assertEquals("1.001g", Gram.of(1.001).toString());
    assertEquals("19g", Gram.of(19).toString());
    assertEquals("999g", Gram.of(999).toString());
    assertEquals("1kg", Gram.of(1000).toString());
    assertEquals("1001g", Gram.of(1001).toString());
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

  @Test
  void ofString() {
    assertEquals(Gram.ZERO, Gram.of("0"));
    assertEquals(200.0, Gram.of("200").asDouble());

    assertEquals(Gram.ZERO, Gram.of("0g"));
    assertEquals(1.0, Gram.of("1g").asDouble());
    assertEquals(200.0, Gram.of("200 g").asDouble());

    assertEquals(Gram.ZERO, Gram.of("0kg"));
    assertEquals(1000.0, Gram.of("1kg").asDouble());
    assertEquals(2200.0, Gram.of("2.2 kg").asDouble());

    assertEquals(Gram.ZERO, Gram.of("0mg"));
    assertEquals(0.001, Gram.of("1mg").asDouble());
    assertEquals(0.023, Gram.of("23 mg").asDouble());

    var ex = assertThrows(IllegalArgumentException.class, () -> Gram.of("200.0 tonn"));
    assertEquals("Parse error! Illegal gram value: '200.0 tonn'", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () -> Gram.of("0x001 g"));
    assertEquals("Parse error! Illegal gram value: '0x001 g'", ex.getMessage());

    ex = assertThrows(IllegalArgumentException.class, () -> Gram.of("1 gk"));
    assertEquals("Parse error! Illegal gram value: '1 gk'", ex.getMessage());
  }
}
