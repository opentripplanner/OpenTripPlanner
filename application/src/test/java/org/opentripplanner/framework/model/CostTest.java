package org.opentripplanner.framework.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.framework.model.Cost.ONE_HOUR_WITH_TRANSIT;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class CostTest {

  private static final int VALUE_SECONDS = 10;
  private final Cost subject = Cost.costOfSeconds(VALUE_SECONDS);

  @Test
  void oneHourWithTransit() {
    assertEquals(60 * 60, ONE_HOUR_WITH_TRANSIT.toSeconds());
  }

  @Test
  void testCostOfSeconds() {
    var c11 = Cost.costOfSeconds(11);
    assertNotEquals(c11, Cost.costOfSeconds(10.99499));
    assertEquals(c11, Cost.costOfSeconds(10.99500));
    assertEquals(c11, Cost.costOfSeconds(11.00499));
    assertNotEquals(c11, Cost.costOfSeconds(11.00500));
  }

  @Test
  void testCostOfMinutes() {
    assertEquals(660, Cost.costOfMinutes(11).toSeconds());
  }

  @Test
  void testFromDuration() {
    Cost c11 = Cost.costOfSeconds(11);
    assertNotEquals(c11, Cost.fromDuration(Duration.ofMillis(10994)));
    assertEquals(c11, Cost.fromDuration(Duration.ofMillis(10995)));
    assertEquals(c11, Cost.fromDuration(Duration.ofMillis(11004)));
    assertNotEquals(c11, Cost.fromDuration(Duration.ofMillis(11005)));
  }

  @Test
  void testFactoryMethods() {
    var a = Cost.costOfSeconds(11);
    var b = Cost.costOfSeconds(11.004);
    var c = Cost.costOfSeconds(10.995);
    var d = Cost.fromDuration(Duration.ofMillis(11004));

    assertEquals(a, b);
    assertEquals(a, c);
    assertEquals(a, d);
  }

  @Test
  void testToCentiSeconds() {
    assertEquals(100 * VALUE_SECONDS, subject.toCentiSeconds());
  }

  @Test
  void isZero() {
    assertTrue(Cost.costOfSeconds(0).isZero());
    assertFalse(subject.isZero());
  }

  @Test
  void asDuration() {
    assertEquals(Duration.ofSeconds(VALUE_SECONDS), subject.asDuration());
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
    assertEquals("$12.50", subject.multiply(1.25).toString());
    assertEquals("$13.40", subject.multiply(1.34).toString());
  }

  @Test
  void testLessThanAndGreaterThenFunctions() {
    Cost same = Cost.costOfCentiSeconds(subject.toCentiSeconds());
    Cost smaller = Cost.costOfCentiSeconds(subject.toCentiSeconds() - 1);
    Cost bigger = Cost.costOfCentiSeconds(subject.toCentiSeconds() + 1);

    assertTrue(subject.greaterThan(smaller));
    assertFalse(subject.greaterThan(same));
    assertFalse(subject.greaterThan(bigger));

    assertTrue(subject.greaterOrEq(smaller));
    assertTrue(subject.greaterOrEq(same));
    assertFalse(subject.greaterOrEq(bigger));

    assertFalse(subject.lessThan(smaller));
    assertFalse(subject.lessThan(same));
    assertTrue(subject.lessThan(bigger));

    assertFalse(subject.lessOrEq(smaller));
    assertTrue(subject.lessOrEq(same));
    assertTrue(subject.lessOrEq(bigger));
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
