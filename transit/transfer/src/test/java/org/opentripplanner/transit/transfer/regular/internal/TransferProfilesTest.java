package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.transit.transfer.regular.FakeRequestMapper;
import org.opentripplanner.transit.transfer.regular.FakeTransferPathProvider;
import org.opentripplanner.transit.transfer.regular.api.MaxDurationsPreference;
import org.opentripplanner.transit.transfer.regular.api.TransferProfilesParameters;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.WalkProfileParameters;

class TransferProfilesTest {

  @Test
  void ofWrapsTheGivenProfilesPreservingOrder() {
    var walk = new TransferProfile<>(
      new WalkProfileParameters<>("walk", WalkPreferences.DEFAULT, WalkPreferences.DEFAULT),
      MaxDurationsPreference.DEFAULT,
      new FakeTransferPathProvider(),
      FakeRequestMapper.INSTANCE
    );

    var profiles = TransferProfiles.of(List.of(walk));

    assertThat(profiles).containsExactly(walk);
  }

  @Test
  void aProfileWithoutUserPreferencesGetsThemFromItsRoutingRequest() {
    var request = WalkPreferences.of().withReluctance(Reluctance.of(7.0)).build();
    var parameters = new TransferProfilesParameters<>(
      List.of(new WalkProfileParameters<>("derived", null, request))
    );

    var profiles = TransferProfiles.of(
      parameters,
      new FakeTransferPathProvider(),
      FakeRequestMapper.INSTANCE
    );

    var profile = profiles.iterator().next();
    assertThat(profile.id()).isEqualTo("derived");
    assertThat(profile.userPreferences()).isEqualTo(request);
  }

  @Test
  void profilesWithEqualUserPreferencesAreRejectedAndNamed() {
    var parameters = new TransferProfilesParameters<>(
      List.of(
        new WalkProfileParameters<>("a", WalkPreferences.DEFAULT, WalkPreferences.DEFAULT),
        // No preferences of its own: they come from the request, which equals the default
        new WalkProfileParameters<>("b", null, WalkPreferences.DEFAULT)
      )
    );

    var ex = assertThrows(IllegalArgumentException.class, () ->
      TransferProfiles.of(parameters, new FakeTransferPathProvider(), FakeRequestMapper.INSTANCE)
    );
    assertThat(ex).hasMessageThat().contains("'a'");
    assertThat(ex).hasMessageThat().contains("'b'");
  }

  @Test
  void theProfileKeepsItsId() {
    var walk = new TransferProfile<>(
      new WalkProfileParameters<>("my-walk", WalkPreferences.DEFAULT, WalkPreferences.DEFAULT),
      MaxDurationsPreference.DEFAULT,
      new FakeTransferPathProvider(),
      FakeRequestMapper.INSTANCE
    );

    assertThat(walk.id()).isEqualTo("my-walk");
    assertThat(walk.toString()).contains("my-walk");
  }
}
