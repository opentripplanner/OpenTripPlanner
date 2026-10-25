package org.opentripplanner.standalone.config.buildconfig;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.transfer.regular.api.parameters.DeduplicationLimit;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileParameters;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfilesParameters;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WheelchairPreferences;
import org.opentripplanner.transit.transfer.regular.api.ways.StreetSegmentTypeUsage;

class TransferProfilesConfigMapperTest {

  @Test
  void aSingleDefaultWalkProfileIsUsedWithoutConfiguration() {
    var subject = map("{}");

    assertThat(subject.profiles()).hasSize(1);
    var walk = subject.profiles().getFirst();
    assertThat(walk.id()).isEqualTo("walk");
    assertThat(walk.userPreferences()).isNull();
    assertThat(walk.routingRequest()).isEqualTo(RouteRequest.defaultValue());
  }

  @Test
  void aWalkBlockWithoutProfilesUsesTheDefaultProfile() {
    var subject = map("{ transfers: { walk: {} } }");

    assertThat(subject.profiles()).hasSize(1);
    assertThat(subject.profiles().getFirst().id()).isEqualTo("walk");
  }

  @Test
  void mapsTheIdAndTheUserPreferencesOfAWalkProfile() {
    var subject = map(
      """
      {
        transfers: {
          walk: {
            profiles: [{
              id: "slow walk",
              userPreferences: { speed: 1.1, reluctance: 3.0, stairs: "FORBIDDEN" }
            }]
          }
        }
      }
      """
    );

    assertThat(subject.profiles()).hasSize(1);
    var profile = subject.profiles().getFirst();
    assertThat(profile.id()).isEqualTo("slow walk");
    assertThat(profile.userPreferences()).isEqualTo(
      WalkPreferences.of()
        .withSpeed(Speed.ofMetersPerSecond(1.1))
        .withReluctance(Reluctance.of(3.0))
        .withStairs(StreetSegmentTypeUsage.FORBIDDEN)
        .build()
    );
  }

  @Test
  void aProfileMayHaveNoUserPreferences() {
    var subject = map("{ transfers: { walk: { profiles: [{ id: 'walk' }] } } }");

    assertThat(subject.profiles().getFirst().userPreferences()).isNull();
  }

  @Test
  void mapsTheRoutingRequestOfAWalkProfile() {
    var subject = map(
      """
      {
        transfers: {
          walk: {
            profiles: [{
              id: "wheelchair-walk",
              routingRequest: { wheelchairAccessibility: { enabled: true } }
            }]
          }
        }
      }
      """
    );

    var request = subject.profiles().getFirst().routingRequest();
    assertThat(request.journey().wheelchair()).isTrue();
  }

  @Test
  void keepsTheProfilesInTheDeclaredOrder() {
    var subject = map(
      """
      {
        transfers: {
          walk: {
            profiles: [
              { id: "b", userPreferences: { reluctance: 3.0 } },
              { id: "a", userPreferences: { reluctance: 5.0 } }
            ]
          }
        }
      }
      """
    );

    assertThat(
      subject
        .profiles()
        .stream()
        .map(p -> p.id())
    )
      .containsExactly("b", "a")
      .inOrder();
  }

  @Test
  void aProfileWithoutAnIdIsRejected() {
    var root = newNodeAdapterForTest(
      "{ transfers: { walk: { profiles: [{ userPreferences: { reluctance: 3.0 } }] } } }"
    );

    assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
  }

  @Test
  void profilesWithTheSameIdAreRejected() {
    var root = newNodeAdapterForTest(
      """
      {
        transfers: {
          walk: {
            profiles: [
              { id: "walk", userPreferences: { reluctance: 3.0 } },
              { id: "walk", userPreferences: { reluctance: 5.0 } }
            ]
          }
        }
      }
      """
    );

    var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
    assertThat(ex).hasMessageThat().contains("walk");
  }

