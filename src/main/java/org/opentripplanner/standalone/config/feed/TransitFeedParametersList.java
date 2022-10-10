package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.util.List;
import javax.annotation.Nonnull;

public class TransitFeedParametersList {

  @Nonnull
  public final List<NetexFeedParameters> netexFeeds;

  @Nonnull
  public final List<GtfsFeedParameters> gtfsFeeds;

  public TransitFeedParametersList(
    List<GtfsFeedParameters> gtfsFeeds,
    List<NetexFeedParameters> netexFeeds
  ) {
    this.netexFeeds = netexFeeds;
    this.gtfsFeeds = gtfsFeeds;
  }

  public List<URI> gtfsFiles() {
    return gtfsFeeds.stream().map(TransitFeedParameters::source).toList();
  }

  public List<URI> netexFiles() {
    return netexFeeds.stream().map(TransitFeedParameters::source).toList();
  }
}
