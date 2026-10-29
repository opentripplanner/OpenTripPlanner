package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

class ProfileStatisticsTest {

  @Test
  void aProfileWithoutAParentHasOnlyItsOwnStreetPaths() {
    var subject = new ProfileStatistics("walk", "WalkPreferences{}", null, 18963, 0);

    assertThat(subject.streetPaths()).isEqualTo(18963);
    assertThat(subject.message()).isEqualTo("Profile 'walk' paths: 18 963 - WalkPreferences{}.");
  }

  @Test
  void aProfileWithAParentHasTheNumberAndThePercentageOfTheSharedPaths() {
    var subject = new ProfileStatistics("wheelchair", "WheelchairPreferences{}", "walk", 90, 52);

    assertThat(subject.streetPaths()).isEqualTo(38);
    assertThat(subject.message()).isEqualTo(
      "Profile 'wheelchair' paths: 90, 52 (58%) reused 'walk' - WheelchairPreferences{}."
    );
  }

  @Test
  void aProfileWithoutTransfersHasNoPercentage() {
    var subject = new ProfileStatistics("child", "p", "parent", 0, 0);

    assertThat(subject.message()).contains("paths: 0, 0 (0%) reused 'parent'");
  }

  @Test
  void numbersAreGroupedWithASpace() {
    assertThat(ProfileStatistics.format(999)).isEqualTo("999");
    assertThat(ProfileStatistics.format(1234567)).isEqualTo("1 234 567");
  }
}
