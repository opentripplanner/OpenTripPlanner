package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.util.List;
import javax.annotation.Nonnull;
import org.opentripplanner.standalone.config.NodeAdapter;

public class TransitFeedsConfig {

  private static final String FEED_TYPE_GTFS = "GTFS";
  private static final String FEED_TYPE_NETEX = "NETEX";

  @Nonnull
  public final List<NetexFeedConfig> netexFeedConfigs;

  @Nonnull
  public final List<GtfsFeedConfig> gtfsFeedConfigs;

  public TransitFeedsConfig(NodeAdapter config) {
    List<NodeAdapter> feedConfigs = config.asList();

    gtfsFeedConfigs =
      feedConfigs
        .stream()
        .filter(feedConfig -> FEED_TYPE_GTFS.equalsIgnoreCase(feedConfig.asText("type")))
        .map(feedConfig -> GtfsFeedConfigBuilder.of(feedConfig).build())
        .toList();

    netexFeedConfigs =
      feedConfigs
        .stream()
        .filter(feedConfig -> FEED_TYPE_NETEX.equalsIgnoreCase(feedConfig.asText("type")))
        .map(feedConfig -> NetexFeedConfigBuilder.of(feedConfig).build())
        .toList();
  }

  public List<URI> gtfsFiles() {
    return gtfsFeedConfigs.stream().map(TransitFeedConfig::source).toList();
  }

  public List<URI> netexFiles() {
    return netexFeedConfigs.stream().map(TransitFeedConfig::source).toList();
  }
}
