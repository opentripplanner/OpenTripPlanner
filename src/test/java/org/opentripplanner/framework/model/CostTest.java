package org.opentripplanner.framework.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CostTest {

  private static final int VALUE_SECONDS = 10;
  private final Cost subject = Cost.costOfSeconds(VALUE_SECONDS);

  @Test
  void testFactoryMethods() {
    var a = Cost.costOfSeconds(11);
    var b = Cost.costOfSeconds(11.004);
    var c = Cost.costOfSeconds(10.995);

    assertEquals(a, b);
    assertEquals(a, c);
  }

  @Test
  void plus() {
    assertEquals("$13", subject.plus(Cost.costOfSeconds(3)).toString());
  }

  @Test
  void minus() {
    assertEquals("$7", subject.minus(Cost.costOfSeconds(3)).toString());
  }

  @Test
  void testMultiply() {
    assertEquals("$30", subject.multiply(3).toString());
    assertEquals("$13", subject.multiply(1.25).toString());
    assertEquals("$13", subject.multiply(1.34).toString());
  }

  @Test
  void testToString() {
    assertEquals("$10", subject.toString());
  }

  @Test
  void testHashCodeAndEquals() {
    var same = Cost.costOfSeconds(VALUE_SECONDS);
    assertEquals(subject, same);
    assertEquals(subject.hashCode(), same.hashCode());

    var other = subject.multiply(1.1);
    assertNotEquals(subject, other);
    assertNotEquals(subject.hashCode(), other.hashCode());
  }

  @Test
  void testCompareTo() {
    var c3 = Cost.costOfSeconds(3);
    var c7 = Cost.costOfSeconds(7);
    var c8 = Cost.costOfSeconds(8);

    assertEquals(List.of(c3, c7), Stream.of(c7, c3).sorted().toList());
    assertEquals(List.of(c3, c7, c8), Stream.of(c8, c3, c7).sorted().toList());
  }
}
