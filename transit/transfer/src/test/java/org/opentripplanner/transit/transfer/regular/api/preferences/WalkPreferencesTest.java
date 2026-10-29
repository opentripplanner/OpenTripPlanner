package org.opentripplanner.transit.transfer.regular.api.preferences;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Ratio;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;

class WalkPreferencesTest {

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
  void distanceIsTheSameThroughTheWalkPreferencesType() {
    var subject = preferences(1.3, 2.0);
    var other = preferences(1.5, 3.0);
    AbstractUserPreferences<?> abstractSubject = subject;

    assertThat(subject.normalizedDistance(other)).isEqualTo(
      abstractSubject.normalizedDistance(other)
    );
  }

  @Test
  void theSafetyFactorIsAPartOfTheEqualityAndTheDistance() {
    var subject = preferences(1.3, 2.0);
    var lessSafe = subject.copyOf().withSafetyFactor(Ratio.of(0.5).normalize()).build();
    var same = subject.copyOf().withSafetyFactor(subject.safetyFactor()).build();

    assertThat(lessSafe).isNotEqualTo(subject);
    assertThat(same).isEqualTo(subject);
    assertThat(subject.normalizedDistance(lessSafe)).isGreaterThan(0.0);
  }

  @Test
  void theDefaultSafetyFactorIsOne() {
    assertThat(WalkPreferences.DEFAULT.safetyFactor().asDouble()).isEqualTo(1.0);
  }

  private static WalkPreferences preferences(double speed, double reluctance) {
    return WalkPreferences.of()
      .withSpeed(Speed.ofMetersPerSecond(speed))
      .withReluctance(Reluctance.of(reluctance))
      .build();
  }

  @Test
  void theDistanceToAnotherTypeIsTheDistanceBetweenTheTypes() {
    assertThat(WalkPreferences.DEFAULT.normalizedDistance(CarPreferences.DEFAULT)).isEqualTo(
      TransferProfileType.WALK.normalizedDistance(TransferProfileType.CAR)
    );
  }

  /** The distance of a step that counts the same for the speed, the reluctance and the safety. */
  private static final double STEP = 0.1;

  @Test
  void aStepOfTheSpeedTheReluctanceAndTheSafetyFactorCountTheSame() {
    var base = WalkPreferences.of()
      .withSpeed(Speed.ofMetersPerSecond(1.3))
      .withReluctance(Reluctance.of(2.0))
      .withSafetyFactor(Ratio.of(0.5).normalize())
      .build();
    var faster = base.copyOf().withSpeed(Speed.ofMetersPerSecond(1.4)).build();
    var avoidWalking = base.copyOf().withReluctance(Reluctance.of(2.5)).build();
    var safer = base.copyOf().withSafetyFactor(Ratio.of(0.6).normalize()).build();

    assertThat(base.normalizedDistance(faster)).isWithin(1e-9).of(STEP);
    assertThat(base.normalizedDistance(avoidWalking)).isWithin(1e-9).of(STEP);
    assertThat(base.normalizedDistance(safer)).isWithin(1e-9).of(STEP);
  }
}