  @Test
  void profilesWithEqualUserPreferencesAreRejectedAndNamed() {
    // The first profile gets the preferences of the default request, the second sets the same
    var root = newNodeAdapterForTest(
      """
      {
        transfers: {
          walk: {
            profiles: [
              { id: "a" },
              { id: "b", userPreferences: { speed: 1.35, reluctance: 2.0 } }
            ]
          }
        }
      }
      """
    );

    var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
    assertThat(ex).hasMessageThat().contains("'a'");
    assertThat(ex).hasMessageThat().contains("'b'");
  }

  @Test
  void aChildInheritsTheUserPreferencesOfItsParent() {
    var subject = map(
      """
      {
        transfers: {
          walk: {
            profiles: [
              { id: "walk", userPreferences: { speed: 1.1, reluctance: 3.0 } },
              { id: "slow", parent: "walk", userPreferences: { reluctance: 5.0 } }
            ]
          }
        }
      }
      """
    );

    var slow = profile(subject, "slow");
    assertThat(slow.parent()).isEqualTo("walk");
    // Set on the child
    assertThat(slow.userPreferences().reluctance().value()).isEqualTo(5.0);
    // Not set on the child, so the one of the parent
    assertThat(slow.userPreferences().speed().toMetersPerSecond()).isWithin(0.001).of(1.1);
  }

  @Test
  void aChildInheritsTheResolvedPreferencesOfAParentWithoutOwnPreferences() {
    // The preferences of the parent are the ones of the default request
    var subject = map(
      """
      {
        transfers: {
          walk: {
            profiles: [
              { id: "walk" },
              { id: "slow", parent: "walk", userPreferences: { reluctance: 5.0 } }
            ]
          }
        }
      }
      """
    );

    var slow = profile(subject, "slow");
    assertThat(slow.userPreferences().speed()).isEqualTo(WalkPreferences.DEFAULT.speed());
    assertThat(slow.userPreferences().reluctance().value()).isEqualTo(5.0);
  }

  @Test
  void aChildInheritsTheRoutingRequestOfItsParentWithTheParentsPreferencesApplied() {
    var subject = map(
      """
      {
        transfers: {
          walk: {
            profiles: [
              {
                id: "walk",
                userPreferences: { speed: 1.1 },
                routingRequest: { numItineraries: 7 }
              },
              { id: "slow", parent: "walk", userPreferences: { reluctance: 5.0 } }
            ]
          }
        }
      }
      """
    );

    var request = profile(subject, "slow").routingRequest();
    assertThat(request.numItineraries()).isEqualTo(7);
    assertThat(request.preferences().walk().speed()).isWithin(0.001).of(1.1);
  }

  @Test
  void aChildDeclaredBeforeItsParentIsMappedAndTheDeclaredOrderIsKept() {
    var subject = map(
      """
      {
        transfers: {
          walk: {
            profiles: [
              { id: "slow", parent: "walk", userPreferences: { reluctance: 5.0 } },
              { id: "walk", userPreferences: { speed: 1.1, reluctance: 3.0 } }
            ]
          }
        }
      }
      """
    );

    assertThat(
      subject
        .profiles()
        .stream()
        .map(p -> p.id())
    )
      .containsExactly("slow", "walk")
      .inOrder();
    assertThat(profile(subject, "slow").userPreferences().speed().toMetersPerSecond())
      .isWithin(0.001)
      .of(1.1);
  }

  @Test
  void aChildWithoutPreferencesOfItsOwnIsADuplicateOfItsParent() {
    var root = newNodeAdapterForTest(
      """
      {
        transfers: {
          walk: { profiles: [{ id: "walk" }, { id: "copy", parent: "walk" }] }
        }
      }
      """
    );

    var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
    assertThat(ex).hasMessageThat().contains("'walk'");
    assertThat(ex).hasMessageThat().contains("'copy'");
  }

  @Test
  void aParentThatDoesNotExistIsRejectedAndNamed() {
    var root = newNodeAdapterForTest(
      "{ transfers: { walk: { profiles: [{ id: 'a', parent: 'missing' }] } } }"
    );

    var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
    assertThat(ex).hasMessageThat().contains("'missing'");
    assertThat(ex).hasMessageThat().contains("'a'");
  }

