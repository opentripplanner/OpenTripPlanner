package org.opentripplanner.ext.gbfsgeofencing.parameters;

import java.util.List;

/**
 * Configuration parameters for build-time GBFS geofencing zone loading.
 */
public record GbfsGeofencingParameters(List<GbfsGeofencingFeedParameters> feeds) {
  public GbfsGeofencingParameters {
    if (feeds == null) {
      feeds = List.of();
    }
  }

  public boolean hasFeeds() {
    return !feeds.isEmpty();
  }
}
