package org.opentripplanner.transit.transfer.regular.api;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
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
}
