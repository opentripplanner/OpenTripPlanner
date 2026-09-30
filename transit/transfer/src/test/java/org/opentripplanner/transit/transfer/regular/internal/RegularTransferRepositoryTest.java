package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.transfer.regular.parameters.TransferProfileType;

class RegularTransferRepositoryTest {

  @Test
  void hasTransfersFromIsFalseForAStopWithNoStoredPaths() {
    var repository = new RegularTransferRepository<String>();
    assertThat(repository.hasTransfersFrom(0)).isFalse();
  }

  @Test
  void hasTransfersFromIsTrueOnceAPathIsStoredForAnyProfile() {
    var repository = new RegularTransferRepository<String>();
    repository.setPath(TransferProfileType.WALK, 0, 1, "A -> B");

    assertThat(repository.hasTransfersFrom(0)).isTrue();
    // Only the fromStop that was actually stored to is linked.
    assertThat(repository.hasTransfersFrom(1)).isFalse();
  }
}
