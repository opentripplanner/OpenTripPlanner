package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WheelchairPreferences;

class ProfileMatcherTest {

  private static final WalkPreferences WALK_2 = walk(2.0);
  private static final WalkPreferences WALK_8 = walk(8.0);
  private static final WheelchairPreferences WHEELCHAIR_4 = WheelchairPreferences.of()
    .withReluctance(Reluctance.of(4.0))
    .build();

  // Reluctance 1.0 and 3.5 are the same distance from 2.0: (1 + 1) * (3.5 + 1) = (2 + 1) ^ 2
  private static final WalkPreferences WALK_1 = walk(1.0);
  private static final WalkPreferences WALK_3_5 = walk(3.5);

  private final ProfileMatcher subject = new ProfileMatcher(List.of(WALK_2, WALK_8, WHEELCHAIR_4));

  @Test
  void noProfilesHasNoClosestProfile() {
    assertThat(new ProfileMatcher(List.of()).closestProfile(WALK_2)).isNull();
  }

  @Test
  void theClosestProfileIsThePreferencesThemselvesIfTheyAreAProfile() {
    assertThat(subject.closestProfile(walk(2.0))).isEqualTo(WALK_2);
  }

  @Test
  void theClosestProfileIsTheClosestOfTheSameType() {
    assertThat(subject.closestProfile(walk(3.0))).isEqualTo(WALK_2);
    assertThat(subject.closestProfile(walk(7.0))).isEqualTo(WALK_8);
  }

  @Test
  void theClosestProfileIsNeverOfAnotherType() {
    // The wheelchair profile is the only one with the same type, even though a walk profile has
    // the same reluctance
    var walkLikeWheelchair = WheelchairPreferences.of().withReluctance(Reluctance.of(2.0)).build();
    assertThat(subject.closestProfile(walkLikeWheelchair)).isEqualTo(WHEELCHAIR_4);
  }

  @Test
  void aTypeWithoutProfilesIsNotAccepted() {
    var walkOnly = new ProfileMatcher(List.of(WALK_2));

    var ex = assertThrows(IllegalArgumentException.class, () ->
      walkOnly.closestProfile(WHEELCHAIR_4)
    );
    assertThat(ex.getMessage()).contains("WHEELCHAIR");
  }

  @Test
  void theFirstDeclaredProfileWinsATie() {
    var request = walk(2.0);

    assertThat(new ProfileMatcher(List.of(WALK_1, WALK_3_5)).closestProfile(request)).isEqualTo(
      WALK_1
    );
    assertThat(new ProfileMatcher(List.of(WALK_3_5, WALK_1)).closestProfile(request)).isEqualTo(
      WALK_3_5
    );
  }

  @Test
  void aCloserProfileWinsEvenIfItIsDeclaredLast() {
    var closer = walk(2.5);
    var matcher = new ProfileMatcher(List.of(WALK_1, WALK_3_5, closer));

    assertThat(matcher.closestProfile(walk(2.0))).isEqualTo(closer);
  }

  private static WalkPreferences walk(double reluctance) {
    return WalkPreferences.of().withReluctance(Reluctance.of(reluctance)).build();
  }
}
