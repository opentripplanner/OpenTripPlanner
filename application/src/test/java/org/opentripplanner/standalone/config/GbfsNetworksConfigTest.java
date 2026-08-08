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
          "networks" : [ { "network" : "tier", "geofencingZones" : "permanent" } ]
        }
      }
      """
    );

    var tier = subject.forNetwork("tier").orElseThrow();

    assertThat(tier.geofencingZones()).isEqualTo(GeofencingZonePhase.PERMANENT);
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
