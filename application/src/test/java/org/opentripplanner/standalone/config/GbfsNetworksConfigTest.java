package org.opentripplanner.standalone.config;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import org.junit.jupiter.api.Test;
import org.opentripplanner.gbfs.network.GeofencingZonePhase;

class GbfsNetworksConfigTest {

  @Test
  void missingSectionLoadsNoNetworks() {
    var subject = map("{}");

    assertThat(subject.byNetwork()).isEmpty();
    assertFalse(subject.includeUnlistedNetworks());
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

    assertEquals(GeofencingZonePhase.REALTIME, tier.geofencingZones());
    assertFalse(tier.requireDropOffInsideBusinessArea());
    assertTrue(tier.allowKeepingVehicleAtDestination());
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

    assertFalse(subject.forNetwork("voi").orElseThrow().requireDropOffInsideBusinessArea());
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
    assertEquals(GeofencingZonePhase.REALTIME, listed.geofencingZones());
    assertTrue(listed.requireDropOffInsideBusinessArea());

    var unlisted = subject.forNetwork("voioslo").orElseThrow();
    assertEquals(GeofencingZonePhase.REALTIME, unlisted.geofencingZones());
    assertFalse(unlisted.requireDropOffInsideBusinessArea());
  }

  @Test
  void hardCodedDefaultsApplyWhenNoDefaultsBlockIsGiven() {
    var subject = map(
      """
      { "gbfs" : { "networks" : [ { "network" : "tier" } ] } }
      """
    );

    var tier = subject.forNetwork("tier").orElseThrow();

    assertEquals(GeofencingZonePhase.OFF, tier.geofencingZones());
    assertTrue(tier.requireDropOffInsideBusinessArea());
    assertFalse(tier.allowKeepingVehicleAtDestination());
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

    assertEquals(
      GeofencingZonePhase.REALTIME,
      subject.forNetwork("ryde").orElseThrow().geofencingZones()
    );
  }

  @Test
  void aNetworkCanOptOutOfZonesEnabledByTheDefaults() {
    var subject = map(
      """
      {
        "gbfs" : {
          "includeUnlistedNetworks" : true,
          "defaults" : { "geofencingZones" : "realtime" },
          "networks" : [ { "network" : "tier", "geofencingZones" : "off" } ]
        }
      }
      """
    );

    assertEquals(
      GeofencingZonePhase.OFF,
      subject.forNetwork("tier").orElseThrow().geofencingZones()
    );
    assertEquals(
      GeofencingZonePhase.REALTIME,
      subject.forNetwork("ryde").orElseThrow().geofencingZones()
    );
  }

  private static org.opentripplanner.gbfs.network.GbfsNetworkOverrides map(String json) {
    return GbfsNetworksConfig.map("gbfs", newNodeAdapterForTest(json));
  }
}
