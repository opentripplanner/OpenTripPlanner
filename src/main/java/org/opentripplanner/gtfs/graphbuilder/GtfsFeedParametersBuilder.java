package org.opentripplanner.gtfs.graphbuilder;

import java.net.URI;

/**
 * Configure a GTFS feed.
 */
public class GtfsFeedParametersBuilder {

  private URI source;
  private String feedId;

  public GtfsFeedParametersBuilder withFeedId(String feedId) {
    this.feedId = feedId;
    return this;
  }

  String feedId() {
    return feedId;
  }

  public GtfsFeedParametersBuilder withSource(URI source) {
    this.source = source;
    return this;
  }

  URI source() {
    return source;
  }

  public GtfsFeedParameters build() {
    return new GtfsFeedParameters(this);
  }
}
