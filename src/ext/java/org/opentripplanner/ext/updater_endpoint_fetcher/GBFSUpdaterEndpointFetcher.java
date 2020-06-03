package org.opentripplanner.ext.updater_endpoint_fetcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.opentripplanner.updater.GraphUpdater;
import org.opentripplanner.updater.bike_rental.BikeRentalUpdater;
import org.opentripplanner.util.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches GBFS endpoints from the Bikeservice component located at
 * https://github.com/entur/bikeservice
 */
public class GBFSUpdaterEndpointFetcher {

  private static final Logger LOG = LoggerFactory.getLogger(GBFSUpdaterEndpointFetcher.class);

  private static final int DEFAULT_FREQUENCY_SEC = 15;

  public static List<GraphUpdater> createUpdatersFromEndpoint(String url) {

    LOG.info("Fetching list of updaters from {}", url);

    List<GraphUpdater> updaters = new ArrayList<>();

    try {
      InputStream is = HttpUtils.getData(url);
      JsonNode node = (new ObjectMapper()).readTree(is);
      for (JsonNode operator : node.get("operators")) {
        String network = operator.get("name").asText();
        String updaterUrl = adjustUrlForUpdater(operator.get("url").asText());

        GbfsDataSource dataSource = new GbfsDataSource(updaterUrl, network);
        GbfsUpdaterSourceConfig gbfsUpdaterSourceConfig = new GbfsUpdaterSourceConfig(dataSource);
        BikeRentalParameters bikeRentalParameters = new BikeRentalParameters(
            gbfsUpdaterSourceConfig,
            updaterUrl,
            DEFAULT_FREQUENCY_SEC,
            network
        );
        LOG.info("Fetched updater info for {} at url {}", network, updaterUrl);

        GraphUpdater updater = new BikeRentalUpdater(bikeRentalParameters);
        updaters.add(updater);
      }
    }
    catch (java.io.IOException e) {
      LOG.warn(
          "Error fetching list of bike rental endpoints from {}", url);
    }

    LOG.info("{} updaters fetched", updaters.size());

    return updaters;
  }

  /**
   * The GBFS standard defines "gbfs.json" as the entrypoint, while
   * {@link org.opentripplanner.updater.bike_rental.BikeRentalDataSource} expects the base url and
   * does not look at "gbfs.json". This method adjusts the URL to what the BikeRentalDataSource
   * expects.
   */
  private static String adjustUrlForUpdater(String url) {
    return url.endsWith("gbfs.json")
        ? StringUtils.substring(url, 0, url.length() - 9)
        : url;
  }
}
