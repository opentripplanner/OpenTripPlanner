package org.opentripplanner.standalone.config.feed;

/**
 * Configure a GTFS feed.
 */
public class GtfsFeedConfig extends TransitFeedConfig {

  GtfsFeedConfig(GtfsFeedConfigBuilder gtfsFeedConfigBuilder) {
    super(gtfsFeedConfigBuilder.getSource(), gtfsFeedConfigBuilder.getFeedId());
  }
}
