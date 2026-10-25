package org.opentripplanner.transit.transfer.regular.api.parameters;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class MaxDurationsPreferenceTest {

  @Test
  void keepsTheMaxDuration() {
    assertThat(MaxDurationsPreference.of(Duration.ofMinutes(45)).maxDuration()).isEqualTo(
      Duration.ofMinutes(45)
    );
  }

  @Test
  void equalDurationsAreEqual() {
    assertThat(MaxDurationsPreference.of(Duration.ofMinutes(30))).isEqualTo(
      MaxDurationsPreference.DEFAULT
    );
  }

  @Test
  void theMaxDurationMustBePositive() {
    assertThrows(IllegalArgumentException.class, () -> MaxDurationsPreference.of(Duration.ZERO));
    assertThrows(IllegalArgumentException.class, () ->
      MaxDurationsPreference.of(Duration.ofMinutes(-1))
    );
  }
}
