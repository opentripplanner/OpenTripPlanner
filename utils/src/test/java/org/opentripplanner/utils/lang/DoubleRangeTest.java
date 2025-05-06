package org.opentripplanner.utils.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DoubleRangeTest {

  public static final double START = 1.0;
  public static final double END = 2.0;
  private final DoubleRange subject = DoubleRange.of(START, END);

  @Test
  void testCreateNew() {
    var ex = assertThrows(IllegalArgumentException.class, () -> DoubleRange.of(2.0, 1.9));
    assertEquals(
      "The start of the range must be less then or equal to the end: [2.0 - 1.9)",
      ex.getMessage()
    );
  }

  @Test
  void start() {
    assertEquals(START, subject.start());
  }

  @Test
  void end() {
    assertEquals(END, subject.end());
  }

  @Test
  void contains() {
    assertTrue(subject.contains(START));
    assertTrue(subject.contains(END - 0.000_000_000_001));
    assertFalse(subject.contains(END));
  }

  @Test
  void testEqualsAndHashCode() {
    var same = DoubleRange.of(START, END);
    var o1 = DoubleRange.of(START, 1.7);
    var o2 = DoubleRange.of(1.7, END);

    assertEquals(same, subject);
    assertNotEquals(o1, subject);
    assertNotEquals(o2, subject);

    assertEquals(same.hashCode(), subject.hashCode());
    assertNotEquals(o1.hashCode(), subject.hashCode());
    assertNotEquals(o2.hashCode(), subject.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("[1.0 - 2.0)", subject.toString());
  }
}
