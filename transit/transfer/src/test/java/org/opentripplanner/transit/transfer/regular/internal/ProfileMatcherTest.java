package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;

class ProfileMatcherTest {

  // Reluctance 1.0 and 3.5 are the same distance from 2.0: (1 + 1) * (3.5 + 1) = (2 + 1) ^ 2
  private static final WalkPreferences REQUEST = walk(2.0);
  private static final WalkPreferences PROFILE_1 = walk(1.0);
  private static final WalkPreferences PROFILE_3_5 = walk(3.5);
  private static final WalkPreferences PROFILE_8 = walk(8.0);

  @Test
  void theClosestProfileIsUsedForAnyPreferences() {
    var subject = new ProfileMatcher(List.of(PROFILE_1, PROFILE_8));

    // The request (2.0) is closer to the profile 1.0 than to 8.0
    assertThat(subject.closestProfile(overlaid(PROFILE_1, PROFILE_8), REQUEST)).isEqualTo(
      PROFILE_1
    );
    assertThat(subject.closestProfile(overlaid(PROFILE_1, PROFILE_8), walk(7.0))).isEqualTo(
      PROFILE_8
    );
  }

  @Test
  void theFirstDeclaredProfileWinsATie() {
    var paths = overlaid(PROFILE_1, PROFILE_3_5);

    var first = new ProfileMatcher(List.of(PROFILE_1, PROFILE_3_5));
    var second = new ProfileMatcher(List.of(PROFILE_3_5, PROFILE_1));

    assertThat(first.closestProfile(paths, REQUEST)).isEqualTo(PROFILE_1);
    assertThat(second.closestProfile(paths, REQUEST)).isEqualTo(PROFILE_3_5);
  }

  private static OverlaidTemplatePaths<String> overlaid(AbstractUserPreferences<?>... profiles) {
    var paths = new TemplatePathsByUserProfile<String>();
    for (var profile : profiles) {
      paths.setPaths(profile, 1, Map.of(2, "path"));
    }
    return new OverlaidTemplatePaths<>(paths);
  }

  private static WalkPreferences walk(double reluctance) {
    return WalkPreferences.of().withReluctance(Reluctance.of(reluctance)).build();
  }
}
