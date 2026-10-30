package org.opentripplanner.transit.transfer.regular.internal;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.CostLinearFunction;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.transit.transfer.regular.FakeRequestMapper;
import org.opentripplanner.transit.transfer.regular.FakeTransferPathProvider;
import org.opentripplanner.transit.transfer.regular.api.maxduration.MaxDurationsPreference;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferParameters;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfile;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.parameters.WarmupParameters;
import org.opentripplanner.transit.transfer.regular.api.preferences.AbstractUserPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WheelchairPreferences;
import org.opentripplanner.transit.transfer.regular.spi.TransferRequestMapper;

class TransferProfilesTest {

  @Test
  void ofWrapsTheGivenProfilesPreservingOrder() {
    var walk = new TransferProfileState<>(
      new TransferProfile<>(
        "walk",
        TransferProfileType.WALK,
        WalkPreferences.DEFAULT,
        WalkPreferences.DEFAULT
      ),
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
    var parameters = new TransferParameters<>(
      List.of(new TransferProfile<>("derived", TransferProfileType.WALK, null, request))
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
    var parameters = new TransferParameters<>(
      List.of(
        new TransferProfile<>(
          "a",
          TransferProfileType.WALK,
          WalkPreferences.DEFAULT,
          WalkPreferences.DEFAULT
        ),
        // No preferences of its own: they come from the request, which equals the default
        new TransferProfile<>("b", TransferProfileType.WALK, null, WalkPreferences.DEFAULT)
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
    var walk = new TransferProfileState<>(
      new TransferProfile<>(
        "my-walk",
        TransferProfileType.WALK,
        WalkPreferences.DEFAULT,
        WalkPreferences.DEFAULT
      ),
      MaxDurationsPreference.DEFAULT,
      new FakeTransferPathProvider(),
      FakeRequestMapper.INSTANCE
    );

    assertThat(walk.id()).isEqualTo("my-walk");
    assertThat(walk.toString()).contains("my-walk");
  }

  @Test
  void aParentIsBeforeItsChildrenWhateverTheDeclaredOrder() {
    var parameters = new TransferParameters<>(
      List.of(
        new TransferProfile<>(
          "child",
          TransferProfileType.WALK,
          "parent",
          withReluctance(3.0),
          withReluctance(3.0)
        ),
        new TransferProfile<>(
          "other",
          TransferProfileType.WALK,
          null,
          withReluctance(4.0),
          withReluctance(4.0)
        ),
        new TransferProfile<>(
          "parent",
          TransferProfileType.WALK,
          null,
          withReluctance(2.0),
          withReluctance(2.0)
        )
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
    var parameters = new TransferParameters<>(
      List.of(
        new TransferProfile<>(
          "parent",
          TransferProfileType.WALK,
          null,
          withReluctance(2.0),
          withReluctance(2.0)
        ),
        new TransferProfile<>(
          "child",
          TransferProfileType.WALK,
          "parent",
          withReluctance(3.0),
          withReluctance(3.0)
        )
      )
    );

    var profiles = TransferProfiles.of(
      parameters,
      new FakeTransferPathProvider(),
      FakeRequestMapper.INSTANCE
    );

    var byId = new HashMap<String, TransferProfileState<?, ?>>();
    profiles.forEach(p -> byId.put(p.id(), p));
    assertThat(byId.get("child").parent()).isSameInstanceAs(byId.get("parent"));
    assertThat(byId.get("parent").parent()).isNull();
  }

  private static WalkPreferences withReluctance(double reluctance) {
    return WalkPreferences.of().withReluctance(Reluctance.of(reluctance)).build();
  }

  @Test
  void aWarmupEntryWithTheUserPreferencesOfAProfileIsRejectedAndNamed() {
    var parameters = new TransferParameters<>(
      List.of(
        new TransferProfile<>(
          "walk",
          TransferProfileType.WALK,
          withReluctance(2.0),
          withReluctance(2.0)
        )
      ),
      Map.of(TransferProfileType.WALK, MaxDurationsPreference.DEFAULT),
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
    var parameters = new TransferParameters<>(
      List.of(
        new TransferProfile<>(
          "walk",
          TransferProfileType.WALK,
          withReluctance(2.0),
          withReluctance(2.0)
        )
      ),
      Map.of(TransferProfileType.WALK, MaxDurationsPreference.DEFAULT),
      List.of(
        new WarmupParameters("a", "walk", withReluctance(5.0)),
        new WarmupParameters("b", "walk", withReluctance(5.0))
      )
    );

    assertThrows(IllegalArgumentException.class, () ->
      TransferProfiles.of(parameters, new FakeTransferPathProvider(), FakeRequestMapper.INSTANCE)
    );
  }

  private static final CostLinearFunction LIMIT_A = CostLinearFunction.of(
    Duration.ofSeconds(5),
    0.1
  );
  private static final CostLinearFunction LIMIT_B = CostLinearFunction.of(
    Duration.ofSeconds(7),
    0.2
  );

  private static TransferProfile<WalkPreferences> walk(
    String id,
    String parent,
    double reluctance,
    CostLinearFunction limit
  ) {
    var preferences = withReluctance(reluctance);
    return new TransferProfile<>(
      id,
      TransferProfileType.WALK,
      parent,
      preferences,
      preferences,
      limit
    );
  }

  private static Map<String, CostLinearFunction> reuseLimits(
    List<TransferProfile<WalkPreferences>> profiles
  ) {
    var result = new HashMap<String, CostLinearFunction>();
    TransferProfiles.of(
      new TransferParameters<>(
        profiles,
        Map.of(TransferProfileType.WALK, MaxDurationsPreference.DEFAULT)
      ),
      new FakeTransferPathProvider(),
      FakeRequestMapper.INSTANCE
    ).forEach(profile -> result.put(profile.id(), profile.reuseLimit()));
    return result;
  }

  @Test
  void aProfileWithoutALimitUsesTheDefaultLimit() {
    var limits = reuseLimits(List.of(walk("walk", null, 2.0, null)));

    assertThat(limits.get("walk")).isEqualTo(TransferParameters.DEFAULT_REUSE_LIMIT);
  }

  @Test
  void aProfileUsesItsOwnLimit() {
    var limits = reuseLimits(
      List.of(walk("walk", null, 2.0, null), walk("child", "walk", 3.0, LIMIT_A))
    );

    assertThat(limits.get("child")).isEqualTo(LIMIT_A);
  }

  @Test
  void aChildInheritsTheLimitOfItsParentOfTheSameType() {
    var limits = reuseLimits(
      List.of(
        walk("walk", null, 2.0, LIMIT_A),
        walk("child", "walk", 3.0, null),
        walk("grandchild", "child", 4.0, null)
      )
    );

    assertThat(limits.get("walk")).isEqualTo(LIMIT_A);
    assertThat(limits.get("child")).isEqualTo(LIMIT_A);
    assertThat(limits.get("grandchild")).isEqualTo(LIMIT_A);
  }

  @Test
  void aChildThatSetsALimitKeepsItAndPassesItOn() {
    var limits = reuseLimits(
      List.of(
        walk("walk", null, 2.0, LIMIT_A),
        walk("child", "walk", 3.0, LIMIT_B),
        walk("grandchild", "child", 4.0, null)
      )
    );

    assertThat(limits.get("child")).isEqualTo(LIMIT_B);
    assertThat(limits.get("grandchild")).isEqualTo(LIMIT_B);
  }

  @Test
  void aChildDoesNotInheritTheLimitOfAParentOfAnotherType() {
    var parent = walk("walk", null, 2.0, LIMIT_A);
    var child = new TransferProfile<>(
      "wheelchair",
      TransferProfileType.WHEELCHAIR,
      "walk",
      WheelchairPreferences.DEFAULT,
      WalkPreferences.DEFAULT,
      null
    );
    // The fake mapper only knows the walk type, the request is not used here
    var mapper = new TransferRequestMapper<WalkPreferences>() {
      @Override
      public WalkPreferences toRequest(
        AbstractUserPreferences<?> userPreferences,
        WalkPreferences base
      ) {
        return base;
      }

      @Override
      public AbstractUserPreferences<?> toUserPreferences(WalkPreferences request) {
        return request;
      }

      @Override
      public AbstractUserPreferences<?> toUserPreferences(
        TransferProfileType type,
        WalkPreferences request
      ) {
        return request;
      }
    };
    var limits = new HashMap<String, CostLinearFunction>();

    TransferProfiles.of(
      new TransferParameters<>(
        List.of(parent, child),
        Map.of(
          TransferProfileType.WALK,
          MaxDurationsPreference.DEFAULT,
          TransferProfileType.WHEELCHAIR,
          MaxDurationsPreference.DEFAULT
        )
      ),
      new FakeTransferPathProvider(),
      mapper
    ).forEach(profile -> limits.put(profile.id(), profile.reuseLimit()));

    assertThat(limits.get("walk")).isEqualTo(LIMIT_A);
    assertThat(limits.get("wheelchair")).isEqualTo(TransferParameters.DEFAULT_REUSE_LIMIT);
  }
}
