package org.opentripplanner.standalone.config.buildconfig;

import java.net.URI;
import java.util.List;
import org.opentripplanner.graph_builder.model.DataSourceConfig;
import org.opentripplanner.gtfs.config.GtfsFeedParameters;
import org.opentripplanner.netex.config.NetexFeedParameters;

public record TransitFeeds(
  List<GtfsFeedParameters> gtfsFeeds,
  List<NetexFeedParameters> netexFeeds
) {
  public TransitFeeds(List<GtfsFeedParameters> gtfsFeeds, List<NetexFeedParameters> netexFeeds) {
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
