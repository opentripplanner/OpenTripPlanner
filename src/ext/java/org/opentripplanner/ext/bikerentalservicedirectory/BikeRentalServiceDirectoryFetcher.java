package org.opentripplanner.ext.bikerentalservicedirectory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.bike_rental.BikeRentalDataSource;
import org.opentripplanner.updater.bike_rental.BikeRentalUpdater;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches GBFS endpoints from the Bikeservice component located at
 * https://github.com/entur/bikeservice, which is an API for aggregating GBFS endpoints.
 */
public class BikeRentalServiceDirectoryFetcher {

  private static final Logger LOG = LoggerFactory.getLogger(BikeRentalServiceDirectoryFetcher.class);

  private static final int DEFAULT_FREQUENCY_SEC = 15;

  private static final String GBFS_JSON_FILENAME = "gbfs.json";

  public static List<GraphUpdater> createUpdatersFromEndpoint(URI url) {

    LOG.info("Fetching list of updaters from {}", url);

    List<GraphUpdater> updaters = new ArrayList<>();

    try {
      InputStream is = HttpUtils.getData(url);
      JsonNode node = (new ObjectMapper()).readTree(is);

      for (JsonNode operator : node.get("operators")) {
        String network = operator.get("name").asText();
        String updaterUrl = adjustUrlForUpdater(operator.get("url").asText());

        BikeRentalParameters bikeRentalParameters = new BikeRentalParameters(
            "bike-rental-service-directory:" + network,
            updaterUrl,
            DEFAULT_FREQUENCY_SEC,
            new GbfsDataSourceParameters(updaterUrl, network)
        );
        LOG.info("Fetched updater info for {} at url {}", network, updaterUrl);

        GraphUpdater updater = new BikeRentalUpdater(bikeRentalParameters);
        updaters.add(updater);
      }
    }
    catch (java.io.IOException e) {
      LOG.warn("Error fetching list of bike rental endpoints from {}", url, e);
    }

    LOG.info("{} updaters fetched", updaters.size());

    return updaters;
  }

  /**
   * The GBFS standard defines "gbfs.json" as the entrypoint, while
   * {@link BikeRentalDataSource} expects the base url and
   * does not look at "gbfs.json". This method adjusts the URL to what the BikeRentalDataSource
   * expects.
   */
  private static String adjustUrlForUpdater(String url) {
    return url.endsWith(GBFS_JSON_FILENAME)
        ? url.substring(0, url.length() - GBFS_JSON_FILENAME.length())
        : url;
  }
}
