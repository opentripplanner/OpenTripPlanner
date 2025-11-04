package org.opentripplanner.ext.vehiclerentalservicedirectory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.mobilitydata.gbfs.v3_0.manifest.GBFSDataset;
import org.mobilitydata.gbfs.v3_0.manifest.GBFSManifest;
import org.mobilitydata.gbfs.v3_0.manifest.GBFSVersion;
import org.opentripplanner.ext.vehiclerentalservicedirectory.api.VehicleRentalServiceDirectoryFetcherParameters;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.service.vehiclerental.VehicleRentalRepository;
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
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
    .registerModule(new JavaTimeModule())
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
    VertexLinker vertexLinker,
    VehicleRentalRepository repository
  ) {
    LOG.info("Fetching GBFS v3 manifest from {}", parameters.getUrl());

    var manifest = loadManifest(parameters);

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
    return serviceDirectory.createUpdatersFromManifest(parameters, manifest);
  }

  public List<GraphUpdater> createUpdatersFromManifest(
    VehicleRentalServiceDirectoryFetcherParameters parameters,
    GBFSManifest manifest
  ) {
    return fetchUpdaterInfoFromDirectoryAndCreateUpdaters(
      buildListOfNetworksFromManifest(parameters, manifest)
    );
  }

  private static List<GbfsVehicleRentalDataSourceParameters> buildListOfNetworksFromManifest(
    VehicleRentalServiceDirectoryFetcherParameters parameters,
    GBFSManifest manifest
  ) {
    List<GbfsVehicleRentalDataSourceParameters> dataSources = new ArrayList<>();

    for (GBFSDataset dataset : manifest.getData().getDatasets()) {
      String networkName = dataset.getSystemId();
      Optional<String> gbfsUrl = selectBestVersion(dataset);

      if (gbfsUrl.isEmpty()) {
        LOG.warn("No suitable GBFS version found for system {}", networkName);
        continue;
      }

      var config = parameters.networkParameters(networkName);

      if (config.isPresent()) {
        var networkParams = config.get();
        dataSources.add(
          new GbfsVehicleRentalDataSourceParameters(
            gbfsUrl.get(),
            parameters.getLanguage(),
            networkParams.allowKeepingAtDestination(),
            parameters.getHeaders(),
            networkName,
            networkParams.geofencingZones(),
            // overloadingAllowed - not part of GBFS, not supported here
            false,
            // rentalPickupType not supported
            RentalPickupType.ALL
          )
        );
      } else {
        LOG.warn("Network not configured in OTP: {}", networkName);
      }
    }
    return dataSources;
  }

  /**
   * Selects the best (newest) GBFS version from the available versions for a dataset.
   * Prefers v3.0 over v2.x versions.
   */
  private static Optional<String> selectBestVersion(GBFSDataset dataset) {
    if (dataset.getVersions() == null || dataset.getVersions().isEmpty()) {
      return Optional.empty();
    }

    // Sort versions by version number (descending) to prefer newer versions
    return dataset
      .getVersions()
      .stream()
      .sorted(Comparator.comparing(GBFSVersion::getVersion).reversed())
      .map(GBFSVersion::getUrl)
      .findFirst();
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
      parameters
    );

    var dataSource = VehicleRentalDataSourceFactory.create(
      vehicleRentalParameters.sourceParameters(),
      otpHttpClientFactory
    );
    return new VehicleRentalUpdater(vehicleRentalParameters, dataSource, vertexLinker, repository);
  }

  private static GBFSManifest loadManifest(
    VehicleRentalServiceDirectoryFetcherParameters parameters
  ) {
    URI url = parameters.getUrl();

    try {
      String manifestContent;

      // Check if URL is a file path
      if ("file".equals(url.getScheme())) {
        Path filePath = Path.of(url.getPath());
        manifestContent = Files.readString(filePath);
        LOG.info("Loaded GBFS manifest from file: {}", filePath);
      } else {
        // Load from remote URL
        var otpHttpClient = new OtpHttpClientFactory().create(LOG);
        var jsonNode = otpHttpClient.getAndMapAsJsonNode(url, Map.of(), OBJECT_MAPPER);
        manifestContent = OBJECT_MAPPER.writeValueAsString(jsonNode);
        LOG.info("Loaded GBFS manifest from URL: {}", url);
      }

      return OBJECT_MAPPER.readValue(manifestContent, GBFSManifest.class);
    } catch (OtpHttpClientException | IOException e) {
      LOG.error("Error loading GBFS manifest from {}", url, e);
      return null;
    }
  }
}
