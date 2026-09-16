package org.opentripplanner.standalone.config;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.standalone.config.framework.json.JsonSupport.newNodeAdapterForTest;

import org.junit.jupiter.api.Test;
import org.opentripplanner.gbfs.network.GeofencingZoneOtpPhase;

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
            "applyGeofencingZones" : "off",
            "requireDropOffInsideBusinessArea" : false,
            "allowKeepingVehicleAtDestination" : true
          },
          "networks" : [ { "network" : "tier", "applyGeofencingZones" : "serve" } ]
        }
      }
      """
    );

    var tier = subject.forNetwork("tier").orElseThrow();

    assertEquals(GeofencingZoneOtpPhase.SERVE, tier.geofencingZonePhase());
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
            "applyGeofencingZones" : "serve",
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
    assertEquals(GeofencingZoneOtpPhase.SERVE, listed.geofencingZonePhase());
    assertTrue(listed.requireDropOffInsideBusinessArea());

    var unlisted = subject.forNetwork("voioslo").orElseThrow();
    assertEquals(GeofencingZoneOtpPhase.SERVE, unlisted.geofencingZonePhase());
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

    assertEquals(GeofencingZoneOtpPhase.OFF, tier.geofencingZonePhase());
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
          "defaults" : { "applyGeofencingZones" : "serve" },
          "networks" : []
        }
      }
      """
    );

    assertEquals(
      GeofencingZoneOtpPhase.SERVE,
      subject.forNetwork("ryde").orElseThrow().geofencingZonePhase()
    );
  }

  @Test
  void aNetworkCanOptOutOfZonesEnabledByTheDefaults() {
    var subject = map(
      """
      {
        "gbfs" : {
          "includeUnlistedNetworks" : true,
          "defaults" : { "applyGeofencingZones" : "serve" },
          "networks" : [ { "network" : "tier", "applyGeofencingZones" : "off" } ]
        }
      }
      """
    );

    assertEquals(
      GeofencingZoneOtpPhase.OFF,
      subject.forNetwork("tier").orElseThrow().geofencingZonePhase()
    );
    assertEquals(
      GeofencingZoneOtpPhase.SERVE,
      subject.forNetwork("ryde").orElseThrow().geofencingZonePhase()
    );
  }

  private static org.opentripplanner.gbfs.network.GbfsNetworkOverrides map(String json) {
    return GbfsNetworksConfig.map("gbfs", newNodeAdapterForTest(json));
  }
}
