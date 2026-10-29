package org.opentripplanner.transit.transfer.regular.api.preferences;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;

class BicyclePreferencesTest {

  /**
   * The preferences are used as a cache key, so preferences built separately from equal values
   * must be equal - one is created for each request.
   */
  @Test
  void preferencesWithEqualValuesAreEqual() {
    var subject = preferences(1.3, 2.0);
    var same = preferences(1.3, 2.0);

    assertThat(same).isNotSameInstanceAs(subject);
    assertThat(same).isEqualTo(subject);
    assertThat(same.hashCode()).isEqualTo(subject.hashCode());
  }

  @Test
  void preferencesWithDifferentValuesAreNotEqual() {
    var subject = preferences(1.3, 2.0);

    assertThat(preferences(1.3, 3.0)).isNotEqualTo(subject);
    assertThat(preferences(1.5, 2.0)).isNotEqualTo(subject);
  }

  @Test
  void distanceToSelfIsZero() {
    var subject = preferences(1.3, 2.0);

    assertThat(subject.normalizedDistance(subject)).isEqualTo(0f);
    assertThat(subject.normalizedDistance(preferences(1.3, 2.0))).isEqualTo(0f);
  }

  @Test
  void distanceIncreasesWithTheDifferenceInPreferences() {
    AbstractUserPreferences<?> subject = preferences(1.3, 2.0);
    AbstractUserPreferences<?> sameSpeed = preferences(1.3, 3.0);
    AbstractUserPreferences<?> sameReluctance = preferences(1.5, 2.0);
    AbstractUserPreferences<?> bothDiffer = preferences(1.5, 3.0);

    assertThat(subject.normalizedDistance(sameSpeed)).isGreaterThan(0f);
    assertThat(subject.normalizedDistance(sameReluctance)).isGreaterThan(0f);
    assertThat(subject.normalizedDistance(bothDiffer)).isGreaterThan(
      subject.normalizedDistance(sameSpeed)
    );
  }

  @Test
  void distanceIsTheSameThroughTheBicyclePreferencesType() {
    var subject = preferences(1.3, 2.0);
    var other = preferences(1.5, 3.0);
    AbstractUserPreferences<?> abstractSubject = subject;

    assertThat(subject.normalizedDistance(other)).isEqualTo(
      abstractSubject.normalizedDistance(other)
    );
  }

  @Test
  void preferencesOfAnotherTypeAreNotEqualAndFarAway() {
    var subject = preferences(1.3, 2.0);
    var walk = WalkPreferences.of()
      .withSpeed(Speed.ofMetersPerSecond(1.3))
      .withReluctance(Reluctance.of(2.0))
      .build();

    assertThat(subject).isNotEqualTo(walk);
    assertThat(subject.type()).isEqualTo(TransferProfileType.BICYCLE);
    assertThat(subject.normalizedDistance(walk)).isGreaterThan(100f);
  }

  @Test
  void thePreferencesForWalkingWithTheBicycleAreAPartOfTheEquality() {
    var subject = preferences(1.3, 2.0);
    var slowWalk = subject
      .copyOf()
      .withWalk(BicycleWalk.of(Speed.ofMetersPerSecond(0.9), Reluctance.of(5.0)))
      .build();
    var sameWalk = subject.copyOf().withWalk(subject.walk()).build();

    assertThat(slowWalk).isNotEqualTo(subject);
    assertThat(sameWalk).isEqualTo(subject);
    assertThat(subject.normalizedDistance(slowWalk)).isGreaterThan(0f);
  }

  @Test
  void theDefaultWalkingWithTheBicycleIsTheDefaultOfTheRequest() {
    assertThat(BicyclePreferences.DEFAULT.walk()).isEqualTo(
      BicycleWalk.of(Speed.ofMetersPerSecond(1.35), Reluctance.of(5.0))
    );
  }

  @Test
  void theOptimizationIsAPartOfTheEqualityAndTheDistance() {
    var subject = preferences(1.3, 2.0);
    var fastest = subject
      .copyOf()
      .withOptimize(TimeSlopeSafetyTrianglePreference.of(1, 0, 0))
      .build();
    var same = subject.copyOf().withOptimize(subject.optimize()).build();

    assertThat(fastest).isNotEqualTo(subject);
    assertThat(same).isEqualTo(subject);
    assertThat(subject.normalizedDistance(fastest)).isGreaterThan(0f);
  }

  @Test
  void theDefaultOptimizationIsTheSafestStreets() {
    assertThat(BicyclePreferences.DEFAULT.optimize()).isEqualTo(
      TimeSlopeSafetyTrianglePreference.of(0.0, 0.0, 1.0)
    );
  }

  private static BicyclePreferences preferences(double speed, double reluctance) {
    return BicyclePreferences.of()
      .withSpeed(Speed.ofMetersPerSecond(speed))
      .withReluctance(Reluctance.of(reluctance))
      .build();
  }
}
