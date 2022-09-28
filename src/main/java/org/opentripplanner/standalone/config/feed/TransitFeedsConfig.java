package org.opentripplanner.standalone.config.feed;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.net.URI;
import java.util.List;
import javax.annotation.Nonnull;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

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
        .filter(feedConfig ->
          FEED_TYPE_GTFS.equalsIgnoreCase(
            feedConfig
              .of("type")
              .withDoc(NA, /*TODO DOC*/"TODO")
              .withExample(/*TODO DOC*/"TODO")
              .asString()
          )
        )
        .map(feedConfig -> GtfsFeedConfigBuilder.of(feedConfig).build())
        .toList();

    netexFeedConfigs =
      feedConfigs
        .stream()
        .filter(feedConfig ->
          FEED_TYPE_NETEX.equalsIgnoreCase(
            feedConfig
              .of("type")
              .withDoc(NA, /*TODO DOC*/"TODO")
              .withExample(/*TODO DOC*/"TODO")
              .asString()
          )
        )
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
