package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.opentripplanner.standalone.config.NodeAdapter;

public class TransitFeedsConfig {

  private static final String FEED_TYPE_GTFS = "GTFS";
  private static final String FEED_TYPE_NETEX = "NETEX";

  @Nonnull
  public final List<NetexFeedConfig> netexFeedConfigs = new ArrayList<>();

  @Nonnull
  public final List<GtfsFeedConfig> gtfsFeedConfigs = new ArrayList<>();

  public TransitFeedsConfig(NodeAdapter config) {
    for (NodeAdapter feedConfig : config.asList()) {
      if (FEED_TYPE_GTFS.equalsIgnoreCase(feedConfig.asText("type"))) {
        gtfsFeedConfigs.add(GtfsFeedConfigBuilder.of(feedConfig).build());
      }
      if (FEED_TYPE_NETEX.equalsIgnoreCase(feedConfig.asText("type"))) {
        netexFeedConfigs.add(NetexFeedConfigBuilder.of(feedConfig).build());
      }
    }
  }

  public Collection<URI> gtfsFiles() {
    return gtfsFeedConfigs.stream().map(gtfsFeedConfig -> gtfsFeedConfig.source).toList();
  }

  public Collection<URI> netexFiles() {
    return netexFeedConfigs.stream().map(netexFeedConfig -> netexFeedConfig.source).toList();
  }
}