  @Test
  void aCycleOfParentsIsRejected() {
    var root = newNodeAdapterForTest(
      """
      {
        transfers: {
          walk: {
            profiles: [
              { id: "a", parent: "b", userPreferences: { reluctance: 3.0 } },
              { id: "b", parent: "a", userPreferences: { reluctance: 4.0 } }
            ]
          }
        }
      }
      """
    );

    var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
    assertThat(ex).hasMessageThat().contains("cycle");
  }

  @Test
  void theDeduplicationLimitIsTheDefaultIfNotConfigured() {
    assertThat(map("{}").deduplicationLimit()).isEqualTo(DeduplicationLimit.DEFAULT);
  }

  @Test
  void mapsTheDeduplicationLimit() {
    var subject = map("{ transfers: { deduplicationLimit: '10s + 0.01 x' } }");

    assertThat(subject.deduplicationLimit().constant()).isEqualTo(Cost.costOfSeconds(10));
    assertThat(subject.deduplicationLimit().coefficient()).isEqualTo(0.01);
  }

  @Test
  void mapsAWarmupEntryThatInheritsThePreferencesOfItsParent() {
    var subject = map(
      """
      {
        transfers: {
          walk: {
            profiles: [{ id: "walk", userPreferences: { speed: 1.1, reluctance: 3.0 } }],
            warmup: [{ id: "avoid", parent: "walk", userPreferences: { reluctance: 9.0 } }]
          }
        }
      }
      """
    );

    assertThat(subject.warmup()).hasSize(1);
    var entry = subject.warmup().getFirst();
    assertThat(entry.id()).isEqualTo("avoid");
    assertThat(entry.parent()).isEqualTo("walk");
    assertThat(entry.userPreferences().reluctance().value()).isEqualTo(9.0);
    // Not set on the entry, so the one of the parent
    assertThat(entry.userPreferences().speed().toMetersPerSecond()).isWithin(0.001).of(1.1);
  }

  @Test
  void aWarmupEntryInheritsThePreferencesOfAParentWithoutPreferencesOfItsOwn() {
    var subject = map(
      """
      {
        transfers: {
          walk: {
            profiles: [{ id: "walk" }],
            warmup: [{ id: "avoid", parent: "walk", userPreferences: { reluctance: 9.0 } }]
          }
        }
      }
      """
    );

    var entry = subject.warmup().getFirst();
    assertThat(entry.userPreferences().speed()).isEqualTo(WalkPreferences.DEFAULT.speed());
  }

  @Test
  void theDefaultProfileCanBeTheParentOfAWarmupEntry() {
    var subject = map(
      """
      {
        transfers: {
          walk: { warmup: [{ id: "avoid", parent: "walk", userPreferences: { reluctance: 9.0 } }] }
        }
      }
      """
    );

    assertThat(subject.warmup()).hasSize(1);
    assertThat(subject.profiles().getFirst().id()).isEqualTo("walk");
  }

  @Test
  void thereAreNoWarmupEntriesByDefault() {
    assertThat(map("{}").warmup()).isEmpty();
  }

  @Test
  void aWarmupEntryNeedsAnId() {
    var root = newNodeAdapterForTest(
      "{ transfers: { walk: { warmup: [{ parent: 'walk', userPreferences: { reluctance: 9.0 } }] } } }"
    );

    assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
  }

  @Test
  void aWarmupEntryNeedsAParent() {
    var root = newNodeAdapterForTest(
      "{ transfers: { walk: { warmup: [{ id: 'a', userPreferences: { reluctance: 9.0 } }] } } }"
    );

    assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
  }

  @Test
  void aWarmupEntryNeedsUserPreferences() {
    var root = newNodeAdapterForTest(
      "{ transfers: { walk: { warmup: [{ id: 'a', parent: 'walk' }] } } }"
    );

    var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
    assertThat(ex).hasMessageThat().contains("'a'");
    assertThat(ex).hasMessageThat().contains("userPreferences");
  }

