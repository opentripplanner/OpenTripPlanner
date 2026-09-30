package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfile;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfiles;

class TransferProfilesTest {

  @Test
  void ofWrapsTheGivenProfilesPreservingOrder() {
    var walk = new TransferProfile<>(
      TransferProfileType.WALK,
      WalkPreferences.DEFAULT,
      WalkPreferences.DEFAULT
    );

    var profiles = TransferProfiles.of(List.of(walk));

    assertThat(profiles).containsExactly(walk);
  }
}
