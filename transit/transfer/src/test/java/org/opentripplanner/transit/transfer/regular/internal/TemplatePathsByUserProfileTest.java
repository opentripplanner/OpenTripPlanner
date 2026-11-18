package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
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

  // Reluctance 1.0 and 3.5 are the same distance from 2.0: (1 + 1) * (3.5 + 1) = (2 + 1) ^ 2
  private static final WalkPreferences WALK_1 = walk(1.0);
  private static final WalkPreferences WALK_3_5 = walk(3.5);

  @Test
  void aTieIsBrokenByTheGivenOrderWhateverTheInsertionOrder() {
    var request = walk(2.0);

    for (var insertion : List.of(List.of(WALK_1, WALK_3_5), List.of(WALK_3_5, WALK_1))) {
      var subject = new TemplatePathsByUserProfile<String>();
      insertion.forEach(p -> subject.setPaths(p, 1, Map.of(2, "path")));

      assertThat(subject.closestProfile(request, declared(WALK_1, WALK_3_5))).isEqualTo(WALK_1);
      assertThat(subject.closestProfile(request, declared(WALK_3_5, WALK_1))).isEqualTo(WALK_3_5);
    }
  }

  @Test
  void aCloserProfileWinsEvenIfItIsDeclaredLast() {
    var subject = new TemplatePathsByUserProfile<String>();
    var closer = walk(2.5);
    subject.setPaths(WALK_1, 1, Map.of(2, "path"));
    subject.setPaths(WALK_3_5, 1, Map.of(2, "path"));
    subject.setPaths(closer, 1, Map.of(2, "path"));

    assertThat(subject.closestProfile(walk(2.0), declared(WALK_1, WALK_3_5, closer))).isEqualTo(
      closer
    );
  }

  /** The profiles in the given order, the order of the declaration in the configuration. */
  static Comparator<AbstractUserPreferences<?>> declared(AbstractUserPreferences<?>... profiles) {
    var list = List.of(profiles);
    return Comparator.comparingInt(p -> list.indexOf(p) < 0 ? Integer.MAX_VALUE : list.indexOf(p));
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
