package org.opentripplanner.gbfs;

import java.util.List;
import org.opentripplanner.framework.io.OtpHttpClient;
import org.opentripplanner.framework.io.OtpHttpClientFactory;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for managing the state and loading of complete GBFS datasets, and updating them according
 * to individual feed's TTL rules.
 */
public class GbfsFeedLoaderAndMapper {

  private static final Logger LOG = LoggerFactory.getLogger(GbfsFeedLoaderAndMapper.class);

  private final GbfsFeedLoader loader;
  private final GbfsFeedMapper mapper;

  private GbfsFeedLoaderAndMapper(GbfsFeedLoader loader, GbfsFeedMapper mapper) {
    this.loader = loader;
    this.mapper = mapper;
  }

  public static GbfsFeedLoaderAndMapper create(
    GbfsDataSourceParameters params,
    OtpHttpClientFactory otpHttpClientFactory
  ) {
    var client = otpHttpClientFactory.create(LOG);
    // Fetched once and handed over to the version-specific loader, since some servers throttle
    // repeated fetches of the same file.
    var autoConfiguration = GbfsAutoConfiguration.fetch(params.url(), params.httpHeaders(), client);
    var gbfsFeedVersion = autoConfiguration.version().orElse(null);

    return switch (gbfsFeedVersion) {
      case "3.0" -> {
        var loader = org.opentripplanner.gbfs.v3.GbfsFeedLoader.create(
          autoConfiguration,
          params.httpHeaders(),
          client
        );
        yield new GbfsFeedLoaderAndMapper(
          loader,
          new org.opentripplanner.gbfs.v3.GbfsFeedMapper(loader, params)
        );
      }
      case "1.1", "2.0", "2.1", "2.2", "2.3" -> {
        if (gbfsFeedVersion.startsWith("1")) {
          LOG.warn(
            "GBFS feed {} is of deprecated version {}. Support for this version will be removed soon.",
            params.url(),
            gbfsFeedVersion
          );
        }
        yield createV23(autoConfiguration, params, client);
      }
      case null -> createV23(autoConfiguration, params, client);
      default -> throw new UnsupportedOperationException(
        "Unsupported GBFS version " + gbfsFeedVersion + " for url " + params.url()
      );
    };
  }

  private static GbfsFeedLoaderAndMapper createV23(
    GbfsAutoConfiguration autoConfiguration,
    GbfsDataSourceParameters params,
    OtpHttpClient client
  ) {
    var loader = org.opentripplanner.gbfs.v2.GbfsFeedLoader.create(
      autoConfiguration,
      params.httpHeaders(),
      params.language(),
      client
    );
    return new GbfsFeedLoaderAndMapper(
      loader,
      new org.opentripplanner.gbfs.v2.GbfsFeedMapper(loader, params)
    );
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
