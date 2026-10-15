package org.opentripplanner.utils.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IntRangeTest {

  @Test
  void plus() {
    assertEquals(range(10, 20), range(5, 15).plus(5));
  }

  @Test
  void minus() {
    assertEquals(range(2, 10), range(7, 15).minus(5));
  }

  @Test
  void testIntersect() {
    assertEquals(range(2, 7), range(1, 15).intersect(range(2, 7)).orElseThrow());
    assertEquals(range(2, 7), range(2, 7).intersect(range(1, 15)).orElseThrow());
    assertEquals(range(2, 7), range(1, 7).intersect(range(2, 15)).orElseThrow());
    assertEquals(range(2, 7), range(2, 15).intersect(range(1, 7)).orElseThrow());

    assertEquals(range(5, 5), range(2, 5).intersect(range(5, 7)).orElseThrow());
    assertEquals(range(5, 5), range(5, 7).intersect(range(2, 5)).orElseThrow());

    assertTrue(range(2, 5).intersect(range(6, 7)).isEmpty());
    assertTrue(range(6, 7).intersect(range(2, 5)).isEmpty());
  }

  @Test
  void testContains() {
    var w = range(2, 4);

    assertFalse(w.contains(1));
    assertTrue(w.contains(2));
    assertTrue(w.contains(4));
    assertFalse(w.contains(5));
  }

  @Test
  void isOutside() {
    var w = range(2, 4);

    assertTrue(w.isOutside(1));
    assertFalse(w.isOutside(2));
    assertFalse(w.isOutside(4));
    assertTrue(w.isOutside(5));
  }

  @Test
  void testEqualsAndHashCode() {
    var subject = range(5, 12);
    var same = range(5, 12);
    var otherStart = range(6, 12);
    var otherEnd = range(5, 11);

    assertEquals(same, subject);
    assertEquals(same.hashCode(), subject.hashCode());

    assertNotEquals(otherStart, subject);
    assertNotEquals(otherStart.hashCode(), subject.hashCode());

    assertNotEquals(otherEnd, subject);
    assertNotEquals(otherEnd.hashCode(), subject.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("[2, 17]", range(2, 17).toString());
    assertEquals("[v2, v17]", range(2, 17).toString(i -> "v" + i));
  }

  @Test
  void start() {
    assertEquals(2, range(2, 17).startInclusive());
  }

  @Test
  void end() {
    assertEquals(17, range(2, 17).endInclusive());
  }

  private IntRange range(int a, int b) {
    return IntRange.ofInclusive(a, b);
  }
}
