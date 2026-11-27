package org.opentripplanner.transit.transfer.regular.configure;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;

class RegularTransferFactoryTest {

  private static final WalkPreferences FIRST = WalkPreferences.DEFAULT;
  private static final WalkPreferences SECOND = WalkPreferences.of()
    .withReluctance(Reluctance.of(4.0))
    .build();

  @Test
  void theConfiguredProfilesMayBeTheGeneratedOnes() {
    assertDoesNotThrow(() ->
      RegularTransferFactory.assertGeneratedProfiles(Set.of(FIRST, SECOND), Set.of(SECOND, FIRST))
    );
  }

  @Test
  void anyProfilesMayBeConfiguredIfNoPathsWereGenerated() {
    assertDoesNotThrow(() -> RegularTransferFactory.assertGeneratedProfiles(null, Set.of(FIRST)));
  }

  @Test
  void otherConfiguredProfilesAreRejected() {
    var e = assertThrows(IllegalStateException.class, () ->
      RegularTransferFactory.assertGeneratedProfiles(Set.of(FIRST), Set.of(SECOND))
    );

    assertThat(e.getMessage()).contains("Profiles without paths in the graph: [" + SECOND + "]");
    assertThat(e.getMessage()).contains("Profiles of the graph that are not configured: [" + FIRST);
  }

  @Test
  void aMissingProfileIsRejected() {
    assertThrows(IllegalStateException.class, () ->
      RegularTransferFactory.assertGeneratedProfiles(Set.of(FIRST, SECOND), Set.of(FIRST))
    );
  }
}
