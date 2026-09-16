package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.transfer.regular.RaptorTransferProfile;
import org.opentripplanner.transit.transfer.regular.spi.RegularTransferParameters;

class RegularTransferProfilesTest {

  private static RegularTransferParameters<String> profile(
    RaptorTransferProfile profileId,
    RaptorTransferProfile deduplicationProfile
  ) {
    return new RegularTransferParameters<>(profileId, deduplicationProfile, profileId.name());
  }

  @Test
  void deduplicationProfilesComeBeforeDependents() {
    var walk = profile(RaptorTransferProfile.WALK, null);
    var wheelchair = profile(RaptorTransferProfile.WHEELCHAIR, RaptorTransferProfile.WALK);
    var bicycle = profile(RaptorTransferProfile.BICYCLE, RaptorTransferProfile.WALK);
    var scooter = profile(RaptorTransferProfile.SCOOTER, RaptorTransferProfile.BICYCLE);

    // Deliberately out of order.
    var ordered = RegularTransferProfiles.of(
      List.of(scooter, bicycle, wheelchair, walk)
    ).orderedProfiles();

    var orderedProfileIds = ordered.stream().map(RegularTransferParameters::profileId).toList();
    assertThat(orderedProfileIds.indexOf(RaptorTransferProfile.WALK)).isLessThan(
      orderedProfileIds.indexOf(RaptorTransferProfile.WHEELCHAIR)
    );
    assertThat(orderedProfileIds.indexOf(RaptorTransferProfile.WALK)).isLessThan(
      orderedProfileIds.indexOf(RaptorTransferProfile.BICYCLE)
    );
    assertThat(orderedProfileIds.indexOf(RaptorTransferProfile.BICYCLE)).isLessThan(
      orderedProfileIds.indexOf(RaptorTransferProfile.SCOOTER)
    );
    assertThat(orderedProfileIds).containsExactlyElementsIn(
      List.of(
        RaptorTransferProfile.WALK,
        RaptorTransferProfile.WHEELCHAIR,
        RaptorTransferProfile.BICYCLE,
        RaptorTransferProfile.SCOOTER
      )
    );
  }

  @Test
  void missingDeduplicationProfileIsRejected() {
    var bicycle = profile(RaptorTransferProfile.BICYCLE, RaptorTransferProfile.WALK);
    assertThrows(IllegalArgumentException.class, () ->
      RegularTransferProfiles.of(List.of(bicycle))
    );
  }

  @Test
  void duplicateProfileIsRejected() {
    var walk1 = profile(RaptorTransferProfile.WALK, null);
    var walk2 = profile(RaptorTransferProfile.WALK, null);
    assertThrows(IllegalArgumentException.class, () ->
      RegularTransferProfiles.of(List.of(walk1, walk2))
    );
  }

  @Test
  void circularDeduplicationProfileIsRejected() {
    var a = profile(RaptorTransferProfile.BICYCLE, RaptorTransferProfile.SCOOTER);
    var b = profile(RaptorTransferProfile.SCOOTER, RaptorTransferProfile.BICYCLE);
    assertThrows(IllegalArgumentException.class, () -> RegularTransferProfiles.of(List.of(a, b)));
  }
}
