package org.opentripplanner.standalone.config;

import java.net.URI;

/**
 * Configuration for a transit data feed.
 */
public class TransitFeedConfig {

  /**
   * The unique ID for this feed.
   */
  public final String feedId;

  /**
   * URI to data files.
   * <p>
   * Example:
   * {@code "file:///Users/kelvin/otp/netex.zip", "gs://my-bucket/netex.zip"  }
   * <p>
   */
  public final URI source;

  public TransitFeedConfig(NodeAdapter config) {
    this.feedId = config.asText("feedId", null);
    this.source = config.asUri("source");
  }
}
