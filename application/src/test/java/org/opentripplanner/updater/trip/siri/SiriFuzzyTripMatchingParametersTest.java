package org.opentripplanner.updater.trip.siri;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class SiriFuzzyTripMatchingParametersTest {

  @Test
  void ofBoolean() {
    assertSame(SiriFuzzyTripMatchingParameters.EXACT, SiriFuzzyTripMatchingParameters.of(true));
    assertSame(SiriFuzzyTripMatchingParameters.DISABLED, SiriFuzzyTripMatchingParameters.of(false));
  }

  @Test
  void exactIsEnabledWithZeroTolerance() {
    assertTrue(SiriFuzzyTripMatchingParameters.EXACT.enabled());
    assertEquals(Duration.ZERO, SiriFuzzyTripMatchingParameters.EXACT.timeTolerance());
    assertFalse(SiriFuzzyTripMatchingParameters.DISABLED.enabled());
  }

  @Test
  void ofTimeTolerance() {
    var subject = SiriFuzzyTripMatchingParameters.ofTimeTolerance(Duration.ofSeconds(59));
    assertTrue(subject.enabled());
    assertEquals(Duration.ofSeconds(59), subject.timeTolerance());
  }

  @Test
  void invalidTimeTolerance() {
    assertThrows(IllegalArgumentException.class, () ->
      SiriFuzzyTripMatchingParameters.ofTimeTolerance(Duration.ofSeconds(-1))
    );
    assertThrows(IllegalArgumentException.class, () ->
      SiriFuzzyTripMatchingParameters.ofTimeTolerance(Duration.ofMinutes(30))
    );
  }

  @Test
  void subSecondTimeToleranceIsRejected() {
    assertThrows(IllegalArgumentException.class, () ->
      SiriFuzzyTripMatchingParameters.ofTimeTolerance(Duration.ofMillis(500))
    );
    assertThrows(IllegalArgumentException.class, () ->
      SiriFuzzyTripMatchingParameters.ofTimeTolerance(Duration.ofMillis(59_500))
    );
  }

  @Test
  void disabledWithTimeToleranceIsRejected() {
    assertThrows(IllegalArgumentException.class, () ->
      new SiriFuzzyTripMatchingParameters(false, Duration.ofSeconds(59))
    );
  }

  @Test
  void testToString() {
    assertEquals(
      "SiriFuzzyTripMatchingParameters{disabled}",
      SiriFuzzyTripMatchingParameters.DISABLED.toString()
    );
    assertEquals(
      "SiriFuzzyTripMatchingParameters{timeTolerance: 59s}",
      SiriFuzzyTripMatchingParameters.ofTimeTolerance(Duration.ofSeconds(59)).toString()
    );
  }
}
