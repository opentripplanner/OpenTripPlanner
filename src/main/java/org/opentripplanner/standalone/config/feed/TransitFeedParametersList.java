package org.opentripplanner.standalone.config.feed;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.net.URI;
import java.util.List;
import javax.annotation.Nonnull;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class TransitFeedParametersList {

  private static final String FEED_TYPE_GTFS = "GTFS";
  private static final String FEED_TYPE_NETEX = "NETEX";

  @Nonnull
  public final List<NetexFeedParameters> netexFeedConfigs;

  @Nonnull
  public final List<GtfsFeedParameters> gtfsFeedConfigs;

  public TransitFeedParametersList(NodeAdapter config) {
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
        .map(feedConfig -> NetexFeedParametersBuilder.of(feedConfig).build())
        .toList();
  }

  public List<URI> gtfsFiles() {
    return gtfsFeedConfigs.stream().map(TransitFeedParameters::source).toList();
  }

  public List<URI> netexFiles() {
    return netexFeedConfigs.stream().map(TransitFeedParameters::source).toList();
  }
}
