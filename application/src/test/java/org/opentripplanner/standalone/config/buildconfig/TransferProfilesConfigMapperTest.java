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
import org.opentripplanner.transit.transfer.regular.api.WalkPreferences;
import org.opentripplanner.transit.transfer.regular.api.ways.StreetSegmentTypeUsage;

class TransferProfilesConfigMapperTest {

  @Test
  void aSingleDefaultWalkProfileIsUsedWithoutConfiguration() {
    var subject = TransferProfilesConfigMapper.map(newNodeAdapterForTest("{}"));

    assertThat(subject.walk()).hasSize(1);
    var walk = subject.walk().getFirst();
    assertThat(walk.userPreferences()).isEqualTo(WalkPreferences.DEFAULT);
    assertThat(walk.routingRequest()).isEqualTo(RouteRequest.defaultValue());
  }

  @Test
  void mapsTheUserPreferencesOfAWalkProfile() {
    var subject = TransferProfilesConfigMapper.map(
      newNodeAdapterForTest(
        """
        {
          transfers: {
            profiles: {
              walk: [{ userPreferences: { speed: 1.1, reluctance: 3.0, stairs: "FORBIDDEN" } }]
            }
          }
        }
        """
      )
    );

    assertThat(subject.walk()).hasSize(1);
    assertThat(subject.walk().getFirst().userPreferences()).isEqualTo(
      WalkPreferences.of()
        .withSpeed(Speed.ofMetersPerSecond(1.1))
        .withReluctance(Reluctance.of(3.0))
        .withStairs(StreetSegmentTypeUsage.FORBIDDEN)
        .build()
    );
  }

  @Test
  void mapsTheRoutingRequestOfAWalkProfile() {
    var subject = TransferProfilesConfigMapper.map(
      newNodeAdapterForTest(
        """
        {
          transfers: {
            profiles: {
              walk: [{ routingRequest: { wheelchairAccessibility: { enabled: true } } }]
            }
          }
        }
        """
      )
    );

    var request = subject.walk().getFirst().routingRequest();
    assertThat(request.journey().wheelchair()).isTrue();
  }

  @Test
  void keepsTheProfilesInTheDeclaredOrder() {
    var subject = TransferProfilesConfigMapper.map(
      newNodeAdapterForTest(
        """
        {
          transfers: {
            profiles: {
              walk: [
                { userPreferences: { reluctance: 3.0 } },
                { userPreferences: { reluctance: 5.0 } }
              ]
            }
          }
        }
        """
      )
    );

    assertThat(
      subject
        .walk()
        .stream()
        .map(p -> p.userPreferences().reluctance().value())
    )
      .containsExactly(3.0, 5.0)
      .inOrder();
  }

  @Test
  void profilesWithEqualUserPreferencesAreRejected() {
    var root = newNodeAdapterForTest(
      """
      {
        transfers: {
          profiles: {
            walk: [
              { userPreferences: { reluctance: 3.0 } },
              { userPreferences: { reluctance: 3.0 } }
            ]
          }
        }
      }
      """
    );

    assertThrows(IllegalArgumentException.class, () -> TransferProfilesConfigMapper.map(root));
  }

  @Test
  void theDefaultMaxDurationIsThirtyMinutes() {
    var subject = TransferProfilesConfigMapper.map(newNodeAdapterForTest("{}"));

    assertThat(subject.maxDurations(TransferProfileType.WALK).maxDuration()).isEqualTo(
      Duration.ofMinutes(30)
    );
  }

  @Test
  void defaultMaxDurationAppliesToTypesWithoutAnOverride() {
    var subject = TransferProfilesConfigMapper.map(
      newNodeAdapterForTest("{ transfers: { defaultMaxDuration: '20m' } }")
    );

    assertThat(subject.maxDurations(TransferProfileType.WALK).maxDuration()).isEqualTo(
      Duration.ofMinutes(20)
    );
  }

  @Test
  void maxDurationsOfATypeReplaceTheDefault() {
    var subject = TransferProfilesConfigMapper.map(
      newNodeAdapterForTest(
        """
        {
          transfers: {
            defaultMaxDuration: '20m',
            maxDurations: { walk: [{ maxDuration: '45m' }] }
          }
        }
        """
      )
    );

    assertThat(subject.maxDurations(TransferProfileType.WALK).maxDuration()).isEqualTo(
      Duration.ofMinutes(45)
    );
  }

  @Test
  void theMaxDurationsAreSharedByAllProfilesOfTheType() {
    var subject = TransferProfilesConfigMapper.map(
      newNodeAdapterForTest(
        """
        {
          transfers: {
            maxDurations: { walk: [{ maxDuration: '10m' }] },
            profiles: {
              walk: [
                { userPreferences: { reluctance: 3.0 } },
                { userPreferences: { reluctance: 5.0 } }
              ]
            }
          }
        }
        """
      )
    );

    assertThat(subject.walk()).hasSize(2);
    assertThat(subject.maxDurations(TransferProfileType.WALK).maxDuration()).isEqualTo(
      Duration.ofMinutes(10)
    );
  }

  @Test
  void theOldMaxTransferDurationIsNotUsed() {
    var subject = TransferProfilesConfigMapper.map(
      newNodeAdapterForTest("{ maxTransferDuration: '5m' }")
    );

    assertThat(subject.maxDurations(TransferProfileType.WALK).maxDuration()).isEqualTo(
      Duration.ofMinutes(30)
    );
  }

  @Test
  void moreThanOneMaxDurationForATypeIsRejected() {
    var root = newNodeAdapterForTest(
      """
      { transfers: { maxDurations: { walk: [{ maxDuration: '10m' }, { maxDuration: '20m' }] } } }
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
          maxDurations: { walk: [{ maxDuration: '10m', allowedModes: ['CAR'] }] }
        }
      }
      """
    );

    var ex = assertThrows(OtpAppException.class, () -> TransferProfilesConfigMapper.map(root));
    assertThat(ex).hasMessageThat().contains("allowedModes");
  }
}
