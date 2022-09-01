package org.opentripplanner.standalone.config.feed;

/**
 * Configure a GTFS feed.
 * Example: {@code [ {type="gtfs", source: "file:///path/to/otp/norway-gtfs.zip"} ] }
 */
public class GtfsFeedConfig extends TransitFeedConfig {

  GtfsFeedConfig(GtfsFeedConfigBuilder gtfsFeedConfigBuilder) {
    super(gtfsFeedConfigBuilder.getSource(), gtfsFeedConfigBuilder.getFeedId());
  }
}
