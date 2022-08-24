package org.opentripplanner.standalone.config.feed;

import java.util.Optional;
import java.util.regex.Pattern;
import org.opentripplanner.standalone.config.NetexDefaultsConfig;

/**
 * Configure a NeTEx feed. Overrides default values specified in {@link NetexDefaultsConfig}
 */
public class NetexFeedConfig extends TransitFeedConfig {

  /**
   * Overrides {@link NetexDefaultsConfig#sharedFilePattern}
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

  public Optional<Pattern> getSharedFilePattern() {
    return Optional.ofNullable(sharedFilePattern);
  }

  public Optional<Pattern> getSharedGroupFilePattern() {
    return Optional.ofNullable(sharedGroupFilePattern);
  }

  public Optional<Pattern> getIgnoreFilePattern() {
    return Optional.ofNullable(ignoreFilePattern);
  }

  public Optional<Pattern> getGroupFilePattern() {
    return Optional.ofNullable(groupFilePattern);
  }
}
