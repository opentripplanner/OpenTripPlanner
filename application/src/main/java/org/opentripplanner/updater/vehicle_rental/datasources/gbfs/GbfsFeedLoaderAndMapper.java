package org.opentripplanner.updater.vehicle_rental.datasources.gbfs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.framework.json.JsonUtils;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.updater.spi.UpdaterConstructionException;
import org.opentripplanner.updater.vehicle_rental.datasources.params.GbfsVehicleRentalDataSourceParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for managing the state and loading of complete GBFS datasets, and updating them according
 * to individual feed's TTL rules.
 */
public class GbfsFeedLoaderAndMapper {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsFeedLoaderAndMapper.class);

  private final org.opentripplanner.updater.vehicle_rental.datasources.gbfs.GbfsFeedLoader loader;
  private final org.opentripplanner.updater.vehicle_rental.datasources.gbfs.GbfsFeedMapper mapper;

  public GbfsFeedLoaderAndMapper(
    GbfsVehicleRentalDataSourceParameters params,
    OtpHttpClientFactory otpHttpClientFactory
  ) {
    URI uri;
    try {
      uri = new URI(params.url());
    } catch (URISyntaxException e) {
      throw new UpdaterConstructionException("Invalid url " + params.url());
    }

    var client = otpHttpClientFactory.create(LOG);
    var gbfsNode = client.getAndMapAsJsonNode(uri, Map.of(), new ObjectMapper());
    var gbfsFeedVersion = JsonUtils.asText(gbfsNode, "version").orElse(null);

    switch (gbfsFeedVersion) {
      case "3.0" -> {
        var loaderv30 =
          new org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3.GbfsFeedLoader(
            params.url(),
            params.httpHeaders(),
            client
          );
        loader = loaderv30;
        mapper = new org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v3.GbfsFeedMapper(
          loaderv30,
          params
        );
      }
      case "2.2", "2.3" -> {
        var loaderv23 =
          new org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v2.GbfsFeedLoader(
            params.url(),
            params.httpHeaders(),
            params.language(),
            client
          );
        loader = loaderv23;
        mapper = new org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v2.GbfsFeedMapper(
          loaderv23,
          params
        );
      }
      case null -> {
        var loaderv23 =
          new org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v2.GbfsFeedLoader(
            params.url(),
            params.httpHeaders(),
            params.language(),
            client
          );
        loader = loaderv23;
        mapper = new org.opentripplanner.updater.vehicle_rental.datasources.gbfs.v2.GbfsFeedMapper(
          loaderv23,
          params
        );
      }
      default -> throw new UnsupportedOperationException(
        "Unsupported GBFS version " + gbfsFeedVersion + " for url " + params.url()
      );
    }
  }

  /**
   * Checks if any of the feeds should be updated based on the TTL and fetches. Returns true, if any
   * feeds were updated.
   */
  public boolean update() {
    return loader.update();
  }

  public List<VehicleRentalPlace> getUpdated() {
    return mapper.getUpdates();
  }

  public List<GeofencingZone> getGeofencingZones() {
    return mapper.getGeofencingZones();
  }
}
