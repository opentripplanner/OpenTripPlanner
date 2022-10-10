package org.opentripplanner.standalone.config.feed;

/**
 * Configure a GTFS feed.
 * Example: {@code [ {type="gtfs", source: "file:///path/to/otp/norway-gtfs.zip"} ] }
 */
public class GtfsFeedParameters extends TransitFeedParameters {

  GtfsFeedParameters(GtfsFeedParametersBuilder builder) {
    super(builder.source(), builder.feedId());
  }
}
