package org.opentripplanner.transit.transfer.regular.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;

class TransferProfilesParametersTest {

  private static final WalkPreferences WALK = WalkPreferences.DEFAULT;
  private static final WalkPreferences OTHER_WALK = WalkPreferences.of()
    .withReluctance(Reluctance.of(5.0))
    .build();

  @Test
  void keepsTheProfilesInTheDeclaredOrder() {
    var first = new WalkProfileParameters<>(OTHER_WALK, "first");
    var second = new WalkProfileParameters<>(WALK, "second");

    var subject = new TransferProfilesParameters<>(List.of(first, second));

    assertThat(subject.walk()).containsExactly(first, second).inOrder();
  }

  @Test
  void requiresAtLeastOneProfile() {
    assertThrows(IllegalArgumentException.class, () ->
      new TransferProfilesParameters<String>(List.of())
    );
  }

  @Test
  void rejectsProfilesWithEqualUserPreferences() {
    var profiles = List.of(
      new WalkProfileParameters<>(WALK, "first"),
      new WalkProfileParameters<>(WalkPreferences.of().build(), "second")
    );

    assertThrows(IllegalArgumentException.class, () -> new TransferProfilesParameters<>(profiles));
  }

  @Test
  void rejectsAProfileWithoutARoutingRequest() {
    assertThrows(NullPointerException.class, () -> new WalkProfileParameters<String>(WALK, null));
  }

  @Test
  void aTypeWithoutAnOverrideUsesTheDefaultMaxDurations() {
    var profiles = List.of(new WalkProfileParameters<>(WALK, "walk"));

    var subject = new TransferProfilesParameters<>(
      profiles,
      MaxDurationsPreference.of(Duration.ofMinutes(20)),
      Map.of()
    );

    assertThat(subject.maxDurations(TransferProfileType.WALK).maxDuration()).isEqualTo(
      Duration.ofMinutes(20)
    );
  }

  @Test
  void anOverrideReplacesTheDefaultMaxDurationsOfTheType() {
    var profiles = List.of(new WalkProfileParameters<>(WALK, "walk"));

    var subject = new TransferProfilesParameters<>(
      profiles,
      MaxDurationsPreference.of(Duration.ofMinutes(20)),
      Map.of(TransferProfileType.WALK, MaxDurationsPreference.of(Duration.ofMinutes(45)))
    );

    assertThat(subject.maxDurations(TransferProfileType.WALK).maxDuration()).isEqualTo(
      Duration.ofMinutes(45)
    );
  }

  @Test
  void theDefaultMaxDurationsAreThirtyMinutes() {
    var subject = new TransferProfilesParameters<>(List.of(new WalkProfileParameters<>(WALK, "w")));

    assertThat(subject.maxDurations(TransferProfileType.WALK)).isEqualTo(
      MaxDurationsPreference.DEFAULT
    );
    assertThat(MaxDurationsPreference.DEFAULT.maxDuration()).isEqualTo(Duration.ofMinutes(30));
  }
}
