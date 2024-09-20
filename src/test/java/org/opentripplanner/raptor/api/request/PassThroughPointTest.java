package org.opentripplanner.raptor.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PassThroughPointTest {

  private static final int[] STOPS = { 2, 7, 13 };

  private final PassThroughPoint subject = new PassThroughPoint("PT1", STOPS);

  @Test
  void asBitSet() {
    var bitSet = subject.asBitSet();

    // Sample some all set values as well as some not set values
    assertFalse(bitSet.get(0));
    assertTrue(bitSet.get(2));
    assertFalse(bitSet.get(3));
    assertFalse(bitSet.get(6));
    assertTrue(bitSet.get(7));
    assertTrue(bitSet.get(13));
    assertFalse(bitSet.get(15000000));
  }

  @Test
  void testEqualsAndHashCode() {
    var same = new PassThroughPoint("PT1", STOPS);
    var other = new PassThroughPoint("PT2", 2, 7);

    assertEquals(subject, subject);
    assertEquals(same, subject);
    assertNotEquals(other, subject);

    assertEquals(same.hashCode(), subject.hashCode());
    assertNotEquals(other.hashCode(), subject.hashCode());
  }

  @Test
  void testToString() {
    assertEquals("(PT1, stops: 2, 7, 13)", subject.toString());
    assertEquals("(stops: 2, 7, 13)", new PassThroughPoint(" ", STOPS).toString());
    assertEquals("(stops: 2, 7, 13)", new PassThroughPoint(null, STOPS).toString());
  }
}
