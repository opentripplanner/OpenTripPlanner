package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.util.Objects;
import org.opentripplanner.standalone.config.NodeAdapter;

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
    this(config.asUri("source"), config.asText("feedId", null));
  }

  public TransitFeedConfig(URI source, String feedId) {
    this.source = Objects.requireNonNull(source);
    this.feedId = feedId;
  }
}
