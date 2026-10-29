package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.preferences.BicyclePreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.CarPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WheelchairPreferences;

class ProfileMatcherTest {

  private static final WalkPreferences WALK = WalkPreferences.of()
    .withReluctance(Reluctance.of(2.0))
    .build();

  private final TemplatePathsByUserProfile<String> paths = pathsOfWalk();
  private final OverlaidTemplatePaths<String> overlaid = new OverlaidTemplatePaths<>(paths);

  @Test
  void aTypeThatMayFallBackUsesTheClosestProfileOfAnotherType() {
    var subject = new ProfileMatcher(
      Map.of(),
      Set.of(TransferProfileType.WHEELCHAIR, TransferProfileType.BICYCLE)
    );

    assertThat(subject.closestProfile(overlaid, WheelchairPreferences.DEFAULT)).isEqualTo(WALK);
    assertThat(subject.closestProfile(overlaid, BicyclePreferences.DEFAULT)).isEqualTo(WALK);
  }

  @Test
  void aTypeThatMayNotFallBackFails() {
    var subject = new ProfileMatcher(
      Map.of(),
      Set.of(TransferProfileType.WHEELCHAIR, TransferProfileType.BICYCLE)
    );

    assertThrows(IllegalArgumentException.class, () ->
      subject.closestProfile(overlaid, CarPreferences.DEFAULT)
    );
  }

  @Test
  void withoutTheFallbackATypeWithoutProfilesFails() {
    assertThrows(IllegalArgumentException.class, () ->
      ProfileMatcher.NO_WARMUP.closestProfile(overlaid, WheelchairPreferences.DEFAULT)
    );
  }

  @Test
  void aTypeWithProfilesNeverFallsBack() {
    var subject = new ProfileMatcher(Map.of(), Set.of(TransferProfileType.WALK));

    var other = WalkPreferences.of().withReluctance(Reluctance.of(3.0)).build();
    assertThat(subject.closestProfile(overlaid, other)).isEqualTo(WALK);
  }

  private static TemplatePathsByUserProfile<String> pathsOfWalk() {
    var paths = new TemplatePathsByUserProfile<String>();
    paths.setPaths(WALK, 1, Map.of(2, "walk"));
    return paths;
  }
}
