package org.opentripplanner.transit.transfer.regular.api.preferences;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;

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
  void distanceIsTheSameThroughTheWalkPreferencesType() {
    var subject = preferences(1.3, 2.0);
    var other = preferences(1.5, 3.0);
    AbstractUserPreferences<?> abstractSubject = subject;

    assertThat(subject.normalizedDistance(other)).isEqualTo(
      abstractSubject.normalizedDistance(other)
    );
  }

  @Test
  void inheritedFromAParentOfAnotherTypeTakesTheValuesTheyHaveInCommon() {
    var wheelchair = WheelchairPreferences.of()
      .withSpeed(Speed.ofMetersPerSecond(0.9))
      .withReluctance(Reluctance.of(4.0))
      .build();

    var subject = WalkPreferences.inheritedFrom(wheelchair);

    assertThat(subject.speed()).isEqualTo(wheelchair.speed());
    assertThat(subject.reluctance()).isEqualTo(wheelchair.reluctance());
  }

  private static WalkPreferences preferences(double speed, double reluctance) {
    return WalkPreferences.of()
      .withSpeed(Speed.ofMetersPerSecond(speed))
      .withReluctance(Reluctance.of(reluctance))
      .build();
  }
}
