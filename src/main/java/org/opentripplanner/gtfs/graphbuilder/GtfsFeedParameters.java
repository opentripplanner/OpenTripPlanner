package org.opentripplanner.gtfs.graphbuilder;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.graph_builder.model.DataSourceConfig;

/**
 * Configure a GTFS feed.
 * Example: {@code [ {type="gtfs", source: "file:///path/to/otp/norway-gtfs.zip"} ] }
 */
public class GtfsFeedParameters implements DataSourceConfig {

  private final URI source;
  private final String feedId;

  GtfsFeedParameters(GtfsFeedParametersBuilder builder) {
    this.source = Objects.requireNonNull(builder.source());
    this.feedId = builder.feedId();
  }

  /** See {@link org.opentripplanner.standalone.config.buildconfig.TransitFeedConfig}. */
  @Override
  public URI source() {
    return source;
  }

  /** See {@link org.opentripplanner.standalone.config.buildconfig.TransitFeedConfig}. */
  public Optional<String> feedId() {
    return Optional.ofNullable(feedId);
  }
}
