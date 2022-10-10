package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.graph_builder.model.DataSourceConfig;

/**
 * Configuration for a transit data feed.
 */
public class TransitFeedParameters implements DataSourceConfig {

  /**
   * The unique ID for this feed.
   */
  private final String feedId;

  /**
   * URI to data files.
   * <p>
   * Example:
   * {@code "file:///Users/kelvin/otp/netex.zip", "gs://my-bucket/netex.zip"  }
   * <p>
   */
  private final URI source;

  public TransitFeedParameters(URI source, String feedId) {
    this.source = Objects.requireNonNull(source);
    this.feedId = feedId;
  }

  @Override
  public URI source() {
    return source;
  }

  public Optional<String> feedId() {
    return Optional.ofNullable(feedId);
  }
}
