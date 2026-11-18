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

    assertThat(subject.normalizedDistance(subject)).isEqualTo(0.0);
    assertThat(subject.normalizedDistance(preferences(1.3, 2.0))).isEqualTo(0.0);
  }

  @Test
  void distanceIncreasesWithTheDifferenceInPreferences() {
    AbstractUserPreferences<?> subject = preferences(1.3, 2.0);
    AbstractUserPreferences<?> sameSpeed = preferences(1.3, 3.0);
    AbstractUserPreferences<?> sameReluctance = preferences(1.5, 2.0);
    AbstractUserPreferences<?> bothDiffer = preferences(1.5, 3.0);

    assertThat(subject.normalizedDistance(sameSpeed)).isGreaterThan(0.0);
    assertThat(subject.normalizedDistance(sameReluctance)).isGreaterThan(0.0);
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
    assertThat(subject.normalizedDistance(walk)).isGreaterThan(100.0);
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
    assertThat(subject.normalizedDistance(slowWalk)).isGreaterThan(0.0);
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
    assertThat(subject.normalizedDistance(fastest)).isGreaterThan(0.0);
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

  @Test
  void aStepOfTheReluctanceTheSpeedAndTheTriangleCountTheSame() {
    var base = BicyclePreferences.of()
      .withReluctance(Reluctance.of(2.0))
      .withSpeed(Speed.ofMetersPerSecond(5.0))
      .withOptimize(TimeSlopeSafetyTrianglePreference.of(0.4, 0.3, 0.3))
      .build();
    var avoidCycling = base.copyOf().withReluctance(Reluctance.of(2.5)).build();
    var faster = base.copyOf().withSpeed(Speed.ofMetersPerSecond(5.4)).build();
    // The sum of the changes of the time and the slope is 0.3, the distance is the sum divided by
    // three. The safety does not change, it counts more, see the triangle test.
    var otherTriangle = base
      .copyOf()
      .withOptimize(TimeSlopeSafetyTrianglePreference.of(0.25, 0.45, 0.3))
      .build();

    assertThat(base.normalizedDistance(avoidCycling)).isWithin(1e-9).of(0.1);
    assertThat(base.normalizedDistance(faster)).isWithin(1e-9).of(0.1);
    assertThat(base.normalizedDistance(otherTriangle)).isWithin(1e-9).of(0.1);
  }

  @Test
  void theSafetyOfTheTriangleCountsMoreInTheDistanceAndDependsOnTheDirection() {
    var safe = preferences(5.0, 2.0)
      .copyOf()
      .withOptimize(TimeSlopeSafetyTrianglePreference.of(0, 0, 1))
      .build();
    var fast = safe
      .copyOf()
      .withOptimize(TimeSlopeSafetyTrianglePreference.of(1, 0, 0))
      .build();

    // time 1 + safety 1 * weight, divided by three: the user wants more safety than the profile has
    assertThat(safe.normalizedDistance(fast))
      .isWithin(1e-9)
      .of((1.0 + 8.0) / 3.0);
    assertThat(fast.normalizedDistance(safe))
      .isWithin(1e-9)
      .of((1.0 + 3.0) / 3.0);
  }

  @Test
  void aStepOfTheWalkingWithTheBicycleCountsAThirdOfAStepOfTheBicycle() {
    var base = BicyclePreferences.of()
      .withWalk(BicycleWalk.of(Speed.ofMetersPerSecond(1.3), Reluctance.of(2.0)))
      .build();
    var fasterWalking = base
      .copyOf()
      .withWalk(BicycleWalk.of(Speed.ofMetersPerSecond(1.4), Reluctance.of(2.0)))
      .build();
    var avoidWalking = base
      .copyOf()
      .withWalk(BicycleWalk.of(Speed.ofMetersPerSecond(1.3), Reluctance.of(2.5)))
      .build();

    assertThat(base.normalizedDistance(fasterWalking))
      .isWithin(1e-9)
      .of(0.1 / 3);
    assertThat(base.normalizedDistance(avoidWalking))
      .isWithin(1e-9)
      .of(0.1 / 3);
  }
}
