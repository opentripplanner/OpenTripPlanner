package org.opentripplanner.standalone.config.feed;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.net.URI;
import java.util.regex.Pattern;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

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

  public NetexFeedParametersBuilder withSource(URI source) {
    this.source = source;
    return this;
  }

  public NetexFeedParametersBuilder withFeedId(String feedId) {
    this.feedId = feedId;
    return this;
  }

  public NetexFeedParametersBuilder withSharedFilePattern(Pattern sharedFilePattern) {
    this.sharedFilePattern = sharedFilePattern;
    return this;
  }

  public NetexFeedParametersBuilder withSharedGroupFilePattern(Pattern sharedGroupFilePattern) {
    this.sharedGroupFilePattern = sharedGroupFilePattern;
    return this;
  }

  public NetexFeedParametersBuilder withGroupFilePattern(Pattern groupFilePattern) {
    this.groupFilePattern = groupFilePattern;
    return this;
  }

  public NetexFeedParametersBuilder withIgnoreFilePattern(Pattern ignoreFilePattern) {
    this.ignoreFilePattern = ignoreFilePattern;
    return this;
  }

  public static NetexFeedParametersBuilder of(NodeAdapter config) {
    NetexFeedParametersBuilder builder = new NetexFeedParametersBuilder();
    builder.source =
      config.of("source").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asUri();
    builder.feedId =
      config
        .of("feedId")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asString(null);
    builder.sharedFilePattern =
      config.of("sharedFilePattern").withDoc(NA, /*TODO DOC*/"TODO").asPattern(null);
    builder.sharedGroupFilePattern =
      config.of("sharedGroupFilePattern").withDoc(NA, /*TODO DOC*/"TODO").asPattern(null);
    builder.ignoreFilePattern =
      config.of("ignoreFilePattern").withDoc(NA, /*TODO DOC*/"TODO").asPattern(null);
    builder.groupFilePattern =
      config.of("groupFilePattern").withDoc(NA, /*TODO DOC*/"TODO").asPattern(null);
    return builder;
  }

  public NetexFeedParameters build() {
    return new NetexFeedParameters(this);
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
