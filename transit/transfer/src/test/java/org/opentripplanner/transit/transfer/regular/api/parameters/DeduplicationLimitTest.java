package org.opentripplanner.transit.transfer.regular.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Cost;

class DeduplicationLimitTest {

  /** 30 seconds and 0.5%, the default. The Raptor cost unit is a centi-second. */
  private static final DeduplicationLimit SUBJECT = DeduplicationLimit.DEFAULT;

  @Test
  void theToleranceIsTheConstantPlusAShareOfTheCost() {
    // 30s = 3000, and 0.5% of 100_000 (1000s) = 500
    assertThat(SUBJECT.toleranceC1(100_000)).isEqualTo(3_500);
    assertThat(SUBJECT.toleranceC1(0)).isEqualTo(3_000);
  }

  @Test
  void aPathWithinTheToleranceIsEquivalent() {
    assertThat(SUBJECT.isEquivalent(103_000, 100_000)).isTrue();
    assertThat(SUBJECT.isEquivalent(103_600, 100_000)).isFalse();
  }

  @Test
  void theToleranceDecidesBothWays() {
    // The path of the parent is cheaper than the best path of the profile - it is close enough
    assertThat(SUBJECT.isEquivalent(97_500, 100_000)).isTrue();
    assertThat(SUBJECT.isEquivalent(90_000, 100_000)).isFalse();
  }

  @Test
  void aLimitOfZeroOnlyAcceptsEqualCosts() {
    var subject = new DeduplicationLimit(Cost.ZERO, 0.0);

    assertThat(subject.isEquivalent(100, 100)).isTrue();
    assertThat(subject.isEquivalent(101, 100)).isFalse();
  }

  @Test
  void theValuesMustNotBeNegative() {
    assertThrows(IllegalArgumentException.class, () ->
      new DeduplicationLimit(Cost.costOfSeconds(-1), 0.0)
    );
    assertThrows(IllegalArgumentException.class, () -> new DeduplicationLimit(Cost.ZERO, -0.1));
  }
}
