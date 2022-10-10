package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * Configure a NeTEx feed. Overrides default values specified in {@link NetexDefaultParameters}
 */
public class NetexFeedParametersBuilder {

  private URI source;

  private String feedId;

  private Pattern sharedFilePattern;

  private Pattern sharedGroupFilePattern;

  private Pattern ignoreFilePattern;

  private Pattern groupFilePattern;

  public String feedId() {
    return feedId;
  }

  public NetexFeedParametersBuilder withFeedId(String feedId) {
    this.feedId = feedId;
    return this;
  }

  public URI source() {
    return source;
  }

  public NetexFeedParametersBuilder withSource(URI source) {
    this.source = source;
    return this;
  }

  public Pattern sharedFilePattern() {
    return sharedFilePattern;
  }

  public NetexFeedParametersBuilder withSharedFilePattern(Pattern sharedFilePattern) {
    this.sharedFilePattern = sharedFilePattern;
    return this;
  }

  public Pattern sharedGroupFilePattern() {
    return sharedGroupFilePattern;
  }

  public NetexFeedParametersBuilder withSharedGroupFilePattern(Pattern sharedGroupFilePattern) {
    this.sharedGroupFilePattern = sharedGroupFilePattern;
    return this;
  }

  public Pattern groupFilePattern() {
    return groupFilePattern;
  }

  public NetexFeedParametersBuilder withGroupFilePattern(Pattern groupFilePattern) {
    this.groupFilePattern = groupFilePattern;
    return this;
  }

  public Pattern ignoreFilePattern() {
    return ignoreFilePattern;
  }

  public NetexFeedParametersBuilder withIgnoreFilePattern(Pattern ignoreFilePattern) {
    this.ignoreFilePattern = ignoreFilePattern;
    return this;
  }

  public NetexFeedParameters build() {
    return new NetexFeedParameters(this);
  }
}
