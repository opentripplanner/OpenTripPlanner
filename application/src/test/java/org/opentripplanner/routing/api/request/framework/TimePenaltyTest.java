package org.opentripplanner.routing.api.request.framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.utils.time.DurationUtils;

class TimePenaltyTest {

  public static final Duration D2m = Duration.ofMinutes(2);

  @Test
  void parse() {
    assertEquals(TimePenalty.of(D2m, 3.0), TimePenalty.of("2m+3t"));
    assertEquals(TimePenalty.ZERO, TimePenalty.of(null));
    assertEquals(TimePenalty.ZERO, TimePenalty.of(""));
  }

  @Test
  void parsePenaltyRandomInputNotAllowed() {
    assertThrows(IllegalArgumentException.class, () -> TimePenalty.of("xyz"));
  }

  @Test
  void negativeDurationNotAllowed() {
    assertThrows(IllegalArgumentException.class, () -> TimePenalty.of("-2m + 1.0 t"));
    assertThrows(IllegalArgumentException.class, () ->
      TimePenalty.of(DurationUtils.duration("-2m"), 1.0)
    );
  }

  @Test
  void parsePenaltyTimeCoefficientMustBeAtLeastZeroAndLessThanTen() {
    assertThrows(IllegalArgumentException.class, () -> TimePenalty.of(D2m, -0.01));
    var ex = assertThrows(IllegalArgumentException.class, () ->
      TimePenalty.of(Duration.ZERO, 100.1)
    );
    assertEquals("The value is not in range[0.0, 100.0]: 100.1", ex.getMessage());
  }

  @Test
  void testToStringIsParsableAndCanBeUsedForSerialization() {
    var original = TimePenalty.of(D2m, 1.7);
    var copy = TimePenalty.of(original.toString());
    assertEquals(original, copy);
  }

  @Test
  void isZero() {
    assertTrue(TimePenalty.ZERO.isZero());
    assertTrue(TimePenalty.of("0s + 0t").isZero());
    assertFalse(TimePenalty.of("1s + 0t").isZero());
    assertFalse(TimePenalty.of("0s + 0.1t").isZero());
  }

  @Test
  void calculate() {
    var subject = TimePenalty.of(D2m, 0.5);
    assertEquals(120 + 150, subject.calculate(Duration.ofMinutes(5)).toSeconds());
  }

  @Test
  void modifies() {
    var subject = TimePenalty.of(D2m, 0.5);
    assertTrue(subject.modifies());
    assertFalse(TimePenalty.ZERO.modifies());
  }
}
