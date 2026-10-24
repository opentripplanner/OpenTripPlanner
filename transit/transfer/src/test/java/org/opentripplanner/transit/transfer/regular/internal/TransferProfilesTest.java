package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.transfer.regular.FakeRequestMapper;
import org.opentripplanner.transit.transfer.regular.FakeTransferPathProvider;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.WalkProfileParameters;

class TransferProfilesTest {

  @Test
  void ofWrapsTheGivenProfilesPreservingOrder() {
    var walk = new TransferProfile<>(
      new WalkProfileParameters<>(WalkPreferences.DEFAULT, WalkPreferences.DEFAULT),
      new FakeTransferPathProvider(),
      FakeRequestMapper.INSTANCE
    );

    var profiles = TransferProfiles.of(List.of(walk));

    assertThat(profiles).containsExactly(walk);
  }
}
