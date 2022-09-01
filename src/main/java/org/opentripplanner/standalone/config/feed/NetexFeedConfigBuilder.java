package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.util.regex.Pattern;
import org.opentripplanner.standalone.config.NodeAdapter;

/**
 * Configure a NeTEx feed. Overrides default values specified in {@link NetexDefaultsConfig}
 */
public class NetexFeedConfigBuilder {

  private URI source;

  private String feedId;

  /**
   * Overrides {@link NetexDefaultsConfig#sharedFilePattern}
   */
  private Pattern sharedFilePattern;

  /**
   * Overrides {@link NetexDefaultsConfig#sharedGroupFilePattern}
   */
  private Pattern sharedGroupFilePattern;

  /**
   * Overrides {@link NetexDefaultsConfig#ignoreFilePattern}
   */
  private Pattern ignoreFilePattern;

  /**
   * Overrides {@link NetexDefaultsConfig#groupFilePattern}
   */
  private Pattern groupFilePattern;

  public NetexFeedConfigBuilder withSource(URI source) {
    this.source = source;
    return this;
  }

  public NetexFeedConfigBuilder withFeedId(String feedId) {
    this.feedId = feedId;
    return this;
  }

  public NetexFeedConfigBuilder withSharedFilePattern(Pattern sharedFilePattern) {
    this.sharedFilePattern = sharedFilePattern;
    return this;
  }

  public NetexFeedConfigBuilder withSharedGroupFilePattern(Pattern sharedGroupFilePattern) {
    this.sharedGroupFilePattern = sharedGroupFilePattern;
    return this;
  }

  public NetexFeedConfigBuilder withGroupFilePattern(Pattern groupFilePattern) {
    this.groupFilePattern = groupFilePattern;
    return this;
  }

  public NetexFeedConfigBuilder withIgnoreFilePattern(Pattern ignoreFilePattern) {
    this.ignoreFilePattern = ignoreFilePattern;
    return this;
  }

  public static NetexFeedConfigBuilder of(NodeAdapter config) {
    NetexFeedConfigBuilder netexFeedConfigBuilder = new NetexFeedConfigBuilder();
    netexFeedConfigBuilder.source = config.asUri("source");
    netexFeedConfigBuilder.feedId = config.asText("feedId", null);
    netexFeedConfigBuilder.sharedFilePattern = config.asPattern("sharedFilePattern", null);
    netexFeedConfigBuilder.sharedGroupFilePattern =
      config.asPattern("sharedGroupFilePattern", null);
    netexFeedConfigBuilder.ignoreFilePattern = config.asPattern("ignoreFilePattern", null);
    netexFeedConfigBuilder.groupFilePattern = config.asPattern("groupFilePattern", null);
    return netexFeedConfigBuilder;
  }

  public NetexFeedConfig build() {
    return new NetexFeedConfig(this);
  }

  public URI getSource() {
    return source;
  }

  public String getFeedId() {
    return feedId;
  }

  public Pattern getSharedFilePattern() {
    return sharedFilePattern;
  }

  public Pattern getSharedGroupFilePattern() {
    return sharedGroupFilePattern;
  }

  public Pattern getIgnoreFilePattern() {
    return ignoreFilePattern;
  }

  public Pattern getGroupFilePattern() {
    return groupFilePattern;
  }
}
