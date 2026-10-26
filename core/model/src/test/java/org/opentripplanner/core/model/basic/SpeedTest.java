package org.opentripplanner.core.model.basic;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SpeedTest {

  private static final float EPSILON = 0.001f;

  // The speed is rounded to a step that grows with the value. The tests below check the result
  // for the edges of the range and of the buckets, not how the steps are defined.

  @ParameterizedTest(name = "ofMetersPerSecond({0}) = {1}")
  @CsvSource({
    "0.1, 0.1",
    "0.12, 0.1",
    "0.13, 0.15",
    "300.0, 300.0",
    "299.0, 300.0",
    "297.0, 295.0",
  })
  void ofMetersPerSecondRoundsTheValuesAtTheEdgesOfTheRange(double input, double expected) {
    assertThat(Speed.ofMetersPerSecond(input).toMetersPerSecond()).isWithin(EPSILON).of(expected);
  }

  @ParameterizedTest(name = "ofMetersPerSecond({0}) = {1}")
  @CsvSource({ "2.0, 2.0", "5.0, 5.0", "10.0, 10.0", "20.0, 20.0", "50.0, 50.0", "100.0, 100.0" })
  void aValueAtTheLowerBoundOfABucketIsNotChanged(double input, double expected) {
    assertThat(Speed.ofMetersPerSecond(input).toMetersPerSecond()).isWithin(EPSILON).of(expected);
  }

  @ParameterizedTest(name = "ofMetersPerSecond({0}) = {1}")
  @CsvSource({ "1.99, 2.0", "4.99, 5.0", "9.95, 10.0", "19.9, 20.0", "49.8, 50.0", "99.5, 100.0" })
  void aValueJustBelowTheUpperBoundOfABucketIsRoundedUpToTheBound(double input, double expected) {
    assertThat(Speed.ofMetersPerSecond(input).toMetersPerSecond()).isWithin(EPSILON).of(expected);
  }

  @ParameterizedTest(name = "ofMetersPerSecond({0}) = {1}")
  @CsvSource({ "2.04, 2.0", "5.09, 5.0", "10.2, 10.0", "20.4, 20.0", "50.9, 50.0", "101.0, 100.0" })
  void aValueJustAboveTheLowerBoundOfABucketIsRoundedBackToTheBound(double input, double expected) {
    assertThat(Speed.ofMetersPerSecond(input).toMetersPerSecond()).isWithin(EPSILON).of(expected);
  }

  @ParameterizedTest(name = "ofMetersPerSecond({0}) = {1}")
  @CsvSource({
    "0.37, 0.35",
    "1.37, 1.35",
    "3.23, 3.2",
    "5.61, 5.6",
    "12.3, 12.5",
    "33.4, 33.0",
    "61.3, 62.0",
    "153.0, 155.0",
  })
  void aValueInsideABucketIsRoundedToTheNearestStep(double input, double expected) {
    assertThat(Speed.ofMetersPerSecond(input).toMetersPerSecond()).isWithin(EPSILON).of(expected);
  }

  @ParameterizedTest(name = "ofMetersPerSecond({0}) = {1}")
  @CsvSource({ "1.325, 1.3", "1.375, 1.4", "5.3, 5.2", "63.0, 64.0" })
  void ofMetersPerSecondBreaksRoundingTiesTowardsTheEvenNeighbour(double input, double expected) {
    assertThat(Speed.ofMetersPerSecond(input).toMetersPerSecond()).isWithin(EPSILON).of(expected);
  }

  @Test
  void theCommonWalkingSpeedsAreNotChanged() {
    for (double walk : new double[] { 0.9, 1.0, 1.1, 1.2, 1.3, 1.35, 1.4, 1.5 }) {
      assertThat(Speed.ofMetersPerSecond(walk).toMetersPerSecond()).isWithin(EPSILON).of(walk);
    }
  }

  @Test
  void ofMetersPerSecondNormalizesRawValuesToTheSameCacheKey() {
    // Two different raw inputs landing in the same step both quantize to 1.35 - the whole point
    // of this class is to let equals()/hashCode() treat them as the same cache key.
    assertThat(Speed.ofMetersPerSecond(1.36)).isEqualTo(Speed.ofMetersPerSecond(1.34));
    assertThat(Speed.ofMetersPerSecond(1.36).hashCode()).isEqualTo(
      Speed.ofMetersPerSecond(1.34).hashCode()
    );
  }

  @Test
  void ofMetersPerSecondRejectsValuesOutsideZeroPointOneToThreeHundred() {
    assertThrows(IllegalArgumentException.class, () -> Speed.ofMetersPerSecond(0.05));
    assertThrows(IllegalArgumentException.class, () -> Speed.ofMetersPerSecond(300.01));
  }

  @Test
  void ofMetersPerSecondAcceptsTheInclusiveBoundaries() {
    assertThat(Speed.ofMetersPerSecond(0.1).toMetersPerSecond()).isWithin(EPSILON).of(0.1);
    assertThat(Speed.ofMetersPerSecond(300.0).toMetersPerSecond()).isWithin(EPSILON).of(300.0);
  }

  @Test
  void equalsAndHashCodeDistinguishDifferentValues() {
    assertThat(Speed.ofMetersPerSecond(1.35)).isNotEqualTo(Speed.ofMetersPerSecond(5.0));
    assertThat(Speed.ofMetersPerSecond(1.35)).isNotEqualTo(null);
    assertThat(Speed.ofMetersPerSecond(1.35)).isNotEqualTo("1.35m/s");
  }

  @Test
  void compareToOrdersByValue() {
    var slow = Speed.ofMetersPerSecond(1.35);
    var fast = Speed.ofMetersPerSecond(30.0);
    assertThat(slow.compareTo(fast)).isLessThan(0);
    assertThat(fast.compareTo(slow)).isGreaterThan(0);
    assertThat(slow.compareTo(Speed.ofMetersPerSecond(1.35))).isEqualTo(0);
  }

  @Test
  void toStringIncludesTheUnit() {
    assertThat(Speed.ofMetersPerSecond(1.35).toString()).isEqualTo("1.35m/s");
  }

  @Test
  void normalizedDistance() {
    var min = Speed.ofMetersPerSecond(0.1f);
    var max = Speed.ofMetersPerSecond(300f);
    assertThat(min.normalizedDistance(min)).isWithin(EPSILON).of(0.0f);
    assertThat(min.normalizedDistance(max)).isWithin(EPSILON).of(1.0f);
    assertThat(max.normalizedDistance(min)).isWithin(EPSILON).of(1.0f);
    assertThat(max.normalizedDistance(max)).isWithin(EPSILON).of(0.0f);
    // The scale is log(v + offset), offset > 0 - doubling a speed near the low end (where the
    // fixed offset dominates) is a smaller jump on the log scale than doubling one near the high
    // end, where the ratio approaches the untranslated log(2).
    var walk = Speed.ofMetersPerSecond(1.35);
    var doubleWalk = Speed.ofMetersPerSecond(2.7);
    var bikeSpeed = Speed.ofMetersPerSecond(150.0);
    var doubleBikeSpeed = Speed.ofMetersPerSecond(300.0);
    assertThat(walk.normalizedDistance(doubleWalk)).isLessThan(
      bikeSpeed.normalizedDistance(doubleBikeSpeed)
    );
  }
}
