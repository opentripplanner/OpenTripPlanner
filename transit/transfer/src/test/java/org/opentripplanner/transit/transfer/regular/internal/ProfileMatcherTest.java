package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;

class ProfileMatcherTest {

  // Reluctance 1.0 and 3.5 are the same distance from 2.0: (1 + 1) * (3.5 + 1) = (2 + 1) ^ 2
  private static final WalkPreferences REQUEST = walk(2.0);
  private static final WalkPreferences WARMUP_1 = walk(1.0);
  private static final WalkPreferences WARMUP_3_5 = walk(3.5);
  private static final WalkPreferences PROFILE_A = walk(8.0);
  private static final WalkPreferences PROFILE_B = walk(12.0);

  @Test
  void aProfileWinsATieAgainstAWarmupEntry() {
    var profile = walk(1.0);
    var subject = new ProfileMatcher(Map.of(WARMUP_3_5, profile), List.of(profile));

    assertThat(subject.closestProfile(overlaid(profile), REQUEST)).isEqualTo(profile);
  }

  @Test
  void theFirstDeclaredWarmupEntryWinsATie() {
    var paths = overlaid(PROFILE_A, PROFILE_B);

    assertThat(
      matcher(WARMUP_1, PROFILE_A, WARMUP_3_5, PROFILE_B).closestProfile(paths, REQUEST)
    ).isEqualTo(PROFILE_A);
    assertThat(
      matcher(WARMUP_3_5, PROFILE_B, WARMUP_1, PROFILE_A).closestProfile(paths, REQUEST)
    ).isEqualTo(PROFILE_B);
  }

  @Test
  void theFirstDeclaredProfileWinsATie() {
    var paths = overlaid(WARMUP_1, WARMUP_3_5);

    var first = new ProfileMatcher(Map.of(), List.of(WARMUP_1, WARMUP_3_5));
    var second = new ProfileMatcher(Map.of(), List.of(WARMUP_3_5, WARMUP_1));

    assertThat(first.closestProfile(paths, REQUEST)).isEqualTo(WARMUP_1);
    assertThat(second.closestProfile(paths, REQUEST)).isEqualTo(WARMUP_3_5);
  }

  /** A matcher with warmup entries in the given order, as pairs of entry and parent profile. */
  private static ProfileMatcher matcher(AbstractUserPreferences<?>... entryAndParent) {
    var warmup = new LinkedHashMap<AbstractUserPreferences<?>, AbstractUserPreferences<?>>();
    for (int i = 0; i < entryAndParent.length; i += 2) {
      warmup.put(entryAndParent[i], entryAndParent[i + 1]);
    }
    return new ProfileMatcher(warmup, List.of(PROFILE_A, PROFILE_B));
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
