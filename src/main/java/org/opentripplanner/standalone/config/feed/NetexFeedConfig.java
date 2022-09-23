package org.opentripplanner.standalone.config.feed;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Configure a NeTEx feed. Overrides default values specified in {@link NetexDefaultsConfig}
 * Example: {@code [ {type="netex", source: "file:///path/to/otp/norway-netex.zip"} ] }
 */
public class NetexFeedConfig extends TransitFeedConfig {

  /**
   *
   */
  private final Pattern sharedFilePattern;

  /**
   * Overrides {@link NetexDefaultsConfig#sharedGroupFilePattern}
   */
  private final Pattern sharedGroupFilePattern;

  /**
   * Overrides {@link NetexDefaultsConfig#ignoreFilePattern}
   */
  private final Pattern ignoreFilePattern;

  /**
   * Overrides {@link NetexDefaultsConfig#groupFilePattern}
   */
  private final Pattern groupFilePattern;

  NetexFeedConfig(NetexFeedConfigBuilder netexFeedConfigBuilder) {
    super(netexFeedConfigBuilder.getSource(), netexFeedConfigBuilder.getFeedId());
    this.sharedFilePattern = netexFeedConfigBuilder.getSharedFilePattern();
    this.sharedGroupFilePattern = netexFeedConfigBuilder.getSharedGroupFilePattern();
    this.ignoreFilePattern = netexFeedConfigBuilder.getIgnoreFilePattern();
    this.groupFilePattern = netexFeedConfigBuilder.getGroupFilePattern();
  }

  /**
   *
   * @return an optional custom shared file pattern
   * that overrides {@link NetexDefaultsConfig#sharedFilePattern}
   */
  public Optional<Pattern> sharedFilePattern() {
    return Optional.ofNullable(sharedFilePattern);
  }

  /**
   *
   * @return an optional custom shared group file pattern
   * that overrides {@link NetexDefaultsConfig#sharedGroupFilePattern}
   */
  public Optional<Pattern> sharedGroupFilePattern() {
    return Optional.ofNullable(sharedGroupFilePattern);
  }

  /**
   *
   * @return an optional custom ignored file pattern
   * that overrides {@link NetexDefaultsConfig#ignoreFilePattern}
   */
  public Optional<Pattern> ignoreFilePattern() {
    return Optional.ofNullable(ignoreFilePattern);
  }

  /**
   *
   * @return an optional custom group file pattern
   * that overrides {@link NetexDefaultsConfig#groupFilePattern}
   */
  public Optional<Pattern> groupFilePattern() {
    return Optional.ofNullable(groupFilePattern);
  }
}
