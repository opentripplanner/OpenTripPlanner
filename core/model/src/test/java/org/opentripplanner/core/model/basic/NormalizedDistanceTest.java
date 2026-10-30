package org.opentripplanner.core.model.basic;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

class NormalizedDistanceTest {

  private static final double EPSILON = 0.001;

  private enum ThreeValues {
    LOW,
    MID,
    HIGH,
  }

  private enum TwoValues {
    A,
    B,
  }

  @Test
  void calculateNormalizedDistanceIsZeroForTheSameValue() {
    assertThat(NormalizedDistance.calculateNormalizedDistance(ThreeValues.MID, ThreeValues.MID))
      .isWithin(EPSILON)
      .of(0.0);
  }

  @Test
  void calculateNormalizedDistanceIsOneForTheOppositeEnds() {
    assertThat(NormalizedDistance.calculateNormalizedDistance(ThreeValues.LOW, ThreeValues.HIGH))
      .isWithin(EPSILON)
      .of(1.0);
    assertThat(NormalizedDistance.calculateNormalizedDistance(ThreeValues.HIGH, ThreeValues.LOW))
      .isWithin(EPSILON)
      .of(1.0);
  }

  @Test
  void calculateNormalizedDistanceIsProportionalToOrdinalDistance() {
    // LOW <-> MID is one step out of two possible steps (LOW..HIGH) - half the max distance.
    assertThat(NormalizedDistance.calculateNormalizedDistance(ThreeValues.LOW, ThreeValues.MID))
      .isWithin(EPSILON)
      .of(0.5);
    assertThat(NormalizedDistance.calculateNormalizedDistance(ThreeValues.MID, ThreeValues.HIGH))
      .isWithin(EPSILON)
      .of(0.5);
  }

  @Test
  void calculateNormalizedDistanceIsSymmetric() {
    assertThat(
      NormalizedDistance.calculateNormalizedDistance(ThreeValues.LOW, ThreeValues.MID)
    ).isEqualTo(NormalizedDistance.calculateNormalizedDistance(ThreeValues.MID, ThreeValues.LOW));
  }

  @Test
  void calculateNormalizedDistanceWorksForATwoValueEnum() {
    assertThat(NormalizedDistance.calculateNormalizedDistance(TwoValues.A, TwoValues.A))
      .isWithin(EPSILON)
      .of(0.0);
    assertThat(NormalizedDistance.calculateNormalizedDistance(TwoValues.A, TwoValues.B))
      .isWithin(EPSILON)
      .of(1.0);
  }
}
