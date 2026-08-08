package org.opentripplanner.ext.vehiclerentalservicedirectory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.mobilitydata.gbfs.v3_0.manifest.GBFSDataset;
import org.mobilitydata.gbfs.v3_0.manifest.GBFSManifest;
import org.opentripplanner.ext.vehiclerentalservicedirectory.api.VehicleRentalServiceDirectoryFetcherParameters;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.gbfs.manifest.GbfsManifestLoader;
import org.opentripplanner.gbfs.network.GbfsNetworkOverrides;
import org.opentripplanner.gbfs.network.GeofencingZonePhase;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.updater.spi.GraphUpdater;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalUpdater;
import org.opentripplanner.updater.vehicle_rental.datasources.VehicleRentalDataSourceFactory;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;
import org.opentripplanner.updater.vehicle_rental.datasources.params.RentalPickupType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches GBFS endpoints from a GBFS v3 manifest.json file.
 * The manifest can be loaded from a remote URL or a local file.
 */
public class VehicleRentalServiceDirectoryFetcher {

  private static final Logger LOG = LoggerFactory.getLogger(
    VehicleRentalServiceDirectoryFetcher.class
  );
  private static final Duration DEFAULT_FREQUENCY = Duration.ofSeconds(15);
  private static final Duration DEFAULT_STARTUP_RETRY_PERIOD = Duration.ZERO;

  private final VertexLinker vertexLinker;
  private final VehicleRentalRepository repository;
  private final OtpHttpClientFactory otpHttpClientFactory;

  public VehicleRentalServiceDirectoryFetcher(
    VertexLinker vertexLinker,
    VehicleRentalRepository repository,
    OtpHttpClientFactory otpHttpClientFactory
  ) {
    this.vertexLinker = vertexLinker;
    this.repository = repository;
    this.otpHttpClientFactory = otpHttpClientFactory;
  }

  public static List<GraphUpdater> createUpdatersFromEndpoint(
    VehicleRentalServiceDirectoryFetcherParameters parameters,
    GbfsNetworkOverrides overrides,
    VertexLinker vertexLinker,
    VehicleRentalRepository repository
  ) {
    LOG.info("Fetching GBFS v3 manifest from {}", parameters.getUrl());

    var manifest = GbfsManifestLoader.loadManifest(parameters.getUrl(), parameters.getHeaders());

    if (
      manifest == null || manifest.getData() == null || manifest.getData().getDatasets() == null
    ) {
      LOG.warn("No datasets found in manifest from {}", parameters.getUrl());
      return List.of();
    }

    int maxHttpConnections = manifest.getData().getDatasets().size();
    var otpHttpClientFactory = new OtpHttpClientFactory(maxHttpConnections);

    var serviceDirectory = new VehicleRentalServiceDirectoryFetcher(
      vertexLinker,
      repository,
      otpHttpClientFactory
    );
    return serviceDirectory.createUpdatersFromManifest(parameters, overrides, manifest);
  }

  public List<GraphUpdater> createUpdatersFromManifest(
    VehicleRentalServiceDirectoryFetcherParameters parameters,
    GbfsNetworkOverrides overrides,
    GBFSManifest manifest
  ) {
    return fetchUpdaterInfoFromDirectoryAndCreateUpdaters(
      buildListOfNetworksFromManifest(parameters, overrides, manifest)
    );
  }

  private static List<GbfsVehicleRentalDataSourceParameters> buildListOfNetworksFromManifest(
    VehicleRentalServiceDirectoryFetcherParameters parameters,
    GbfsNetworkOverrides overrides,
    GBFSManifest manifest
  ) {
    List<GbfsVehicleRentalDataSourceParameters> dataSources = new ArrayList<>();

    for (GBFSDataset dataset : manifest.getData().getDatasets()) {
      String networkName = dataset.getSystemId();
      var gbfsUrl = GbfsManifestLoader.selectBestVersion(dataset);

      if (gbfsUrl.isEmpty()) {
        LOG.warn("No suitable GBFS version found for system {}", networkName);
        continue;
      }

      var config = overrides.forNetwork(networkName);

      if (config.isEmpty()) {
        LOG.warn("Network not configured in OTP: {}", networkName);
        continue;
      }

      var networkParams = config.get();
      dataSources.add(
        new GbfsVehicleRentalDataSourceParameters(
          gbfsUrl.get(),
          parameters.getLanguage(),
          networkParams.allowKeepingVehicleAtDestination(),
          parameters.getHeaders(),
          networkName,
          // Zones are only computed here when this network is in the realtime phase; the
          // permanent phase is handled by the vehicle rental graph builder.
          networkParams.geofencingZones() == GeofencingZonePhase.REALTIME,
          networkParams.requireDropOffInsideBusinessArea(),
          // overloadingAllowed - not part of GBFS, not supported here
          false,
          // rentalPickupType not supported
          RentalPickupType.ALL
        )
      );
    }
    return dataSources;
  }

  private List<GraphUpdater> fetchUpdaterInfoFromDirectoryAndCreateUpdaters(
    List<GbfsVehicleRentalDataSourceParameters> dataSources
  ) {
    List<GraphUpdater> updaters = new ArrayList<>();
    for (var it : dataSources) {
      updaters.add(fetchAndCreateUpdater(it));
    }
    LOG.info("{} updaters fetched", updaters.size());
    return updaters;
  }

  private VehicleRentalUpdater fetchAndCreateUpdater(
    GbfsVehicleRentalDataSourceParameters parameters
  ) {
    LOG.info("Fetched updater info for {} at url {}", parameters.network(), parameters.url());

    VehicleRentalParameters vehicleRentalParameters = new VehicleRentalParameters(
      "vehicle-rental-service-directory:" + parameters.network(),
      DEFAULT_FREQUENCY,
      DEFAULT_STARTUP_RETRY_PERIOD,
      parameters
    );

    var dataSource = VehicleRentalDataSourceFactory.create(
      vehicleRentalParameters.sourceParameters(),
      otpHttpClientFactory
    );
    return new VehicleRentalUpdater(vehicleRentalParameters, dataSource, vertexLinker, repository);
  }
}
