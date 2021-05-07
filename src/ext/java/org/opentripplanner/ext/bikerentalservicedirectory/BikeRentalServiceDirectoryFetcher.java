package org.opentripplanner.ext.bikerentalservicedirectory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.ext.bikerentalservicedirectory.api.BikeRentalServiceDirectoryFetcherParameters;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.bike_rental.BikeRentalUpdater;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fetches GBFS endpoints from the Bikeservice component located at
 * https://github.com/entur/bikeservice, which is an API for aggregating GBFS endpoints.
 */
public class BikeRentalServiceDirectoryFetcher {

  private static final Logger LOG = LoggerFactory.getLogger(BikeRentalServiceDirectoryFetcher.class);

  private static final int DEFAULT_FREQUENCY_SEC = 15;

  private static final String GBFS_JSON_FILENAME = "gbfs.json";

  public static List<GraphUpdater> createUpdatersFromEndpoint(
      BikeRentalServiceDirectoryFetcherParameters parameters
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

        BikeRentalParameters bikeRentalParameters = new BikeRentalParameters(
            "bike-rental-service-directory:" + network,
            DEFAULT_FREQUENCY_SEC,
            new GbfsDataSourceParameters(updaterUrl.asText(), network.asText())
        );
        LOG.info("Fetched updater info for {} at url {}", network, updaterUrl);

        GraphUpdater updater = new BikeRentalUpdater(bikeRentalParameters);
        updaters.add(updater);
      }
    }
    catch (java.io.IOException e) {
      LOG.warn("Error fetching list of bike rental endpoints from {}", parameters.getUrl(), e);
    }

    LOG.info("{} updaters fetched", updaters.size());

    return updaters;
  }
}
