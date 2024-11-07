package org.opentripplanner.raptor.rangeraptor.transit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.raptor.api.model.RaptorConstants.TIME_UNREACHED_FORWARD;

import org.junit.jupiter.api.Test;

public class ForwardTimeCalculatorTest {

  private final TimeCalculator subject = new ForwardTimeCalculator();

  @Test
  public void searchForward() {
    assertTrue(subject.searchForward());
  }

  @Test
  public void isBefore() {
    assertTrue(subject.isBefore(10, 11));
    assertFalse(subject.isBefore(11, 10));
    assertFalse(subject.isBefore(10, 10));
  }

  @Test
  public void isAfter() {
    assertTrue(subject.isAfter(11, 10));
    assertFalse(subject.isAfter(10, 11));
    assertFalse(subject.isAfter(10, 10));
  }

  @Test
  public void duration() {
    assertEquals(600, subject.plusDuration(500, 100));
    assertEquals(400, subject.minusDuration(500, 100));
    assertEquals(400, subject.duration(100, 500));
  }

  @Test
  public void unreachedTime() {
    assertEquals(TIME_UNREACHED_FORWARD, subject.unreachedTime());
  }
}
