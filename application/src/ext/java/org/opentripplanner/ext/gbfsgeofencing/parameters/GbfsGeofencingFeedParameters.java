package org.opentripplanner.ext.gbfsgeofencing.parameters;

import javax.annotation.Nullable;
import org.opentripplanner.updater.spi.HttpHeaders;

/**
 * Parameters for a single GBFS geofencing feed.
 */
public record GbfsGeofencingFeedParameters(
  String url,
  @Nullable String network,
  HttpHeaders httpHeaders
) {
  public GbfsGeofencingFeedParameters {
    if (url == null || url.isBlank()) {
      throw new IllegalArgumentException("GBFS feed URL is required");
    }
    if (httpHeaders == null) {
      httpHeaders = HttpHeaders.empty();
    }
  }
}
