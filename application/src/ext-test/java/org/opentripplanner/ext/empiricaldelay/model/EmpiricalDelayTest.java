package org.opentripplanner.ext.empiricaldelay.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Serializable;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class EmpiricalDelayTest {

  private static final int p50 = 10;
  private static final int p90 = 50;

  private final EmpiricalDelay subject = new EmpiricalDelay(p50, p90);

  @Test
  void p50() {
    assertEquals(Duration.ofSeconds(p50), subject.p50());
  }

  @Test
  void p90() {
    assertEquals(Duration.ofSeconds(p90), subject.p90());
  }

  @Test
  void testToString() {
    assertEquals("[10s, 50s]", subject.toString());
  }

  @Test
  void isSerializable() {
    assertTrue(subject instanceof Serializable);
  }
}
