package org.opentripplanner.ext.vehiclerentalgraphbuilder.internal.graphbuilder;

import java.util.ArrayList;
import java.util.List;
import org.mobilitydata.gbfs.v3_0.manifest.GBFSManifest;
import org.opentripplanner.ext.vehiclerentalgraphbuilder.parameters.VehicleRentalGraphBuilderParameters;
import org.opentripplanner.ext.vehiclerentalgraphbuilder.parameters.VehicleRentalNetworkDataSourceParameters;
import org.opentripplanner.framework.io.HttpHeaders;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.gbfs.GbfsAutoConfiguration;
import org.opentripplanner.gbfs.GbfsFeedLoaderAndMapper;
import org.opentripplanner.gbfs.manifest.GbfsManifestLoader;
import org.opentripplanner.gbfs.network.GbfsNetworkOverrides;
import org.opentripplanner.gbfs.network.GbfsNetworkParameters;
import org.opentripplanner.gbfs.network.GeofencingZonePhase;
import org.opentripplanner.graph_builder.model.GraphBuilderModule;
import org.opentripplanner.service.vehiclerental.internal.DefaultVehicleRentalRepository;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.street.geofencing.GeofencingZoneApplier;
import org.opentripplanner.street.Scope;
import org.opentripplanner.street.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Graph builder module that loads GBFS geofencing zones at build time and applies them to street
 * edges.
 * <p>
 * The networks to load are discovered from a GBFS v3 manifest: a dataset is loaded when the shared
 * {@code gbfs} configuration puts it in the {@link GeofencingZonePhase#PERMANENT} phase and its
 * feed actually publishes a {@code geofencing_zones} feed. Zones are applied and indexed per
 * network, so {@code requireDropOffInsideBusinessArea} takes effect for exactly the networks that
 * enable it.
 *
 * <p>Computed zones and the spatial indices are registered on
 * {@link DefaultVehicleRentalRepository} via its build-time setter — the raw zones are persisted
 * via {@code SerializedGraphObject} so the runtime application sees them after deserialization.
 */
public class VehicleRentalGraphBuilder implements GraphBuilderModule {

  private static final Logger LOG = LoggerFactory.getLogger(VehicleRentalGraphBuilder.class);

  /** The GBFS feed that must be published for a network to be worth loading here. */
  private static final String GEOFENCING_ZONES_FEED = "geofencing_zones";

  /** Prefix of the repository key a network's build-time zone index is registered under. */
  private static final String INDEX_KEY_PREFIX = "permanent:";

  private final VehicleRentalGraphBuilderParameters parameters;
  private final GbfsNetworkOverrides overrides;
  private final Graph graph;
  private final DefaultVehicleRentalRepository rentalRepository;

  public VehicleRentalGraphBuilder(
    VehicleRentalGraphBuilderParameters parameters,
    GbfsNetworkOverrides overrides,
    Graph graph,
    DefaultVehicleRentalRepository rentalRepository
  ) {
    this.parameters = parameters;
    this.overrides = overrides;
    this.graph = graph;
    this.rentalRepository = rentalRepository;
  }

  @Override
  public void buildGraph() {
    LOG.info("Loading GBFS geofencing zones at build time from manifest {}", parameters.url());

    var manifest = GbfsManifestLoader.loadManifest(parameters.url(), parameters.headers());
    if (
      manifest == null || manifest.getData() == null || manifest.getData().getDatasets() == null
    ) {
      LOG.warn("No datasets found in GBFS manifest {}", parameters.url());
      return;
    }

    try (var httpClientFactory = new OtpHttpClientFactory()) {
      var networks = selectNetworks(manifest, overrides, parameters.headers(), httpClientFactory);
      for (var network : networks) {
        try {
          applyNetwork(network, httpClientFactory);
        } catch (Exception e) {
          LOG.error("Failed to load geofencing zones for network {}", network.network(), e);
        }
      }
    }
  }

  /**
   * The networks from the manifest that are configured for the permanent phase and publish
   * geofencing zones. Static and package private so the selection rules can be tested without a
   * graph or a repository.
   */
  static List<SelectedNetwork> selectNetworks(
    GBFSManifest manifest,
    GbfsNetworkOverrides overrides,
    HttpHeaders headers,
    OtpHttpClientFactory clientFactory
  ) {
    var selected = new ArrayList<SelectedNetwork>();

    for (var dataset : manifest.getData().getDatasets()) {
      var network = dataset.getSystemId();

      var override = overrides.forNetwork(network);
      if (override.isEmpty()) {
        LOG.warn("Network not configured in OTP, skipping: {}", network);
        continue;
      }
      var parameters = override.get();
      if (parameters.geofencingZones() != GeofencingZonePhase.PERMANENT) {
        LOG.debug(
          "Network {} does not build geofencing zones at graph build time ({}), skipping",
          network,
          parameters.geofencingZones()
        );
        continue;
      }

      var url = GbfsManifestLoader.selectBestVersion(dataset);
      if (url.isEmpty()) {
        LOG.warn("No suitable GBFS version found for network {}", network);
        continue;
      }

      GbfsAutoConfiguration autoConfiguration;
      try {
        autoConfiguration = GbfsAutoConfiguration.fetch(
          url.get(),
          headers,
          clientFactory.create(LOG)
        );
      } catch (Exception e) {
        LOG.error("Failed to fetch the GBFS auto-configuration file for network {}", network, e);
        continue;
      }

      if (!autoConfiguration.feedNames().contains(GEOFENCING_ZONES_FEED)) {
        LOG.debug("Network {} publishes no geofencing zones, skipping", network);
        continue;
      }

      selected.add(new SelectedNetwork(network, parameters, autoConfiguration));
    }
    return selected;
  }

  private void applyNetwork(SelectedNetwork network, OtpHttpClientFactory clientFactory) {
    var zones = loadGeofencingZones(network, clientFactory);
    if (zones.isEmpty()) {
      LOG.info("No geofencing zones loaded for network {}", network.network());
      return;
    }

    var applier = new GeofencingZoneApplier(
      ls -> graph.findEdgesAlongLineStrings(ls, Scope.PERMANENT),
      graph::findEdges,
      network.parameters().requireDropOffInsideBusinessArea()
    );
    var result = applier.applyGeofencingZones(zones);

    rentalRepository.setGeofencingZoneIndex(indexKey(network.network()), result.zoneIndex(), zones);

    LOG.info(
      "Applied {} geofencing zones with {} boundary vertices for network {}",
      zones.size(),
      result.boundaryVertices().size(),
      network.network()
    );
  }

  private List<GeofencingZone> loadGeofencingZones(
    SelectedNetwork network,
    OtpHttpClientFactory clientFactory
  ) {
    var dataSourceParameters = new VehicleRentalNetworkDataSourceParameters(
      network.autoConfiguration().url(),
      network.network(),
      parameters.language(),
      parameters.headers()
    );
    var loaderAndMapper = GbfsFeedLoaderAndMapper.create(
      dataSourceParameters,
      network.autoConfiguration(),
      clientFactory
    );

    if (!loaderAndMapper.update()) {
      LOG.warn("Failed to update GBFS feed for network {}", network.network());
      return List.of();
    }

    // getUpdated() must be called to trigger geofencing zone mapping internally
    loaderAndMapper.getUpdated();

    return loaderAndMapper.getGeofencingZones();
  }

  /** The repository key a network's build-time zone index is registered under. */
  public static String indexKey(String network) {
    return INDEX_KEY_PREFIX + network;
  }

  /** A manifest dataset that passed every selection rule, with its fetched auto-configuration. */
  record SelectedNetwork(
    String network,
    GbfsNetworkParameters parameters,
    GbfsAutoConfiguration autoConfiguration
  ) {}
}
