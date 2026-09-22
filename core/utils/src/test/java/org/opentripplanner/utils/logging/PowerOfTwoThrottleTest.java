package org.opentripplanner.utils.logging;

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.utils.lang.IntBox;

class PowerOfTwoThrottleTest {

  @Test
  void smokeTest() {
    var subject = new PowerOfTwoThrottle();

    assertCounterIncremented(subject);
    IntStream.of(1, 3, 7, 15, 31, 63, 127, 255, 511).forEach(skip -> {
      assertCounterIncremented(subject);
      skipNextNLogEvents(subject, skip);
    });
  }

  private static void assertCounterIncremented(PowerOfTwoThrottle subject) {
    IntBox value = new IntBox(0);
    subject.throttle(value::set);
    assertNotEquals(0, value.get(), "Value not set, throttle did not execute the body");
  }

  private static void skipNextNLogEvents(PowerOfTwoThrottle subject, int n) {
    for (int i = 0; i < n; i++) {
      subject.throttle(v -> {
        Assertions.fail("Not expected: " + v);
      });
    }
  }
}
