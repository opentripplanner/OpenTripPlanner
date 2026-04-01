package org.opentripplanner.framework.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.core.model.basic.Cost.ONE_HOUR_WITH_TRANSIT;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.opentripplanner._support.asserts.AssertEqualsAndHashCode;
import org.opentripplanner.core.model.basic.Cost;

class CostTest {

  private static final int VALUE_CENTI_SECONDS = 1001;
  private final Cost subject = Cost.costOfCentiSeconds(VALUE_CENTI_SECONDS);

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
  void testToSeconds() {
    assertEquals(3, Cost.costOfCentiSeconds(300).toSeconds());
    assertEquals(3, Cost.costOfCentiSeconds(349).toSeconds());
    assertEquals(4, Cost.costOfCentiSeconds(350).toSeconds());
  }

  @Test
  void testToCentiSeconds() {
    assertEquals(VALUE_CENTI_SECONDS, subject.toCentiSeconds());
  }

  @Test
  void isZero() {
    assertTrue(Cost.costOfSeconds(0).isZero());
    assertFalse(subject.isZero());
  }

  @Test
  void asDuration() {
    assertEquals(Duration.ofMillis(10 * VALUE_CENTI_SECONDS), subject.asDuration());
  }

  @Test
  void plus() {
    assertEquals("$13.01", subject.plus(Cost.costOfSeconds(3)).toString());
  }

  @Test
  void minus() {
    assertEquals("$7.01", subject.minus(Cost.costOfSeconds(3)).toString());
  }

  @Test
  void testMultiply() {
    assertEquals("$30.03", subject.multiply(3).toString());
    assertEquals("$12.51", subject.multiply(1.25).toString());
    assertEquals("$13.41", subject.multiply(1.34).toString());
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
  void normalize() {
    var c2 = Cost.normalizedCost(2);

    assertEquals(c2, Cost.costOfSeconds(2).normalize());
    assertEquals(c2, Cost.costOfCentiSeconds(150).normalize());
    assertEquals(c2, Cost.costOfCentiSeconds(249).normalize());

    assertNotEquals(c2, Cost.costOfCentiSeconds(149).normalize());
    assertNotEquals(c2, Cost.costOfCentiSeconds(250).normalize());
  }

  @Test
  void testToString() {
    assertEquals("$10.01", subject.toString());
    assertEquals("$10", subject.normalize().toString());
  }

  @Test
  void testHashCodeAndEquals() {
    var same = Cost.costOfCentiSeconds(VALUE_CENTI_SECONDS);
    var other = subject.multiply(1.1);

    AssertEqualsAndHashCode.verify(subject).sameAs(same).differentFrom(other);

    AssertEqualsAndHashCode.verify(subject.normalize())
      .sameAs(same.normalize())
      .differentFrom(other.normalize());
  }

  @Test
  void testCompareTo() {
    var c3 = Cost.costOfCentiSeconds(3);
    var c7 = Cost.costOfCentiSeconds(7);
    var c8 = Cost.costOfCentiSeconds(8);

    assertEquals(List.of(c3, c7), Stream.of(c7, c3).sorted().toList());
    assertEquals(List.of(c3, c7, c8), Stream.of(c8, c3, c7).sorted().toList());
  }
}
