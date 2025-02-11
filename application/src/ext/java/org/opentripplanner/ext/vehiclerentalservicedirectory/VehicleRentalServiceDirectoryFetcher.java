package org.opentripplanner.ext.vehiclerentalservicedirectory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.ext.vehiclerentalservicedirectory.api.VehicleRentalServiceDirectoryFetcherParameters;
import org.opentripplanner.framework.io.OtpHttpClientException;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.framework.json.JsonUtils;
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
 * Fetches GBFS endpoints from the micromobility aggregation service located at
 * https://github.com/entur/lamassu, which is an API for aggregating GBFS endpoints.
 */
public class VehicleRentalServiceDirectoryFetcher {

  private static final Logger LOG = LoggerFactory.getLogger(
    VehicleRentalServiceDirectoryFetcher.class
  );
  private static final Duration DEFAULT_FREQUENCY = Duration.ofSeconds(15);

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
    LOG.info("Fetching list of updaters from {}", parameters.getUrl());

    var sources = listSources(parameters);

    if (sources.isEmpty()) {
      return List.of();
    }

    int maxHttpConnections = sources.size();
    var otpHttpClientFactory = new OtpHttpClientFactory(maxHttpConnections);

    var serviceDirectory = new VehicleRentalServiceDirectoryFetcher(
      vertexLinker,
      repository,
      otpHttpClientFactory
    );
    return serviceDirectory.createUpdatersFromEndpoint(parameters, sources);
  }

  public List<GraphUpdater> createUpdatersFromEndpoint(
    VehicleRentalServiceDirectoryFetcherParameters parameters,
    JsonNode sources
  ) {
    return fetchUpdaterInfoFromDirectoryAndCreateUpdaters(
      buildListOfNetworksFromConfig(parameters, sources)
    );
  }

  private static List<GbfsVehicleRentalDataSourceParameters> buildListOfNetworksFromConfig(
    VehicleRentalServiceDirectoryFetcherParameters parameters,
    JsonNode sources
  ) {
    List<GbfsVehicleRentalDataSourceParameters> dataSources = new ArrayList<>();

    for (JsonNode source : sources) {
      Optional<String> network = JsonUtils.asText(source, parameters.getSourceNetworkName());
      Optional<String> updaterUrl = JsonUtils.asText(source, parameters.getSourceUrlName());

      if (network.isEmpty() || updaterUrl.isEmpty()) {
        LOG.warn(
          "Error reading json from {}. Are json tag names configured properly?",
          parameters.getUrl()
        );
      } else {
        var networkName = network.get();
        var config = parameters.networkParameters(networkName);

        if (config.isPresent()) {
          var networkParams = config.get();
          dataSources.add(
            new GbfsVehicleRentalDataSourceParameters(
              updaterUrl.get(),
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
      parameters
    );

    var dataSource = VehicleRentalDataSourceFactory.create(
      vehicleRentalParameters.sourceParameters(),
      otpHttpClientFactory
    );
    return new VehicleRentalUpdater(vehicleRentalParameters, dataSource, vertexLinker, repository);
  }

  private static JsonNode listSources(VehicleRentalServiceDirectoryFetcherParameters parameters) {
    JsonNode node;
    URI url = parameters.getUrl();
    try {
      var otpHttpClient = new OtpHttpClientFactory().create(LOG);
      node = otpHttpClient.getAndMapAsJsonNode(url, Map.of(), new ObjectMapper());
    } catch (OtpHttpClientException e) {
      LOG.warn("Error fetching list of vehicle rental endpoints from {}", url, e);
      return MissingNode.getInstance();
    }
    if (node == null) {
      LOG.warn("Error reading json from {}. Node is null!", url);
      return MissingNode.getInstance();
    }

    String sourcesName = parameters.getSourcesName();
    JsonNode sources = node.get(sourcesName);
    if (sources == null) {
      LOG.warn(
        "Error reading json from {}. No JSON node for sources name '{}' found.",
        url,
        sourcesName
      );
      return MissingNode.getInstance();
    }
    return sources;
  }
}
