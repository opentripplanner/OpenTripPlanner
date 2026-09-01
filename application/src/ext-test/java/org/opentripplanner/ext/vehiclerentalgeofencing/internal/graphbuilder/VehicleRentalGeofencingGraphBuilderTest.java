package org.opentripplanner.ext.vehiclerentalgeofencing.internal.graphbuilder;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.gbfs.network.GeofencingZoneOtpPhase.GRAPH_BUILD;
import static org.opentripplanner.gbfs.network.GeofencingZoneOtpPhase.OFF;
import static org.opentripplanner.gbfs.network.GeofencingZoneOtpPhase.SERVE;

import java.net.URI;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mobilitydata.gbfs.v3_0.manifest.GBFSManifest;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.gbfs.manifest.GbfsManifestLoader;
import org.opentripplanner.gbfs.network.GbfsNetworkOverrides;
import org.opentripplanner.gbfs.network.GbfsNetworkParameters;
import org.opentripplanner.gbfs.network.GeofencingZoneOtpPhase;
import org.opentripplanner.test.support.ResourceLoader;

/**
 * The test manifest lists two datasets:
 * <ul>
 *   <li>{@code tieroslo} - publishes {@code geofencing_zones}</li>
 *   <li>{@code duplicate-stations} - v2 and v3 feeds, neither publishing geofencing zones</li>
 * </ul>
 */
class VehicleRentalGeofencingGraphBuilderTest {

  private static final URI MANIFEST = ResourceLoader.of(
    VehicleRentalGeofencingGraphBuilderTest.class
  ).uri("/gbfs/manifest.json");

  @Test
  void selectsOnlyNetworksThatPublishGeofencingZones() {
    var selected = selectNetworks(allNetworks(GRAPH_BUILD));

    assertThat(selected).containsExactly("tieroslo");
  }

  @Test
  void skipsNetworksThatAreNotConfigured() {
    var overrides = new GbfsNetworkOverrides(defaults(GRAPH_BUILD), false, Map.of());

    assertThat(selectNetworks(overrides)).isEmpty();
  }

  @Test
  void selectsAListedNetworkEvenWhenUnlistedNetworksAreExcluded() {
    var overrides = new GbfsNetworkOverrides(
      defaults(OFF),
      false,
      Map.of("tieroslo", defaults(GRAPH_BUILD))
    );

    assertThat(selectNetworks(overrides)).containsExactly("tieroslo");
  }

  @Test
  void skipsNetworksInTheRealtimePhase() {
    assertThat(selectNetworks(allNetworks(SERVE))).isEmpty();
  }

  @Test
  void skipsNetworksWithGeofencingTurnedOff() {
    assertThat(selectNetworks(allNetworks(OFF))).isEmpty();
  }

  private static List<String> selectNetworks(GbfsNetworkOverrides overrides) {
    try (var clientFactory = new OtpHttpClientFactory()) {
      return VehicleRentalGeofencingGraphBuilder.selectNetworks(
        manifest(),
        overrides,
        HttpHeaders.empty(),
        clientFactory
      )
        .stream()
        .map(VehicleRentalGeofencingGraphBuilder.SelectedNetwork::network)
        .toList();
    }
  }

  private static GBFSManifest manifest() {
    return GbfsManifestLoader.loadManifest(MANIFEST, HttpHeaders.empty());
  }

  /** Every dataset in the manifest is a candidate, in the given scope. */
  private static GbfsNetworkOverrides allNetworks(GeofencingZoneOtpPhase scope) {
    return new GbfsNetworkOverrides(defaults(scope), true, Map.of());
  }

  private static GbfsNetworkParameters defaults(GeofencingZoneOtpPhase scope) {
    return new GbfsNetworkParameters(scope, true, false);
  }
}
