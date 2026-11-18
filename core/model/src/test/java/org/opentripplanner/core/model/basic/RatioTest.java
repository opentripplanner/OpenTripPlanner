package org.opentripplanner.core.model.basic;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RatioTest {

  @ParameterizedTest
  @ValueSource(doubles = { 0.0, 0.25, 0.5, 1.0 })
  void aRatioInTheRangeIsKept(double value) {
    assertThat(Ratio.of(value).asDouble()).isEqualTo(value);
  }

  @ParameterizedTest
  @ValueSource(doubles = { -0.001, 1.001, 2.0, -1.0 })
  void aRatioOutsideTheRangeIsRejected(double value) {
    assertThrows(IllegalArgumentException.class, () -> Ratio.of(value));
  }

  @Test
  void aRatioOutsideTheRangeIsReportedToTheErrorHandler() {
    var errors = new ArrayList<String>();

    var result = Ratio.of(1.5, errors::add);

    assertThat(result).isEmpty();
    assertThat(errors).hasSize(1);
    assertThat(errors.getFirst()).contains("1.5");
  }

  @Test
  void aRatioInTheRangeIsNotReportedToTheErrorHandler() {
    var errors = new ArrayList<String>();

    var result = Ratio.of(0.5, errors::add);

    assertThat(result).isPresent();
    assertThat(errors).isEmpty();
  }

  @Test
  void aBoxedRatioThatIsNullIsEmpty() {
    var errors = new ArrayList<String>();

    assertThat(Ratio.ofBoxed(null, errors::add)).isEmpty();
    assertThat(errors).isEmpty();
    assertThat(Ratio.ofBoxed(0.4, errors::add).orElseThrow().asDouble()).isEqualTo(0.4);
  }

  @Test
  void theRatioIsRoundedToThreeDecimals() {
    assertThat(Ratio.of(0.12345).asDouble()).isEqualTo(0.123);
    assertThat(Ratio.of(0.12351).asDouble()).isEqualTo(0.124);
  }

  @Test
  void equalRatiosAreEqual() {
    assertThat(Ratio.of(0.5)).isEqualTo(Ratio.of(0.5));
    assertThat(Ratio.of(0.5).hashCode()).isEqualTo(Ratio.of(0.5).hashCode());
    assertThat(Ratio.of(0.5)).isNotEqualTo(Ratio.of(0.6));
    assertThat(Ratio.of(0.5)).isNotEqualTo(null);
  }

  @Test
  void aRatioIsNotEqualToTheNormalizedRatioOfTheSameValue() {
    assertThat(Ratio.of(0.5)).isNotEqualTo(Ratio.of(0.5).normalize());
  }

  @Test
  void toStringIsTheValue() {
    assertThat(Ratio.of(0.5).toString()).isEqualTo("0.5");
  }

  @Test
  void normalizeRoundsToAStepOfFiveHundredths() {
    assertThat(Ratio.of(0.12).normalize().asDouble()).isEqualTo(0.1);
    assertThat(Ratio.of(0.13).normalize().asDouble()).isEqualTo(0.15);
    assertThat(Ratio.of(0.0).normalize().asDouble()).isEqualTo(0.0);
    assertThat(Ratio.of(1.0).normalize().asDouble()).isEqualTo(1.0);
  }
}