  @Test
  void theParentOfAWarmupEntryMustBeAProfile() {
    var root = newNodeAdapterForTest(
      """
      {
        transfers: {
          walk: {
            profiles: [{ id: "walk" }],
            warmup: [
              { id: "a", parent: "walk", userPreferences: { reluctance: 9.0 } },
              { id: "b", parent: "a", userPreferences: { reluctance: 7.0 } }
            ]
          }
        }
      }
      """
    );

    var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
    assertThat(ex).hasMessageThat().contains("'a'");
    assertThat(ex).hasMessageThat().contains("'b'");
  }

  @Test
  void aWarmupIdMustBeUniqueAmongTheProfilesAndTheWarmupEntries() {
    var root = newNodeAdapterForTest(
      """
      {
        transfers: {
          walk: {
            profiles: [{ id: "walk" }],
            warmup: [{ id: "walk", parent: "walk", userPreferences: { reluctance: 9.0 } }]
          }
        }
      }
      """
    );

    var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
    assertThat(ex).hasMessageThat().contains("walk");
  }

  @Test
  void aWarmupEntryWithTheUserPreferencesOfAProfileIsRejectedAndNamed() {
    var root = newNodeAdapterForTest(
      """
      {
        transfers: {
          walk: {
            profiles: [{ id: "walk", userPreferences: { reluctance: 3.0 } }],
            warmup: [{ id: "copy", parent: "walk", userPreferences: { reluctance: 3.0 } }]
          }
        }
      }
      """
    );

    var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
    assertThat(ex).hasMessageThat().contains("'walk'");
    assertThat(ex).hasMessageThat().contains("'copy'");
  }

  @Test
  void theDefaultMaxDurationIsThirtyMinutes() {
    assertThat(maxDuration(map("{}"))).isEqualTo(Duration.ofMinutes(30));
  }

  @Test
  void defaultMaxDurationAppliesToTypesWithoutAnOverride() {
    var subject = map("{ transfers: { defaultMaxDuration: '20m' } }");

    assertThat(maxDuration(subject)).isEqualTo(Duration.ofMinutes(20));
  }

  @Test
  void maxDurationsOfATypeReplaceTheDefault() {
    var subject = map(
      """
      {
        transfers: {
          defaultMaxDuration: '20m',
          walk: { maxDurations: [{ maxDuration: '45m' }] }
        }
      }
      """
    );

    assertThat(maxDuration(subject)).isEqualTo(Duration.ofMinutes(45));
  }

  @Test
  void theMaxDurationsAreSharedByAllProfilesOfTheType() {
    var subject = map(
      """
      {
        transfers: {
          walk: {
            maxDurations: [{ maxDuration: '10m' }],
            profiles: [
              { id: "a", userPreferences: { reluctance: 3.0 } },
              { id: "b", userPreferences: { reluctance: 5.0 } }
            ]
          }
        }
      }
      """
    );

    assertThat(subject.profiles()).hasSize(2);
    assertThat(maxDuration(subject)).isEqualTo(Duration.ofMinutes(10));
  }

  @Test
  void theOldMaxTransferDurationIsNotUsed() {
    assertThat(maxDuration(map("{ maxTransferDuration: '5m' }"))).isEqualTo(Duration.ofMinutes(30));
  }

  @Test
  void moreThanOneMaxDurationForATypeIsRejected() {
    var root = newNodeAdapterForTest(
      """
      {
        transfers: { walk: { maxDurations: [{ maxDuration: '10m' }, { maxDuration: '20m' }] } }
      }
      """
    );

    var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
    assertThat(ex).hasMessageThat().contains("walk");
  }

  @Test
  void allowedModesAreNotSupportedYet() {
    var root = newNodeAdapterForTest(
      """
      {
        transfers: {
          walk: { maxDurations: [{ maxDuration: '10m', allowedModes: ['CAR'] }] }
        }
      }
      """
    );

    var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
    assertThat(ex).hasMessageThat().contains("allowedModes");
  }

