package org.opentripplanner.standalone.config;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import org.junit.jupiter.api.Test;
import org.opentripplanner.gbfs.network.GeofencingZonePhase;

class GbfsNetworksConfigTest {

  @Test
  void missingSectionLoadsNoNetworks() {
    var subject = map("{}");

    assertThat(subject.byNetwork()).isEmpty();
    assertThat(subject.includeUnlistedNetworks()).isFalse();
    assertThat(subject.forNetwork("tier")).isEmpty();
  }

  @Test
  void listedNetworkInheritsUnnamedFieldsFromDefaults() {
    var subject = map(
      """
      {
        "gbfs" : {
          "defaults" : {
            "geofencingZones" : "off",
            "requireDropOffInsideBusinessArea" : false,
            "allowKeepingVehicleAtDestination" : true
          },
          "networks" : [ { "network" : "tier", "geofencingZones" : "realtime" } ]
        }
      }
      """
    );

    var tier = subject.forNetwork("tier").orElseThrow();

    assertThat(tier.geofencingZones()).isEqualTo(GeofencingZonePhase.REALTIME);
    assertThat(tier.requireDropOffInsideBusinessArea()).isFalse();
    assertThat(tier.allowKeepingVehicleAtDestination()).isTrue();
  }

  @Test
  void listedNetworkOverridesDefaults() {
    var subject = map(
      """
      {
        "gbfs" : {
          "defaults" : { "requireDropOffInsideBusinessArea" : true },
          "networks" : [
            { "network" : "voi", "requireDropOffInsideBusinessArea" : false }
          ]
        }
      }
      """
    );

    assertThat(
      subject.forNetwork("voi").orElseThrow().requireDropOffInsideBusinessArea()
    ).isFalse();
  }

  /**
   * The shape a deployment uses to build zones for every system that publishes them: the phase is
   * set once in the defaults and only the business-area exceptions are listed.
   */
  @Test
  void listedNetworkInheritsThePhaseWhileOverridingBusinessAreas() {
    var subject = map(
      """
      {
        "gbfs" : {
          "includeUnlistedNetworks" : true,
          "defaults" : {
            "geofencingZones" : "realtime",
            "requireDropOffInsideBusinessArea" : false
          },
          "networks" : [
            { "network" : "boltoslo", "requireDropOffInsideBusinessArea" : true }
          ]
        }
      }
      """
    );

    var listed = subject.forNetwork("boltoslo").orElseThrow();
    assertThat(listed.geofencingZones()).isEqualTo(GeofencingZonePhase.REALTIME);
    assertThat(listed.requireDropOffInsideBusinessArea()).isTrue();

    var unlisted = subject.forNetwork("voioslo").orElseThrow();
    assertThat(unlisted.geofencingZones()).isEqualTo(GeofencingZonePhase.REALTIME);
    assertThat(unlisted.requireDropOffInsideBusinessArea()).isFalse();
  }

  @Test
  void hardCodedDefaultsApplyWhenNoDefaultsBlockIsGiven() {
    var subject = map(
      """
      { "gbfs" : { "networks" : [ { "network" : "tier" } ] } }
      """
    );

    var tier = subject.forNetwork("tier").orElseThrow();

    assertThat(tier.geofencingZones()).isEqualTo(GeofencingZonePhase.OFF);
    assertThat(tier.requireDropOffInsideBusinessArea()).isTrue();
    assertThat(tier.allowKeepingVehicleAtDestination()).isFalse();
  }

  @Test
  void unlistedNetworksAreIncludedWhenConfigured() {
    var subject = map(
      """
      {
        "gbfs" : {
          "includeUnlistedNetworks" : true,
          "defaults" : { "geofencingZones" : "realtime" },
          "networks" : []
        }
      }
      """
    );

    assertThat(subject.forNetwork("ryde").orElseThrow().geofencingZones()).isEqualTo(
      GeofencingZonePhase.REALTIME
    );
  }

  private static org.opentripplanner.gbfs.network.GbfsNetworkOverrides map(String json) {
    return GbfsNetworksConfig.map("gbfs", newNodeAdapterForTest(json));
  }
}
