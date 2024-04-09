package org.opentripplanner.ext.flex.trip;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class DurationModifierTest {

  @Test
  void doesNotModify() {
    assertFalse(DurationModifier.NONE.modifies());
  }

  @Test
  void modifies() {
    assertTrue(new DurationModifier(Duration.ofMinutes(1), 1.5f).modifies());
  }
}