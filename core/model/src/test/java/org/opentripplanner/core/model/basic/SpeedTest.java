package org.opentripplanner.core.model.basic;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SpeedTest {

  private static final float EPSILON = 0.001f;

  /**
   * One representative value per step-size bucket: [0.1, 2)->0.05, [2, 30)->0.1, [30, 300]->1.
   * <p>
   * Each value is already an exact multiple of its bucket's step, so
   * {@code ofMetersPerSecond(value).toMetersPerSecond()} is stable (no actual rounding needed) -
   * this pins down which step size applies in each bucket without also depending on the rounding
   * tie-breaking rule (covered separately below).
   */
  @ParameterizedTest
  @CsvSource({ "0.1, 0.1", "1.35, 1.35", "5.0, 5.0", "25.0, 25.0", "30.0, 30.0", "300.0, 300.0" })
  void ofMetersPerSecondRoundsToTheExpectedStepPerBucket(double input, double expected) {
    assertThat(Speed.ofMetersPerSecond(input).toMetersPerSecond()).isWithin(EPSILON).of(expected);
  }

  @Test
  void ofMetersPerSecondActuallyRoundsWithinABucket() {
    // step 0.05 in [0.1, 2)
    assertThat(Speed.ofMetersPerSecond(1.37).toMetersPerSecond()).isWithin(EPSILON).of(1.35);
    // step 0.1 in [2, 30)
    assertThat(Speed.ofMetersPerSecond(5.23).toMetersPerSecond()).isWithin(EPSILON).of(5.2);
    // step 1 in [30, 300]
    assertThat(Speed.ofMetersPerSecond(50.4).toMetersPerSecond()).isWithin(EPSILON).of(50.0);
  }

  @Test
  void ofMetersPerSecondBreaksRoundingTiesTowardsTheEvenNeighbour() {
    // 1.325 / 0.05 = 26.5, a tie - rounds to the even neighbour, 26, not 27.
    assertThat(Speed.ofMetersPerSecond(1.325).toMetersPerSecond()).isWithin(EPSILON).of(1.3);
    // 1.375 / 0.05 = 27.5, a tie - rounds to the even neighbour, 28, not 27.
    assertThat(Speed.ofMetersPerSecond(1.375).toMetersPerSecond()).isWithin(EPSILON).of(1.4);
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
