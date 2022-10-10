package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import org.opentripplanner.graph_builder.model.DataSourceConfig;

/**
 * Configure a NeTEx feed. Overrides default values specified in {@link NetexDefaultParameters}
 * Example: {@code [ {type="netex", source: "file:///path/to/otp/norway-netex.zip"} ] }
 */
public class NetexFeedParameters implements DataSourceConfig {

  private final URI source;
  private final String feedId;
  private final Pattern sharedFilePattern;

  /**
   * Overrides {@link NetexDefaultParameters#sharedGroupFilePattern}
   */
  private final Pattern sharedGroupFilePattern;

  /**
   * Overrides {@link NetexDefaultParameters#ignoreFilePattern}
   */
  private final Pattern ignoreFilePattern;

  /**
   * Overrides {@link NetexDefaultParameters#groupFilePattern}
   */
  private final Pattern groupFilePattern;

  NetexFeedParameters(NetexFeedParametersBuilder builder) {
    this.source = Objects.requireNonNull(builder.source());
    this.feedId = builder.feedId();
    this.sharedFilePattern = builder.sharedFilePattern();
    this.sharedGroupFilePattern = builder.sharedGroupFilePattern();
    this.ignoreFilePattern = builder.ignoreFilePattern();
    this.groupFilePattern = builder.groupFilePattern();
  }

  @Override
  public URI source() {
    return source;
  }

  /**
   * The unique ID for this feed.
   */
  public Optional<String> feedId() {
    return Optional.ofNullable(feedId);
  }

  /**
   *
   * @return an optional custom shared file pattern
   * that overrides {@link NetexDefaultParameters#sharedFilePattern}
   */
  public Optional<Pattern> sharedFilePattern() {
    return Optional.ofNullable(sharedFilePattern);
  }

  /**
   *
   * @return an optional custom shared group file pattern
   * that overrides {@link NetexDefaultParameters#sharedGroupFilePattern}
   */
  public Optional<Pattern> sharedGroupFilePattern() {
    return Optional.ofNullable(sharedGroupFilePattern);
  }

  /**
   *
   * @return an optional custom ignored file pattern
   * that overrides {@link NetexDefaultParameters#ignoreFilePattern}
   */
  public Optional<Pattern> ignoreFilePattern() {
    return Optional.ofNullable(ignoreFilePattern);
  }

  /**
   *
   * @return an optional custom group file pattern
   * that overrides {@link NetexDefaultParameters#groupFilePattern}
   */
  public Optional<Pattern> groupFilePattern() {
    return Optional.ofNullable(groupFilePattern);
  }
}
