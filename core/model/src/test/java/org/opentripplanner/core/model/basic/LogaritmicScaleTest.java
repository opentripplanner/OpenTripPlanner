package org.opentripplanner.core.model.basic;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LogaritmicScaleTest {

  private static final float EPSILON = 0.00001f;

  @Test
  void constructorRejectsMinNotLessThanMax() {
    assertThrows(IllegalArgumentException.class, () -> new LogaritmicScale(5.0, 5.0));
    assertThrows(IllegalArgumentException.class, () -> new LogaritmicScale(5.0, 4.0));
  }

  @Test
  void normalizedDistance() {
    // The approximate value representing the normalized half distance between 0.0 and 25.0 is using a logaritmic
    // scale.
    double half = 4.099021;
    var subject = new LogaritmicScale(0.0, 25.0);

    assertThat(subject.normalizedDistance(0.0, 0.0)).isZero();
    assertThat(subject.normalizedDistance(10.0, 10.0)).isZero();
    assertThat(subject.normalizedDistance(25.0, 25.0)).isZero();

    assertThat(subject.normalizedDistance(0.0, 25.0)).isWithin(EPSILON).of(1.0f);
    assertThat(subject.normalizedDistance(25.0, 0.0)).isWithin(EPSILON).of(1.0f);

    assertThat(subject.normalizedDistance(0.0, half)).isWithin(EPSILON).of(0.5f);
    assertThat(subject.normalizedDistance(half, 0.0)).isWithin(EPSILON).of(0.5f);
    assertThat(subject.normalizedDistance(half, 25.0)).isWithin(EPSILON).of(0.5f);
    assertThat(subject.normalizedDistance(25.0, half)).isWithin(EPSILON).of(0.5f);
  }
}
