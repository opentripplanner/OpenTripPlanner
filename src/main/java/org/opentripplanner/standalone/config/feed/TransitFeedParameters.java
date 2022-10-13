package org.opentripplanner.standalone.config.feed;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import org.opentripplanner.graph_builder.model.DataSourceConfig;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

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

  public TransitFeedParameters(NodeAdapter config) {
    this(
      config.of("source").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asUri(),
      config
        .of("feedId")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asString(null)
    );
  }

  @Override
  public URI source() {
    return source;
  }

  public Optional<String> feedId() {
    return Optional.ofNullable(feedId);
  }
}
