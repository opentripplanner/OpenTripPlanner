package org.opentripplanner.raptor.rangeraptor.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IntArraySingleCriteriaArrivalsTest {

  public static final int UNREACHED = -1;
  public static final int A = 3;
  public static final int B = 7;
  private final IntArraySingleCriteriaArrivals subject = new IntArraySingleCriteriaArrivals(
    UNREACHED,
    new int[] { A, UNREACHED, B }
  );

  @Test
  void value() {
    assertEquals(A, subject.value(0));
    assertEquals(UNREACHED, subject.value(1));
    assertEquals(B, subject.value(2));
  }

  @Test
  void isReached() {
    assertTrue(subject.isReached(0));
    assertFalse(subject.isReached(1));
    assertTrue(subject.isReached(2));
  }
}
