package org.opentripplanner.transit.transfer.regular.api.preferences;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;

class CarPreferencesTest {

  /**
   * The preferences are used as a cache key, so preferences built separately from equal values
   * must be equal - one is created for each request.
   */
  @Test
  void preferencesWithEqualValuesAreEqual() {
    var subject = preferences(3.0);
    var same = preferences(3.0);

    assertThat(same).isNotSameInstanceAs(subject);
    assertThat(same).isEqualTo(subject);
    assertThat(same.hashCode()).isEqualTo(subject.hashCode());
  }

  @Test
  void preferencesWithDifferentValuesAreNotEqual() {
    assertThat(preferences(3.0)).isNotEqualTo(preferences(2.0));
  }

  @Test
  void theTypeIsCar() {
    assertThat(CarPreferences.DEFAULT.type()).isEqualTo(TransferProfileType.CAR);
  }

  @Test
  void distanceIncreasesWithTheDifferenceInReluctance() {
    var subject = preferences(2.0);

    assertThat(subject.normalizedDistance(preferences(2.0))).isEqualTo(0.0);
    assertThat(subject.normalizedDistance(preferences(3.0))).isGreaterThan(0.0);
    assertThat(subject.normalizedDistance(preferences(6.0))).isGreaterThan(
      subject.normalizedDistance(preferences(3.0))
    );
  }

  @Test
  void preferencesOfAnotherTypeAreNotEqualAndFarAway() {
    var subject = preferences(2.0);
    var bicycle = BicyclePreferences.of().withReluctance(Reluctance.of(2.0)).build();

    assertThat(subject).isNotEqualTo(bicycle);
    assertThat(subject.normalizedDistance(bicycle)).isGreaterThan(100.0);
  }

  private static CarPreferences preferences(double reluctance) {
    return CarPreferences.of().withReluctance(Reluctance.of(reluctance)).build();
  }
}
