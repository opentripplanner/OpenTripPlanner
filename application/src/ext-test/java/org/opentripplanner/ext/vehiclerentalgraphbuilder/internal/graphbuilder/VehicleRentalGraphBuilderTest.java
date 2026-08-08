package org.opentripplanner.ext.vehiclerentalgraphbuilder.internal.graphbuilder;

import static com.google.common.truth.Truth.assertThat;
import static org.opentripplanner.gbfs.network.GeofencingZonePhase.OFF;
import static org.opentripplanner.gbfs.network.GeofencingZonePhase.PERMANENT;
import static org.opentripplanner.gbfs.network.GeofencingZonePhase.REALTIME;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mobilitydata.gbfs.v3_0.manifest.GBFSManifest;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.gbfs.manifest.GbfsManifestLoader;
import org.opentripplanner.gbfs.network.GbfsNetworkOverrides;
import org.opentripplanner.gbfs.network.GbfsNetworkParameters;
import org.opentripplanner.gbfs.network.GeofencingZonePhase;

/**
 * The test manifest lists three datasets:
 * <ul>
 *   <li>{@code tieroslo} - a v2 feed that publishes {@code geofencing_zones}</li>
 *   <li>{@code duplicate-stations} - v2 and v3 feeds, neither publishing geofencing zones</li>
 *   <li>{@code no-versions} - a dataset publishing no GBFS version at all</li>
 * </ul>
 */
class VehicleRentalGraphBuilderTest {

  private static final URI MANIFEST = Path.of("src/test/resources/gbfs/manifest.json")
    .toAbsolutePath()
    .toUri();

  @Test
  void selectsOnlyNetworksThatPublishGeofencingZones() {
    var selected = selectNetworks(allNetworks(PERMANENT));

    assertThat(selected).containsExactly("tieroslo");
  }

  @Test
  void skipsNetworksThatAreNotConfigured() {
    var overrides = new GbfsNetworkOverrides(defaults(PERMANENT), false, Map.of());

    assertThat(selectNetworks(overrides)).isEmpty();
  }

  @Test
  void selectsAListedNetworkEvenWhenUnlistedNetworksAreExcluded() {
    var overrides = new GbfsNetworkOverrides(
      defaults(OFF),
      false,
      Map.of("tieroslo", defaults(PERMANENT))
    );

    assertThat(selectNetworks(overrides)).containsExactly("tieroslo");
  }

  @Test
  void skipsNetworksHandledAtRuntime() {
    assertThat(selectNetworks(allNetworks(REALTIME))).isEmpty();
  }

  @Test
  void skipsNetworksWithGeofencingTurnedOff() {
    assertThat(selectNetworks(allNetworks(OFF))).isEmpty();
  }

  private static List<String> selectNetworks(GbfsNetworkOverrides overrides) {
    try (var clientFactory = new OtpHttpClientFactory()) {
      return VehicleRentalGraphBuilder.selectNetworks(
        manifest(),
        overrides,
        HttpHeaders.empty(),
        clientFactory
      )
        .stream()
        .map(VehicleRentalGraphBuilder.SelectedNetwork::network)
        .toList();
    }
  }

  private static GBFSManifest manifest() {
    return GbfsManifestLoader.loadManifest(MANIFEST, HttpHeaders.empty());
  }

  /** Every dataset in the manifest is a candidate, in the given phase. */
  private static GbfsNetworkOverrides allNetworks(GeofencingZonePhase phase) {
    return new GbfsNetworkOverrides(defaults(phase), true, Map.of());
  }

  private static GbfsNetworkParameters defaults(GeofencingZonePhase phase) {
    return new GbfsNetworkParameters(phase, true, false);
  }
}
