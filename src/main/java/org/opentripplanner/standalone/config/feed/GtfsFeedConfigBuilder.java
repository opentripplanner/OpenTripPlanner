package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import org.opentripplanner.standalone.config.NodeAdapter;

/**
 * Configure a GTFS feed.
 */
public class GtfsFeedConfigBuilder {

  private URI source;
  private String feedId;

  public static GtfsFeedConfigBuilder of(NodeAdapter config) {
    GtfsFeedConfigBuilder gtfsFeedConfigBuilder = new GtfsFeedConfigBuilder();
    gtfsFeedConfigBuilder.source = config.asUri("source");
    gtfsFeedConfigBuilder.feedId = config.asText("feedId", null);

    return gtfsFeedConfigBuilder;
  }

  public GtfsFeedConfigBuilder withSource(URI source) {
    this.source = source;
    return this;
  }

  public GtfsFeedConfig build() {
    return new GtfsFeedConfig(this);
  }

  public URI getSource() {
    return source;
  }

  public String getFeedId() {
    return feedId;
  }
}