  @Test
  void mapsAWheelchairProfile() {
    var subject = map(
      """
      {
        transfers: {
          walk: { profiles: [{ id: "walk" }] },
          wheelchair: {
            profiles: [{
              id: "wheelchair",
              userPreferences: { speed: 0.9, reluctance: 4.0, stairs: "FORBIDDEN" }
            }],
            maxDurations: [{ maxDuration: "45m" }]
          }
        }
      }
      """
    );

    assertThat(subject.profiles()).hasSize(2);
    var wheelchair = profile(subject, "wheelchair");
    assertThat(wheelchair.type()).isEqualTo(TransferProfileType.WHEELCHAIR);
    assertThat(wheelchair.userPreferences()).isEqualTo(
      WheelchairPreferences.of()
        .withSpeed(Speed.ofMetersPerSecond(0.9))
        .withReluctance(Reluctance.of(4.0))
        .withStairs(StreetSegmentTypeUsage.FORBIDDEN)
        .build()
    );
    assertThat(subject.maxDurations(TransferProfileType.WHEELCHAIR).maxDuration()).isEqualTo(
      Duration.ofMinutes(45)
    );
    assertThat(subject.maxDurations(TransferProfileType.WALK).maxDuration()).isEqualTo(
      Duration.ofMinutes(30)
    );
  }

  @Test
  void aWheelchairProfileInheritsTheValuesInCommonFromAWalkParent() {
    var subject = map(
      """
      {
        transfers: {
          walk: {
            profiles: [{
              id: "walk",
              userPreferences: { speed: 1.1, reluctance: 3.0, stairs: "DISCOURAGED" }
            }]
          },
          wheelchair: {
            profiles: [{
              id: "wheelchair",
              parent: "walk",
              userPreferences: { reluctance: 5.0 }
            }]
          }
        }
      }
      """
    );

    var wheelchair = (WheelchairPreferences) profile(subject, "wheelchair").userPreferences();
    assertThat(wheelchair.speed()).isEqualTo(Speed.ofMetersPerSecond(1.1));
    assertThat(wheelchair.reluctance()).isEqualTo(Reluctance.of(5.0));
    assertThat(wheelchair.stairs()).isEqualTo(StreetSegmentTypeUsage.DISCOURAGED);
  }

  @Test
  void aParentCanBeDeclaredInAnotherTypeBlockAfterTheChild() {
    var subject = map(
      """
      {
        transfers: {
          walk: { profiles: [{ id: "walk", parent: "wheelchair" }] },
          wheelchair: { profiles: [{ id: "wheelchair" }] }
        }
      }
      """
    );

    assertThat(
      subject
        .profiles()
        .stream()
        .map(p -> p.id())
        .toList()
    ).containsExactly("walk", "wheelchair");
  }

  @Test
  void aWarmupParentMustBeOfTheTypeOfTheWarmup() {
    var ex = assertThrows(OtpAppException.class, () ->
      map(
        """
        {
          transfers: {
            walk: { profiles: [{ id: "walk" }] },
            wheelchair: {
              profiles: [{ id: "wheelchair" }],
              warmup: [{ id: "w", parent: "walk", userPreferences: { speed: 1.0 } }]
            }
          }
        }
        """
      )
    );
    assertThat(ex.getMessage()).contains("is a walk profile");
  }

  @Test
  void noDefaultProfileIsAddedIfOnlyAWheelchairProfileIsConfigured() {
    var subject = map("{ transfers: { wheelchair: { profiles: [{ id: \"wheelchair\" }] } } }");

    assertThat(subject.profiles()).hasSize(1);
    assertThat(subject.profiles().getFirst().type()).isEqualTo(TransferProfileType.WHEELCHAIR);
  }

  private static TransferProfilesParameters<RouteRequest> map(String json) {
    return TransferProfilesConfigMapper.map(newNodeAdapterForTest(json));
  }

  private static Duration maxDuration(TransferProfilesParameters<RouteRequest> parameters) {
    return parameters.maxDurations(TransferProfileType.WALK).maxDuration();
  }

  private static TransferProfileParameters<RouteRequest> profile(
    TransferProfilesParameters<RouteRequest> parameters,
    String id
  ) {
    return parameters
      .profiles()
      .stream()
      .filter(p -> p.id().equals(id))
      .findFirst()
      .orElseThrow();
  }
}
