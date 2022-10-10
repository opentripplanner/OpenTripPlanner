package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.util.List;
import javax.annotation.Nonnull;
import org.opentripplanner.graph_builder.model.DataSourceConfig;

public class TransitFeeds {

  @Nonnull
  public final List<NetexFeedParameters> netexFeeds;

  @Nonnull
  public final List<GtfsFeedParameters> gtfsFeeds;

  public TransitFeeds(
    List<GtfsFeedParameters> gtfsFeeds,
    List<NetexFeedParameters> netexFeeds
  ) {
    this.netexFeeds = netexFeeds;
    this.gtfsFeeds = gtfsFeeds;
  }

  public List<URI> gtfsFiles() {
    return gtfsFeeds.stream().map(DataSourceConfig::source).toList();
  }

  public List<URI> netexFiles() {
    return netexFeeds.stream().map(DataSourceConfig::source).toList();
  }
}
