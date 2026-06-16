package org.opentripplanner.standalone.config.buildconfig;

import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

  public Set<URI> ignoreHttpsSources() {
    return gtfsFeeds
      .stream()
      .filter(GtfsFeedParameters::ignoreHttps)
      .map(DataSourceConfig::source)
      .collect(Collectors.toSet());
  }

  public Set<URI> ignoreZipExtensionSources() {
    return gtfsFeeds
      .stream()
      .filter(GtfsFeedParameters::ignoreZipExtension)
      .map(DataSourceConfig::source)
      .collect(Collectors.toSet());
  }
}
