package org.opentripplanner.standalone.config.buildconfig;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Reluctance;
import org.opentripplanner.core.model.basic.Speed;
import org.opentripplanner.framework.application.OtpAppException;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.transfer.regular.api.TransferProfileType;
import org.opentripplanner.transit.transfer.regular.api.TransferProfilesParameters;
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.ways.StreetSegmentTypeUsage;

class TransferProfilesConfigMapperTest {

  @Test
  void aSingleDefaultWalkProfileIsUsedWithoutConfiguration() {
    var subject = map("{}");

    assertThat(subject.walk()).hasSize(1);
    var walk = subject.walk().getFirst();
    assertThat(walk.id()).isEqualTo("walk");
    assertThat(walk.userPreferences()).isNull();
    assertThat(walk.routingRequest()).isEqualTo(RouteRequest.defaultValue());
  }

  @Test
  void aWalkBlockWithoutProfilesUsesTheDefaultProfile() {
    var subject = map("{ transfers: { walk: {} } }");

    assertThat(subject.walk()).hasSize(1);
    assertThat(subject.walk().getFirst().id()).isEqualTo("walk");
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

    assertThat(subject.walk()).hasSize(1);
    var profile = subject.walk().getFirst();
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

    assertThat(subject.walk().getFirst().userPreferences()).isNull();
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

    var request = subject.walk().getFirst().routingRequest();
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
        .walk()
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
  void parentIsNotSupportedYet() {
    var root = newNodeAdapterForTest(
      "{ transfers: { walk: { profiles: [{ id: 'a', parent: 'b' }] } } }"
    );

    var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
    assertThat(ex).hasMessageThat().contains("parent");
  }

  @Test
  void warmupIsNotSupportedYet() {
    var root = newNodeAdapterForTest(
      "{ transfers: { walk: { warmup: [{ id: 'a', parent: 'walk' }] } } }"
    );

    var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
    assertThat(ex).hasMessageThat().contains("warmup");
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

    assertThat(subject.walk()).hasSize(2);
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

  private static TransferProfilesParameters<RouteRequest> map(String json) {
    return TransferProfilesConfigMapper.map(newNodeAdapterForTest(json));
  }

  private static Duration maxDuration(TransferProfilesParameters<RouteRequest> parameters) {
    return parameters.maxDurations(TransferProfileType.WALK).maxDuration();
  }
}
