package org.opentripplanner.framework.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class IntBoxTest {

  private static final int VALUE = 7;

  private final IntBox subject = new IntBox(VALUE);

  @Test
  void get() {
    assertEquals(VALUE, subject.get());
  }

  @Test
  void set() {
    subject.set(137);
    assertEquals(137, subject.get());
  }

  @Test
  void inc() {
    subject.inc();
    assertEquals(VALUE + 1, subject.get());
  }

  @Test
  void testEqualsAndHashCode() {
    IntBox same = new IntBox(VALUE);
    IntBox other = new IntBox(VALUE + 1);

    // equals
    assertEquals(subject, subject);
    assertEquals(same, subject);
    assertNotEquals(other, subject);
    assertNotEquals(subject, null);
    assertNotEquals(subject, new Object());

    // hashCode
    assertEquals(subject.hashCode(), subject.hashCode());
    assertEquals(same.hashCode(), subject.hashCode());
    assertNotEquals(other.hashCode(), subject.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("" + VALUE, subject.toString());
  }
}
