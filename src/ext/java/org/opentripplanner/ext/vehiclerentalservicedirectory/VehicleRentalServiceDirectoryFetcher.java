package org.opentripplanner.ext.vehiclerentalservicedirectory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.ext.vehiclerentalservicedirectory.api.VehicleRentalServiceDirectoryFetcherParameters;
import org.opentripplanner.framework.io.HttpUtils;
import org.opentripplanner.routing.linking.VertexLinker;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalService;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.vehicle_rental.VehicleRentalUpdater;
import org.opentripplanner.updater.vehicle_rental.datasources.VehicleRentalDataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches GBFS endpoints from the micromobility aggregation service located at
 * https://github.com/entur/lahmu, which is an API for aggregating GBFS endpoints.
 */
public class VehicleRentalServiceDirectoryFetcher {

  private static final Logger LOG = LoggerFactory.getLogger(
    VehicleRentalServiceDirectoryFetcher.class
  );

  private static final int DEFAULT_FREQUENCY_SEC = 15;

  public static List<GraphUpdater> createUpdatersFromEndpoint(
    VehicleRentalServiceDirectoryFetcherParameters parameters,
    VertexLinker vertexLinker,
    VehicleRentalService vehicleRentalStationService
  ) {
    LOG.info("Fetching list of updaters from {}", parameters.getUrl());

    List<GraphUpdater> updaters = new ArrayList<>();

    try {
      InputStream is = HttpUtils.getData(parameters.getUrl());
      JsonNode node = (new ObjectMapper()).readTree(is);

      JsonNode sources = node.get(parameters.getSourcesName());

      if (sources == null) {
        LOG.warn(
          "Error reading json from {}. Are json tag names configured properly?",
          parameters.getUrl()
        );
        return updaters;
      }

      for (JsonNode source : sources) {
        JsonNode network = source.get(parameters.getSourceNetworkName());
        JsonNode updaterUrl = source.get(parameters.getSourceUrlName());

        if (network == null || updaterUrl == null) {
          LOG.warn(
            "Error reading json from {}. Are json tag names configured properly?",
            parameters.getUrl()
          );
          return updaters;
        }

        VehicleRentalParameters vehicleRentalParameters = new VehicleRentalParameters(
          "vehicle-rental-service-directory:" + network,
          DEFAULT_FREQUENCY_SEC,
          new GbfsDataSourceParameters(
            updaterUrl.asText(),
            parameters.getLanguage(),
            parameters.getHeaders(),
            parameters.getSourceNetworkName()
          )
        );
        LOG.info("Fetched updater info for {} at url {}", network, updaterUrl);

        var dataSource = VehicleRentalDataSourceFactory.create(
          vehicleRentalParameters.sourceParameters()
        );
        GraphUpdater updater = new VehicleRentalUpdater(
          vehicleRentalParameters,
          dataSource,
          vertexLinker,
          vehicleRentalStationService
        );
        updaters.add(updater);
      }
    } catch (java.io.IOException e) {
      LOG.warn("Error fetching list of vehicle rental endpoints from {}", parameters.getUrl(), e);
    }

    LOG.info("{} updaters fetched", updaters.size());

    return updaters;
  }
}
