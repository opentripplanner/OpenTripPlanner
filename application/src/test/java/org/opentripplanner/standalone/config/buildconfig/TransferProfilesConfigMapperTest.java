package org.opentripplanner.standalone.config.buildconfig;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.basic.Ratio;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transfer.streetadapter.RegularTransferPreferencesMapper;
import org.opentripplanner.transit.transfer.regular.api.maxduration.BicycleMaxDurations;
import org.opentripplanner.transit.transfer.regular.api.maxduration.CarMaxDuration;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferParameters;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfile;
import org.opentripplanner.transit.transfer.regular.api.parameters.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.preferences.BicyclePreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.BicycleWalk;
import org.opentripplanner.transit.transfer.regular.api.preferences.CarPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.TimeSlopeSafetyTrianglePreference;
import org.opentripplanner.transit.transfer.regular.api.preferences.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.preferences.WheelchairPreferences;

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
    var subject = map("{ transfers: { walk: { maxDuration: '30m' } } }");

    assertThat(subject.profiles()).hasSize(1);
    assertThat(subject.profiles().getFirst().id()).isEqualTo("walk");
  }

  @Test
  void mapsTheIdAndTheUserPreferencesOfAWalkProfile() {
    var subject = map(
      """
      {
        transfers: {
          walk: { maxDuration: '30m',
            profiles: [{
              id: "slow walk",
              userPreferences: { speed: 1.1, reluctance: 3.0 }
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
        .build()
    );
  }

  @Test
  void aProfileMayHaveNoUserPreferences() {
    var subject = map(
      "{ transfers: { walk: { maxDuration: '30m', profiles: [{ id: 'walk' }] } } }"
    );

    assertThat(subject.profiles().getFirst().userPreferences()).isNull();
  }

  @Test
  void mapsTheRoutingRequestOfAWalkProfile() {
    var subject = map(
      """
      {
        transfers: {
          walk: { maxDuration: '30m',
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
          walk: { maxDuration: '30m',
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
      "{ transfers: { walk: { maxDuration: '30m', profiles: [{ userPreferences: { reluctance: 3.0 } }] } } }"
    );

    assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
  }

  @Test
  void profilesWithTheSameIdAreRejected() {
    var root = newNodeAdapterForTest(
      """
      {
        transfers: {
          walk: { maxDuration: '30m',
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
          walk: { maxDuration: '30m',
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
          walk: { maxDuration: '30m',
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
    assertThat(((WalkPreferences) slow.userPreferences()).speed().toMetersPerSecond())
      .isWithin(0.001)
      .of(1.1);
  }

  @Test
  void aChildInheritsTheResolvedPreferencesOfAParentWithoutOwnPreferences() {
    // The preferences of the parent are the ones of the default request
    var subject = map(
      """
      {
        transfers: {
          walk: { maxDuration: '30m',
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
    assertThat(((WalkPreferences) slow.userPreferences()).speed()).isEqualTo(
      WalkPreferences.DEFAULT.speed()
    );
    assertThat(slow.userPreferences().reluctance().value()).isEqualTo(5.0);
  }

  @Test
  void aChildInheritsTheRoutingRequestOfItsParentWithTheParentsPreferencesApplied() {
    var subject = map(
      """
      {
        transfers: {
          walk: { maxDuration: '30m',
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
          walk: { maxDuration: '30m',
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
    assertThat(
      ((WalkPreferences) profile(subject, "slow").userPreferences()).speed().toMetersPerSecond()
    )
      .isWithin(0.001)
      .of(1.1);
  }

  @Test
  void aChildWithoutPreferencesOfItsOwnIsADuplicateOfItsParent() {
    var root = newNodeAdapterForTest(
      """
      {
        transfers: {
          walk: { maxDuration: '30m', profiles: [{ id: "walk" }, { id: "copy", parent: "walk" }] }
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
      "{ transfers: { walk: { maxDuration: '30m', profiles: [{ id: 'a', parent: 'missing' }] } } }"
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
          walk: { maxDuration: '30m',
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
    assertThat(map("{}").deduplicationLimit()).isEqualTo(
      TransferParameters.DEFAULT_DEDUPLICATION_LIMIT
    );
  }

  @Test
  void mapsTheDeduplicationLimit() {
    var subject = map("{ transfers: { deduplicationLimit: '10s + 0.01 x' } }");

    assertThat(subject.deduplicationLimit().constant()).isEqualTo(Cost.costOfSeconds(10));
    assertThat(subject.deduplicationLimit().coefficient()).isEqualTo(0.01);
  }

  @Test
  void theDeduplicationLimitHasTheConstantFirst() {
    var root = newNodeAdapterForTest("{ transfers: { deduplicationLimit: '0.01x + 10s' } }");

    assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
  }

  @Test
  void mapsAWarmupEntryThatInheritsThePreferencesOfItsParent() {
    var subject = map(
      """
      {
        transfers: {
          walk: { maxDuration: '30m',
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
    assertThat(((WalkPreferences) entry.userPreferences()).speed().toMetersPerSecond())
      .isWithin(0.001)
      .of(1.1);
  }

  @Test
  void aWarmupEntryInheritsThePreferencesOfAParentWithoutPreferencesOfItsOwn() {
    var subject = map(
      """
      {
        transfers: {
          walk: { maxDuration: '30m',
            profiles: [{ id: "walk" }],
            warmup: [{ id: "avoid", parent: "walk", userPreferences: { reluctance: 9.0 } }]
          }
        }
      }
      """
    );

    var entry = subject.warmup().getFirst();
    assertThat(((WalkPreferences) entry.userPreferences()).speed()).isEqualTo(
      WalkPreferences.DEFAULT.speed()
    );
  }

  @Test
  void theDefaultProfileCanBeTheParentOfAWarmupEntry() {
    var subject = map(
      """
      {
        transfers: {
          walk: { maxDuration: '30m', warmup: [{ id: "avoid", parent: "walk", userPreferences: { reluctance: 9.0 } }] }
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
      "{ transfers: { walk: { maxDuration: '30m', warmup: [{ parent: 'walk', userPreferences: { reluctance: 9.0 } }] } } }"
    );

    assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
  }

  @Test
  void aWarmupEntryNeedsAParent() {
    var root = newNodeAdapterForTest(
      "{ transfers: { walk: { maxDuration: '30m', warmup: [{ id: 'a', userPreferences: { reluctance: 9.0 } }] } } }"
    );

    assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
  }

  @Test
  void aWarmupEntryNeedsUserPreferences() {
    var root = newNodeAdapterForTest(
      "{ transfers: { walk: { maxDuration: '30m', warmup: [{ id: 'a', parent: 'walk' }] } } }"
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
          walk: { maxDuration: '30m',
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
          walk: { maxDuration: '30m',
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
          walk: { maxDuration: '30m',
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
  void theMaxDurationOfATypeIsRequiredIfItHasProfiles() {
    var root = newNodeAdapterForTest(
      "{ transfers: { wheelchair: { profiles: [{ id: 'wheelchair' }] } } }"
    );

    var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
    assertThat(ex).hasMessageThat().contains("wheelchair");
    assertThat(ex).hasMessageThat().contains("max duration");
  }

  @Test
  void theMaxDurationOfATypeIsUsedByTheImplicitWalkProfile() {
    var subject = map("{ transfers: { walk: { maxDuration: '45m' } } }");

    assertThat(maxDuration(subject)).isEqualTo(Duration.ofMinutes(45));
  }

  @Test
  void theDefaultMaxDurationParameterIsNotSupported() {
    var subject = map("{ transfers: { defaultMaxDuration: '20m' } }");

    assertThat(maxDuration(subject)).isEqualTo(Duration.ofMinutes(30));
  }

  @Test
  void theMaxDurationsAreSharedByAllProfilesOfTheType() {
    var subject = map(
      """
      {
        transfers: {
          walk: {
            maxDuration: '10m',
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
  void theMaxDurationIsNotInheritedFromAParentProfile() {
    var subject = map(
      """
      {
        transfers: {
          walk: { maxDuration: '45m', profiles: [{ id: "walk" }] },
          bicycle: { maxDuration: '20m', profiles: [{ id: "bicycle", parent: "walk" }] }
        }
      }
      """
    );

    assertThat(subject.maxDurations(TransferProfileType.WALK).longestDuration()).isEqualTo(
      Duration.ofMinutes(45)
    );
    assertThat(subject.maxDurations(TransferProfileType.BICYCLE).longestDuration()).isEqualTo(
      Duration.ofMinutes(20)
    );
  }

  @Test
  void aBicycleHasLimitsForAllStopsBicycleStopsAndCarStops() {
    var subject = map(
      """
      {
        transfers: {
          bicycle: {
            profiles: [{ id: "bicycle" }],
            maxDurationAllowAllModes: '20m',
            maxDuration: '40m',
            maxDurationAllowsCar: '60m'
          }
        }
      }
      """
    );

    assertThat(subject.maxDurations(TransferProfileType.BICYCLE)).isEqualTo(
      new BicycleMaxDurations(
        Duration.ofMinutes(20),
        Duration.ofMinutes(40),
        Duration.ofMinutes(60)
      )
    );
  }

  @Test
  void aBicycleWithoutTheAllStopsLimitHasTransfersBetweenAllowedStopsOnly() {
    var subject = map(
      """
      {
        transfers: {
          bicycle: { profiles: [{ id: "bicycle" }], maxDurationAllowsCar: '3h' }
        }
      }
      """
    );

    assertThat(subject.maxDurations(TransferProfileType.BICYCLE)).isEqualTo(
      new BicycleMaxDurations(null, null, Duration.ofHours(3))
    );
  }

  @Test
  void theBicycleLimitsOfAllowedStopsMustBeLongerThanTheLimitForAllStops() {
    for (var parameter : List.of("maxDuration", "maxDurationAllowsCar")) {
      var root = newNodeAdapterForTest(
        "{ transfers: { bicycle: { maxDuration: '30m', profiles: [{ id: 'b' }], maxDurationAllowAllModes: '20m', " +
          parameter +
          ": '20m' } } }"
      );

      var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
      assertThat(ex).hasMessageThat().contains(parameter);
    }
  }

  @Test
  void theBicycleLimitsOfAllowedStopsAreIndependent() {
    var subject = map(
      """
      {
        transfers: {
          bicycle: {
            profiles: [{ id: "bicycle" }],
            maxDurationAllowAllModes: '10m',
            maxDuration: '60m',
            maxDurationAllowsCar: '30m'
          }
        }
      }
      """
    );

    assertThat(subject.maxDurations(TransferProfileType.BICYCLE)).isEqualTo(
      new BicycleMaxDurations(
        Duration.ofMinutes(10),
        Duration.ofMinutes(60),
        Duration.ofMinutes(30)
      )
    );
  }

  @Test
  void theBicycleOnlyLimitsAreNotSupportedForOtherTypes() {
    for (var parameter : List.of("maxDurationAllowAllModes", "maxDurationAllowsCar")) {
      for (var type : List.of("walk", "wheelchair", "car")) {
        var root = newNodeAdapterForTest(
          "{ transfers: { " + type + ": { " + parameter + ": '40m' } } }"
        );

        var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
        assertThat(ex).hasMessageThat().contains(parameter);
        assertThat(ex).hasMessageThat().contains(type);
      }
    }
  }

  @Test
  void theOldMaxDurationsListIsNotUsed() {
    var subject = map("{ transfers: { walk: { maxDurations: [{ maxDuration: '45m' }] } } }");

    assertThat(maxDuration(subject)).isEqualTo(Duration.ofMinutes(30));
  }

  @Test
  void aCarHasTransfersBetweenStopsThatAllowCarsOnly() {
    var subject = map(
      "{ transfers: { car: { maxDuration: '30m', profiles: [{ id: 'car' }], maxDuration: '3h' } } }"
    );

    assertThat(subject.maxDurations(TransferProfileType.CAR)).isEqualTo(
      new CarMaxDuration(Duration.ofHours(3))
    );
  }

  @Test
  void mapsAWheelchairProfile() {
    var subject = map(
      """
      {
        transfers: {
          walk: { maxDuration: '30m', profiles: [{ id: "walk" }] },
          wheelchair: { maxDuration: '30m',
            profiles: [{
              id: "wheelchair",
              userPreferences: { speed: 0.9, reluctance: 4.0 }
            }],
            maxDuration: "45m"
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
        .build()
    );
    assertThat(subject.maxDurations(TransferProfileType.WHEELCHAIR).longestDuration()).isEqualTo(
      Duration.ofMinutes(45)
    );
    assertThat(subject.maxDurations(TransferProfileType.WALK).longestDuration()).isEqualTo(
      Duration.ofMinutes(30)
    );
  }

  @Test
  void aWheelchairProfileDoesNotInheritFromAWalkParentButTakesTheValuesOfTheRequest() {
    var subject = map(
      """
      {
        transfers: {
          walk: { maxDuration: '30m',
            profiles: [{
              id: "walk",
              userPreferences: { speed: 1.1, reluctance: 3.0 }
            }]
          },
          wheelchair: { maxDuration: '30m',
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
    var request = new RegularTransferPreferencesMapper().toUserPreferences(
      TransferProfileType.WHEELCHAIR,
      RouteRequest.defaultValue()
    );
    assertThat(wheelchair.speed()).isEqualTo(((WheelchairPreferences) request).speed());
    assertThat(wheelchair.reluctance()).isEqualTo(Reluctance.of(5.0));
  }

  @Test
  void aParentCanBeDeclaredInAnotherTypeBlockAfterTheChild() {
    var subject = map(
      """
      {
        transfers: {
          walk: { maxDuration: '30m', profiles: [{ id: "walk", parent: "wheelchair" }] },
          wheelchair: { maxDuration: '30m', profiles: [{ id: "wheelchair" }] }
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
            walk: { maxDuration: '30m', profiles: [{ id: "walk" }] },
            wheelchair: { maxDuration: '30m',
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
    var subject = map(
      "{ transfers: { wheelchair: { maxDuration: '30m', profiles: [{ id: \"wheelchair\" }] } } }"
    );

    assertThat(subject.profiles()).hasSize(1);
    assertThat(subject.profiles().getFirst().type()).isEqualTo(TransferProfileType.WHEELCHAIR);
  }

  @Test
  void mapsABicycleProfile() {
    var subject = map(
      """
      {
        transfers: {
          walk: { maxDuration: '30m', profiles: [{ id: "walk" }] },
          bicycle: { maxDuration: '30m',
            profiles: [{
              id: "bicycle",
              userPreferences: { speed: 6.0, reluctance: 3.0 }
            }],
            maxDuration: "20m"
          }
        }
      }
      """
    );

    var bicycle = profile(subject, "bicycle");
    assertThat(bicycle.type()).isEqualTo(TransferProfileType.BICYCLE);
    assertThat(bicycle.userPreferences()).isEqualTo(
      BicyclePreferences.of()
        .withSpeed(Speed.ofMetersPerSecond(6.0))
        .withReluctance(Reluctance.of(3.0))
        .build()
    );
    assertThat(subject.maxDurations(TransferProfileType.BICYCLE).longestDuration()).isEqualTo(
      Duration.ofMinutes(20)
    );
  }

  @Test
  void aBicycleProfileDoesNotInheritFromAWalkParentButTakesTheValuesOfTheRequest() {
    var subject = map(
      """
      {
        transfers: {
          walk: { maxDuration: '30m',
            profiles: [{
              id: "walk",
              userPreferences: { speed: 1.1, reluctance: 3.0 }
            }]
          },
          bicycle: { maxDuration: '30m',
            profiles: [{
              id: "bicycle",
              parent: "walk",
              userPreferences: { speed: 5.5 }
            }]
          }
        }
      }
      """
    );

    var bicycle = (BicyclePreferences) profile(subject, "bicycle").userPreferences();
    var request = new RegularTransferPreferencesMapper().toUserPreferences(
      TransferProfileType.BICYCLE,
      RouteRequest.defaultValue()
    );
    assertThat(bicycle.speed()).isEqualTo(Speed.ofMetersPerSecond(5.5));
    assertThat(bicycle.reluctance()).isEqualTo(request.reluctance());
  }

  @Test
  void aProfileInheritsFromAParentOfTheSameTypeAndTheRootTakesTheValuesOfTheRequest() {
    var subject = map(
      """
      {
        transfers: {
          bicycle: { maxDuration: '30m',
            profiles: [
              { id: "root", userPreferences: { speed: 6.0 } },
              { id: "child", parent: "root", userPreferences: { reluctance: 7.0 } }
            ]
          }
        }
      }
      """
    );

    var root = (BicyclePreferences) profile(subject, "root").userPreferences();
    var child = (BicyclePreferences) profile(subject, "child").userPreferences();
    var request = new RegularTransferPreferencesMapper().toUserPreferences(
      TransferProfileType.BICYCLE,
      RouteRequest.defaultValue()
    );
    assertThat(root.reluctance()).isEqualTo(request.reluctance());
    assertThat(child.speed()).isEqualTo(Speed.ofMetersPerSecond(6.0));
    assertThat(child.reluctance()).isEqualTo(Reluctance.of(7.0));
  }

  @Test
  void mapsACarProfileWithoutSpeedAndStairs() {
    var subject = map(
      """
      {
        transfers: {
          walk: { maxDuration: '30m', profiles: [{ id: "walk" }] },
          car: { maxDuration: '30m',
            profiles: [{ id: "car", userPreferences: { reluctance: 3.0 } }],
            maxDuration: "3h"
          }
        }
      }
      """
    );

    var car = profile(subject, "car");
    assertThat(car.type()).isEqualTo(TransferProfileType.CAR);
    assertThat(car.userPreferences()).isEqualTo(
      CarPreferences.of().withReluctance(Reluctance.of(3.0)).build()
    );
    assertThat(subject.maxDurations(TransferProfileType.CAR).longestDuration()).isEqualTo(
      Duration.ofHours(3)
    );
  }

  @Test
  void theWalkingWithTheBicycleIsSetWithTheWalkSpeedAndReluctance() {
    var subject = map(
      """
      {
        transfers: {
          bicycle: {
            maxDuration: '30m',
            profiles: [
              { id: "root", userPreferences: { walk: { speed: 1.0 } } },
              { id: "child", parent: "root", userPreferences: { walk: { reluctance: 8.0 } } }
            ]
          }
        }
      }
      """
    );

    var root = (BicyclePreferences) profile(subject, "root").userPreferences();
    var child = (BicyclePreferences) profile(subject, "child").userPreferences();
    assertThat(root.walk().speed()).isEqualTo(Speed.ofMetersPerSecond(1.0));
    assertThat(root.walk().reluctance()).isEqualTo(BicyclePreferences.DEFAULT.walk().reluctance());
    // The child inherits the unset values of the parent of the same type
    assertThat(child.walk()).isEqualTo(
      BicycleWalk.of(Speed.ofMetersPerSecond(1.0), Reluctance.of(8.0))
    );
  }

  @Test
  void theSafetyFactorOfAWalkAndAWheelchairProfileIsSet() {
    var subject = map(
      """
      {
        transfers: {
          walk: {
            maxDuration: '30m',
            profiles: [
              { id: "walk", userPreferences: { safetyFactor: 0.5 } },
              { id: "child", parent: "walk", userPreferences: { reluctance: 3.0 } }
            ]
          },
          wheelchair: {
            maxDuration: '30m',
            profiles: [{ id: "wheelchair", userPreferences: { safetyFactor: 0.3 } }]
          }
        }
      }
      """
    );

    var walk = (WalkPreferences) profile(subject, "walk").userPreferences();
    var child = (WalkPreferences) profile(subject, "child").userPreferences();
    var wheelchair = (WheelchairPreferences) profile(subject, "wheelchair").userPreferences();
    assertThat(walk.safetyFactor()).isEqualTo(Ratio.of(0.5).normalize());
    assertThat(child.safetyFactor()).isEqualTo(Ratio.of(0.5).normalize());
    assertThat(wheelchair.safetyFactor()).isEqualTo(Ratio.of(0.3).normalize());
  }

  @Test
  void aSafetyFactorOutsideTheRangeIsRejected() {
    var root = newNodeAdapterForTest(
      """
      {
        transfers: {
          walk: {
            maxDuration: '30m',
            profiles: [{ id: "walk", userPreferences: { safetyFactor: 1.5 } }]
          }
        }
      }
      """
    );

    var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
    assertThat(ex).hasMessageThat().contains("safetyFactor");
  }

  @Test
  void theOptimizationOfABicycleIsSetWithTheTimeTheSlopeAndTheSafety() {
    var subject = map(
      """
      {
        transfers: {
          bicycle: {
            maxDuration: '30m',
            profiles: [
              { id: "time", userPreferences: { optimize: { time: 0.1 } } },
              { id: "mix", userPreferences: { optimize: { time: 2.0, slope: 3.0, safety: 5.0 } } },
              { id: "child", parent: "mix", userPreferences: { reluctance: 3.0 } },
              { id: "reset", parent: "mix", userPreferences: { optimize: { slope: 1.0 } } }
            ]
          }
        }
      }
      """
    );

    assertThat(optimize(subject, "time")).isEqualTo(
      TimeSlopeSafetyTrianglePreference.of(1.0, 0.0, 0.0)
    );
    assertThat(optimize(subject, "mix")).isEqualTo(
      TimeSlopeSafetyTrianglePreference.of(0.2, 0.3, 0.5)
    );
    // Inherited when it is not set, the values that are not set are 0.0 when it is set
    assertThat(optimize(subject, "child")).isEqualTo(optimize(subject, "mix"));
    assertThat(optimize(subject, "reset")).isEqualTo(
      TimeSlopeSafetyTrianglePreference.of(0.0, 1.0, 0.0)
    );
  }

  @Test
  void theOptimizationOfARootIsTheOptimizationOfTheRequestIfNotSet() {
    var subject = map(
      "{ transfers: { bicycle: { maxDuration: '30m', profiles: [{ id: 'b', userPreferences: { speed: 6.0 } }] } } }"
    );

    assertThat(optimize(subject, "b")).isEqualTo(
      TimeSlopeSafetyTrianglePreference.of(0.0, 0.0, 1.0)
    );
  }

  @Test
  void anOptimizationWithoutAnyValueIsRejected() {
    var root = newNodeAdapterForTest(
      """
      {
        transfers: {
          bicycle: {
            maxDuration: '30m',
            profiles: [{ id: "b", userPreferences: { optimize: {} } }]
          }
        }
      }
      """
    );

    var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
    assertThat(ex).hasMessageThat().contains("optimize");
  }

  @Test
  void anOptimizationWithANegativeValueIsRejected() {
    var root = newNodeAdapterForTest(
      """
      {
        transfers: {
          bicycle: {
            maxDuration: '30m',
            profiles: [{ id: "b", userPreferences: { optimize: { time: -1.0 } } }]
          }
        }
      }
      """
    );

    var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
    assertThat(ex).hasMessageThat().contains("optimize");
  }

  private static TimeSlopeSafetyTrianglePreference optimize(
    TransferParameters<RouteRequest> parameters,
    String id
  ) {
    return ((BicyclePreferences) profile(parameters, id).userPreferences()).optimize();
  }

  private static TransferParameters<RouteRequest> map(String json) {
    return TransferProfilesConfigMapper.map(newNodeAdapterForTest(json));
  }

  private static Duration maxDuration(TransferParameters<RouteRequest> parameters) {
    return parameters.maxDurations(TransferProfileType.WALK).longestDuration();
  }

  private static TransferProfile<RouteRequest> profile(
    TransferParameters<RouteRequest> parameters,
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
