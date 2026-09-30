package org.opentripplanner.core.model.basic;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ReluctanceTest {

  private static final float EPSILON = 0.001f;

  /**
   * One representative value per step-size bucket in the SCALE table: [1, 2)->0.1, [2, 5)->0.5,
   * [5, 10)->1.0, [10, 20)->2.0, [20, 50)->5.0, [50, 100)->10.0, [100, 200)->20.0,
   * [200, 500)->50.0, [500, 1e3)->100.0, [1e3, 2e3)->200.0, [2e3, 5e3)->500.0,
   * [5e3, 1e4)->1e3, [1e4, 1e5)->1e3, [1e5, 1e6)->1e5, [1e6, +inf)->1e5 (MAX_VALUE / 10).
   * <p>
   * Each value is already an exact multiple of its bucket's step, so {@code of(value).value()}
   * is stable (no actual rounding needed) - this pins down which step size applies in each
   * bucket without also depending on the rounding tie-breaking rule (covered separately below).
   */
  @ParameterizedTest
  @CsvSource({
    "1.0, 1.0",
    "1.5, 1.5",
    "3.5, 3.5",
    "7.0, 7.0",
    "14.0, 14.0",
    "30.0, 30.0",
    "70.0, 70.0",
    "140.0, 140.0",
    "300.0, 300.0",
    "700.0, 700.0",
    "1400.0, 1400.0",
    "3000.0, 3000.0",
    "7000.0, 7000.0",
    "70000.0, 70000.0",
    "700000.0, 700000.0",
    "1000000.0, 1000000.0",
  })
  void ofRoundsToTheExpectedStepPerBucket(double input, double expected) {
    assertThat(Reluctance.of(input).value()).isWithin(EPSILON).of(expected);
  }

  @Test
  void ofActuallyRoundsWithinABucket() {
    // step 0.1 in [1, 2)
    assertThat(Reluctance.of(1.37).value()).isWithin(EPSILON).of(1.4);
    // step 1.0 in [5, 10)
    assertThat(Reluctance.of(7.3).value()).isWithin(EPSILON).of(7.0);
    // step 10.0 in [50, 100)
    assertThat(Reluctance.of(73.0).value()).isWithin(EPSILON).of(70.0);
  }

  @Test
  void ofBreaksRoundingTiesTowardsTheEvenNeighbour() {
    // 1.05 / 0.1 = 10.5, a tie - rounds to the even neighbour, 10, not 11.
    assertThat(Reluctance.of(1.05).value()).isWithin(EPSILON).of(1.0);
    // 1.15 / 0.1 = 11.5, a tie - rounds to the even neighbour, 12, not 11.
    assertThat(Reluctance.of(1.15).value()).isWithin(EPSILON).of(1.2);
  }

  @Test
  void ofNormalizesRawValuesToTheSameCacheKey() {
    // Two different raw inputs landing in the same step both quantize to 7.0 - the whole point
    // of this class is to let equals()/hashCode() treat them as the same cache key.
    assertThat(Reluctance.of(7.1)).isEqualTo(Reluctance.of(7.3));
    assertThat(Reluctance.of(7.1).hashCode()).isEqualTo(Reluctance.of(7.3).hashCode());
  }

  @Test
  void ofRejectsValuesOutsideOneToOneMillion() {
    assertThrows(IllegalArgumentException.class, () -> Reluctance.of(0.99));
    assertThrows(IllegalArgumentException.class, () -> Reluctance.of(1_000_000.01));
  }

  @Test
  void ofAcceptsTheInclusiveBoundaries() {
    assertThat(Reluctance.of(1.0).value()).isWithin(EPSILON).of(1.0);
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
    var min = Reluctance.of(1.0);
    var max = Reluctance.of(1_000_000.0);
    assertThat(min.normalizedDistance(min)).isWithin(EPSILON).of(0.0f);
    assertThat(min.normalizedDistance(max)).isWithin(EPSILON).of(1.0f);
    assertThat(max.normalizedDistance(min)).isWithin(EPSILON).of(1.0f);
    assertThat(max.normalizedDistance(max)).isWithin(EPSILON).of(0.0f);
    // A reluctance an order of magnitude higher is "further" than one twice as high.
    var double_ = Reluctance.of(2.0);
    var tenX = Reluctance.of(10.0);
    assertThat(min.normalizedDistance(tenX)).isGreaterThan(min.normalizedDistance(double_));
  }
}
