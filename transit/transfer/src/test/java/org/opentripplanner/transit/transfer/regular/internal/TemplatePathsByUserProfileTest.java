package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.transit.transfer.regular.api.preferences.CarPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WheelchairPreferences;

class TemplatePathsByUserProfileTest {

  private static final WalkPreferences WALK_2 = walk(2.0);
  private static final WalkPreferences WALK_8 = walk(8.0);
  private static final WheelchairPreferences WHEELCHAIR_4 = WheelchairPreferences.of()
    .withReluctance(Reluctance.of(4.0))
    .build();

  @Test
  void noPathsHasNoClosestProfile() {
    assertThat(new TemplatePathsByUserProfile<String>().closestProfile(WALK_2)).isNull();
  }

  @Test
  void theClosestProfileIsTheProfileItselfIfItHasPaths() {
    var subject = subject();

    assertThat(subject.closestProfile(walk(2.0))).isEqualTo(WALK_2);
  }

  @Test
  void theClosestProfileIsTheClosestOfTheSameType() {
    var subject = subject();

    assertThat(subject.closestProfile(walk(3.0))).isEqualTo(WALK_2);
    assertThat(subject.closestProfile(walk(7.0))).isEqualTo(WALK_8);
  }

  @Test
  void theClosestProfileIsNeverOfAnotherType() {
    var subject = subject();

    // The wheelchair profile is the only one with the same type, even though a walk profile has
    // the same reluctance
    var walkLikeWheelchair = WheelchairPreferences.of().withReluctance(Reluctance.of(2.0)).build();
    assertThat(subject.closestProfile(walkLikeWheelchair)).isEqualTo(WHEELCHAIR_4);
  }

  @Test
  void aTypeWithoutProfilesIsNotAccepted() {
    var subject = new TemplatePathsByUserProfile<String>();
    subject.setPaths(WALK_2, 1, Map.of(2, "path"));

    var ex = assertThrows(IllegalArgumentException.class, () ->
      subject.closestProfile(WHEELCHAIR_4)
    );
    assertThat(ex.getMessage()).contains("WHEELCHAIR");
  }

  @Test
  void aTypeWithoutProfilesFallsBackToTheClosestTypeIfAllowed() {
    var car = CarPreferences.DEFAULT;
    var subject = new TemplatePathsByUserProfile<String>();
    subject.setPaths(WALK_2, 1, Map.of(2, "walk 2"));
    subject.setPaths(car, 1, Map.of(2, "car"));

    // The walk is closer to the wheelchair than the car
    assertThat(subject.closestProfile(WHEELCHAIR_4, true)).isEqualTo(WALK_2);
  }

  @Test
  void theFallbackToAnotherTypeIsOffUnlessAllowed() {
    var subject = new TemplatePathsByUserProfile<String>();
    subject.setPaths(WALK_2, 1, Map.of(2, "walk 2"));

    assertThrows(IllegalArgumentException.class, () -> subject.closestProfile(WHEELCHAIR_4, false));
  }

  @Test
  void aProfileOfTheSameTypeIsPreferredEvenIfTheFallbackIsAllowed() {
    var subject = subject();

    assertThat(subject.closestProfile(walk(3.0), true)).isEqualTo(WALK_2);
  }

  private static TemplatePathsByUserProfile<String> subject() {
    var subject = new TemplatePathsByUserProfile<String>();
    subject.setPaths(WALK_2, 1, Map.of(2, "walk 2"));
    subject.setPaths(WALK_8, 1, Map.of(2, "walk 8"));
    subject.setPaths(WHEELCHAIR_4, 1, Map.of(2, "wheelchair 4"));
    return subject;
  }

  private static WalkPreferences walk(double reluctance) {
    return WalkPreferences.of().withReluctance(Reluctance.of(reluctance)).build();
  }
}
