package org.opentripplanner.core.model.basic;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReluctanceTest {

  private static final double EPSILON = 0.001;

  // The reluctance is rounded to a step that grows with the value. The tests below check the
  // result for the edges of the range and of the buckets, not how the steps are defined.

  @ParameterizedTest(name = "of({0}) = {1}")
  @CsvSource({
    "0.0, 0.0",
    "0.02, 0.0",
    "0.03, 0.05",
    "0.1, 0.1",
    "0.12, 0.1",
    "0.13, 0.15",
    "1000000.0, 1000000.0",
    "999999.0, 1000000.0",
  })
  void ofRoundsTheValuesAtTheEdgesOfTheRange(double input, double expected) {
    assertThat(Reluctance.of(input).value()).isWithin(EPSILON).of(expected);
  }

  @ParameterizedTest(name = "of({0}) = {1}")
  @CsvSource({
    "0.5, 0.5",
    "1.0, 1.0",
    "2.0, 2.0",
    "5.0, 5.0",
    "10.0, 10.0",
    "20.0, 20.0",
    "50.0, 50.0",
    "100.0, 100.0",
    "200.0, 200.0",
    "500.0, 500.0",
    "1000.0, 1000.0",
    "2000.0, 2000.0",
    "5000.0, 5000.0",
    "10000.0, 10000.0",
    "100000.0, 100000.0",
  })
  void aValueAtTheLowerBoundOfABucketIsNotChanged(double input, double expected) {
    assertThat(Reluctance.of(input).value()).isWithin(EPSILON).of(expected);
  }

  @ParameterizedTest(name = "of({0}) = {1}")
  @CsvSource({
    "0.49, 0.5",
    "0.99, 1.0",
    "1.99, 2.0",
    "4.9, 5.0",
    "9.9, 10.0",
    "19.5, 20.0",
    "49.0, 50.0",
    "99.0, 100.0",
    "199.0, 200.0",
    "499.0, 500.0",
    "999.0, 1000.0",
    "1999.0, 2000.0",
    "4999.0, 5000.0",
    "9999.0, 10000.0",
    "99999.0, 100000.0",
  })
  void aValueJustBelowTheUpperBoundOfABucketIsRoundedUpToTheBound(double input, double expected) {
    assertThat(Reluctance.of(input).value()).isWithin(EPSILON).of(expected);
  }

  @ParameterizedTest(name = "of({0}) = {1}")
  @CsvSource({
    "0.51, 0.5",
    "1.01, 1.0",
    "2.2, 2.0",
    "5.4, 5.0",
    "10.9, 10.0",
    "21.0, 20.0",
    "52.0, 50.0",
    "104.0, 100.0",
    "215.0, 200.0",
    "520.0, 500.0",
    "1040.0, 1000.0",
    "2200.0, 2000.0",
    "5400.0, 5000.0",
    "12000.0, 10000.0",
    "149999.0, 100000.0",
  })
  void aValueJustAboveTheLowerBoundOfABucketIsRoundedBackToTheBound(double input, double expected) {
    assertThat(Reluctance.of(input).value()).isWithin(EPSILON).of(expected);
  }

  @ParameterizedTest(name = "of({0}) = {1}")
  @CsvSource({
    "0.137, 0.15",
    "0.37, 0.35",
    "0.7, 0.7",
    "1.37, 1.4",
    "1.6, 1.6",
    "3.3, 3.5",
    "7.3, 7.0",
    "14.0, 14.0",
    "33.0, 35.0",
    "73.0, 70.0",
    "73000.0, 70000.0",
    "140000.0, 100000.0",
    "949999.0, 900000.0",
  })
  void aValueInsideABucketIsRoundedToTheNearestStep(double input, double expected) {
    assertThat(Reluctance.of(input).value()).isWithin(EPSILON).of(expected);
  }

  @ParameterizedTest(name = "of({0}) = {1}")
  @CsvSource({
    "0.125, 0.1",
    "0.175, 0.2",
    "1.1, 1.2",
    "1.3, 1.2",
    "1.5, 1.6",
    "150000.0, 200000.0",
    "950000.0, 1000000.0",
  })
  void ofBreaksRoundingTiesTowardsTheEvenNeighbour(double input, double expected) {
    assertThat(Reluctance.of(input).value()).isWithin(EPSILON).of(expected);
  }

  @Test
  void ofNormalizesRawValuesToTheSameCacheKey() {
    // Two different raw inputs landing in the same step both quantize to 7.0 - the whole point
    // of this class is to let equals()/hashCode() treat them as the same cache key.
    assertThat(Reluctance.of(7.1)).isEqualTo(Reluctance.of(7.3));
    assertThat(Reluctance.of(7.1).hashCode()).isEqualTo(Reluctance.of(7.3).hashCode());
  }

  @Test
  void ofRejectsValuesOutsideZeroToOneMillion() {
    assertThrows(IllegalArgumentException.class, () -> Reluctance.of(-0.01));
    assertThrows(IllegalArgumentException.class, () -> Reluctance.of(1_000_000.01));
  }

  @Test
  void ofAcceptsTheInclusiveBoundaries() {
    assertThat(Reluctance.of(0.0).value()).isWithin(EPSILON).of(0.0);
    assertThat(Reluctance.of(1_000_000.0).value()).isWithin(EPSILON).of(1_000_000.0);
  }

  @Test
  void equalsAndHashCodeDistinguishDifferentValues() {
    assertThat(Reluctance.of(2.0)).isNotEqualTo(Reluctance.of(3.0));
    assertThat(Reluctance.of(2.0)).isNotEqualTo(null);
    assertThat(Reluctance.of(2.0)).isNotEqualTo("2.0Ω");
  }

  @Test
  void compareToOrdersByValue() {
    var low = Reluctance.of(2.0);
    var high = Reluctance.of(10.0);
    assertThat(low.compareTo(high)).isLessThan(0);
    assertThat(high.compareTo(low)).isGreaterThan(0);
    assertThat(low.compareTo(Reluctance.of(2.0))).isEqualTo(0);
  }

  @Test
  void toStringIncludesTheOhmSign() {
    // U+2126 OHM SIGN, not U+03A9 GREEK CAPITAL LETTER OMEGA - they look identical.
    assertThat(Reluctance.of(2.0).toString()).isEqualTo("2.0Ω");
  }

  @Test
  void normalizedDistance() {
    var min = Reluctance.of(0.0);
    var max = Reluctance.of(1_000_000.0);
    assertThat(min.normalizedDistance(min)).isWithin(EPSILON).of(0.0);
    assertThat(min.normalizedDistance(max)).isWithin(EPSILON).of(1.0);
    assertThat(max.normalizedDistance(min)).isWithin(EPSILON).of(1.0);
    assertThat(max.normalizedDistance(max)).isWithin(EPSILON).of(0.0);
    // A reluctance an order of magnitude higher is "further" than one twice as high.
    var double_ = Reluctance.of(2.0);
    var tenX = Reluctance.of(10.0);
    assertThat(min.normalizedDistance(tenX)).isGreaterThan(min.normalizedDistance(double_));
  }
}
