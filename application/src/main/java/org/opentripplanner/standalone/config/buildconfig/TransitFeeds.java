package org.opentripplanner.standalone.config.buildconfig;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.opentripplanner.datastore.api.DataSourceOptions;
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

  public Map<URI, DataSourceOptions> gtfsSourceOptions() {
    return gtfsFeeds
      .stream()
      .filter(feed -> feed.ignoreHttps() || feed.ignoreZipExtension())
      .collect(
        Collectors.toMap(
          DataSourceConfig::source,
          feed -> new DataSourceOptions(feed.ignoreHttps(), feed.ignoreZipExtension())
        )
      );
  }
}
