package org.opentripplanner.standalone.config;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;

public class TransitFeedsConfig {

  private static final String FEED_TYPE_GTFS = "GTFS";
  private static final String FEED_TYPE_NETEX = "NETEX";

  @Nonnull
  public final List<NetexFeedConfig> netexFiles = new ArrayList<>();

  @Nonnull
  public final List<GtfsFeedConfig> gtfsFiles = new ArrayList<>();

  TransitFeedsConfig(NodeAdapter config) {
    for (NodeAdapter feedConfig : config.asList()) {
      if (FEED_TYPE_GTFS.equalsIgnoreCase(feedConfig.asText("type"))) {
        gtfsFiles.add(new GtfsFeedConfig(feedConfig));
      }
      if (FEED_TYPE_NETEX.equalsIgnoreCase(feedConfig.asText("type"))) {
        netexFiles.add(new NetexFeedConfig(feedConfig));
      }
    }
  }

  public Collection<URI> gtfsFiles() {
    return gtfsFiles.stream().map(gtfsFeedConfig -> gtfsFeedConfig.source).toList();
  }

  public Collection<URI> netexFiles() {
    return netexFiles.stream().map(netexFeedConfig -> netexFeedConfig.source).toList();
  }
}
