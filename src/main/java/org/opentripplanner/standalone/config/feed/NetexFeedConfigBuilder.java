package org.opentripplanner.standalone.config.feed;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.net.URI;
import java.util.regex.Pattern;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

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
    netexFeedConfigBuilder.source =
      config.of("source").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asUri();
    netexFeedConfigBuilder.feedId =
      config
        .of("feedId")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asString(null);
    netexFeedConfigBuilder.sharedFilePattern =
      config.of("sharedFilePattern").withDoc(NA, /*TODO DOC*/"TODO").asPattern(null);
    netexFeedConfigBuilder.sharedGroupFilePattern =
      config.of("sharedGroupFilePattern").withDoc(NA, /*TODO DOC*/"TODO").asPattern(null);
    netexFeedConfigBuilder.ignoreFilePattern =
      config.of("ignoreFilePattern").withDoc(NA, /*TODO DOC*/"TODO").asPattern(null);
    netexFeedConfigBuilder.groupFilePattern =
      config.of("groupFilePattern").withDoc(NA, /*TODO DOC*/"TODO").asPattern(null);
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
