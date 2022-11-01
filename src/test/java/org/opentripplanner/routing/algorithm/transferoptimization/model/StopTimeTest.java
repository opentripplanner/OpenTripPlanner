package org.opentripplanner.routing.algorithm.transferoptimization.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class StopTimeTest {

  private final StopTime subject = StopTime.stopTime(1, 600);

  @Test
  public void stop() {
    assertEquals(1, subject.stop());
  }

  @Test
  public void time() {
    assertEquals(600, subject.time());
  }

  @Test
  public void duration() {
    var later = StopTime.stopTime(subject.stop(), subject.time() + 300);
    assertEquals(300, subject.duration(later));
    assertEquals(-300, later.duration(subject));
  }

  @Test
  public void testToString() {
    assertEquals("[1 0:10]", subject.toString());
  }

  @Test
  public void testEquals() {
    var same = StopTime.stopTime(subject.stop(), subject.time());

    assertEquals(same, subject);
    assertEquals(same.hashCode(), subject.hashCode());
  }
}
