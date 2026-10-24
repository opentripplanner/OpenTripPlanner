package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.transit.transfer.regular.FakeRequestMapper;
import org.opentripplanner.transit.transfer.regular.FakeTransferPathProvider;
import org.opentripplanner.transit.transfer.regular.api.DeduplicationLimit;
import org.opentripplanner.transit.transfer.regular.api.MaxDurationsPreference;
import org.opentripplanner.transit.transfer.regular.api.TransferProfilesParameters;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.WalkProfileParameters;
import org.opentripplanner.transit.transfer.regular.api.WarmupParameters;

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

  @Test
  void aParentIsBeforeItsChildrenWhateverTheDeclaredOrder() {
    var parameters = new TransferProfilesParameters<>(
      List.of(
        new WalkProfileParameters<>("child", "parent", withReluctance(3.0), withReluctance(3.0)),
        new WalkProfileParameters<>("other", null, withReluctance(4.0), withReluctance(4.0)),
        new WalkProfileParameters<>("parent", null, withReluctance(2.0), withReluctance(2.0))
      )
    );

    var profiles = TransferProfiles.of(
      parameters,
      new FakeTransferPathProvider(),
      FakeRequestMapper.INSTANCE
    );

    var ids = new ArrayList<String>();
    profiles.forEach(p -> ids.add(p.id()));
    assertThat(ids).containsExactly("parent", "child", "other").inOrder();
  }

  @Test
  void aChildKnowsItsParent() {
    var parameters = new TransferProfilesParameters<>(
      List.of(
        new WalkProfileParameters<>("parent", null, withReluctance(2.0), withReluctance(2.0)),
        new WalkProfileParameters<>("child", "parent", withReluctance(3.0), withReluctance(3.0))
      )
    );

    var profiles = TransferProfiles.of(
      parameters,
      new FakeTransferPathProvider(),
      FakeRequestMapper.INSTANCE
    );

    var byId = new HashMap<String, TransferProfile<?, ?>>();
    profiles.forEach(p -> byId.put(p.id(), p));
    assertThat(byId.get("child").parent()).isSameInstanceAs(byId.get("parent"));
    assertThat(byId.get("parent").parent()).isNull();
  }

  private static WalkPreferences withReluctance(double reluctance) {
    return WalkPreferences.of().withReluctance(Reluctance.of(reluctance)).build();
  }

  @Test
  void aWarmupEntryWithTheUserPreferencesOfAProfileIsRejectedAndNamed() {
    var parameters = new TransferProfilesParameters<>(
      List.of(new WalkProfileParameters<>("walk", withReluctance(2.0), withReluctance(2.0))),
      MaxDurationsPreference.DEFAULT,
      Map.of(),
      DeduplicationLimit.DEFAULT,
      List.of(new WarmupParameters("same", "walk", withReluctance(2.0)))
    );

    var ex = assertThrows(IllegalArgumentException.class, () ->
      TransferProfiles.of(parameters, new FakeTransferPathProvider(), FakeRequestMapper.INSTANCE)
    );
    assertThat(ex).hasMessageThat().contains("'walk'");
    assertThat(ex).hasMessageThat().contains("'same'");
  }

  @Test
  void warmupEntriesWithTheSameUserPreferencesAreRejected() {
    var parameters = new TransferProfilesParameters<>(
      List.of(new WalkProfileParameters<>("walk", withReluctance(2.0), withReluctance(2.0))),
      MaxDurationsPreference.DEFAULT,
      Map.of(),
      DeduplicationLimit.DEFAULT,
      List.of(
        new WarmupParameters("a", "walk", withReluctance(5.0)),
        new WarmupParameters("b", "walk", withReluctance(5.0))
      )
    );

    assertThrows(IllegalArgumentException.class, () ->
      TransferProfiles.of(parameters, new FakeTransferPathProvider(), FakeRequestMapper.INSTANCE)
    );
  }
}
