package org.opentripplanner.core.model.basic;

import static com.google.common.truth.Truth.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class NormalizedRatioTest {

  private static final float EPSILON = 0.0001f;

  private static NormalizedRatio normalized(double value) {
    return Ratio.of(value).normalize();
  }

  @ParameterizedTest(name = "normalize({0}) = {1}")
  @CsvSource({
    "0.0, 0.0",
    "0.02, 0.0",
    "0.03, 0.05",
    "0.12, 0.1",
    "0.13, 0.15",
    "0.5, 0.5",
    "0.97, 0.95",
    "0.98, 1.0",
    "1.0, 1.0",
  })
  void theValueIsRoundedToAStepOfFiveHundredths(double input, double expected) {
    assertThat(normalized(input).asDouble()).isWithin(EPSILON).of(expected);
  }

  @Test
  void normalizingANormalizedRatioReturnsItself() {
    var subject = normalized(0.35);

    assertThat(subject.normalize()).isSameInstanceAs(subject);
  }

  @Test
  void ratiosThatRoundToTheSameStepAreEqual() {
    assertThat(normalized(0.11)).isEqualTo(normalized(0.09));
    assertThat(normalized(0.11).hashCode()).isEqualTo(normalized(0.09).hashCode());
    assertThat(normalized(0.11)).isNotEqualTo(normalized(0.2));
  }

  @Test
  void theDistanceToItselfIsZero() {
    assertThat(normalized(0.4).normalizedDistance(normalized(0.4))).isEqualTo(0f);
  }

  @Test
  void theDistanceIsTheDifferenceBetweenTheRatios() {
    assertThat(normalized(0.2).normalizedDistance(normalized(0.7)))
      .isWithin(EPSILON)
      .of(0.5f);
    assertThat(normalized(0.7).normalizedDistance(normalized(0.2)))
      .isWithin(EPSILON)
      .of(0.5f);
  }

  @Test
  void theDistanceIsInTheRangeFromZeroToOne() {
    assertThat(normalized(0.0).normalizedDistance(normalized(1.0))).isEqualTo(1f);
  }

  @Test
  void aSmallDifferenceThatRoundsToTheSameStepHasNoDistance() {
    assertThat(normalized(0.11).normalizedDistance(normalized(0.09))).isEqualTo(0f);
  }

  @Test
  void theNormalizedRatioCanBeSerialized() throws IOException, ClassNotFoundException {
    var subject = normalized(0.35);
    var bytes = new ByteArrayOutputStream();
    try (var out = new ObjectOutputStream(bytes)) {
      out.writeObject(subject);
    }

    try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
      assertThat(in.readObject()).isEqualTo(subject);
    }
  }
}
