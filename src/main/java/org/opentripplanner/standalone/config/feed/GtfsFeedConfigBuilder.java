package org.opentripplanner.standalone.config.feed;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.net.URI;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * Configure a GTFS feed.
 */
public class GtfsFeedConfigBuilder {

  private URI source;
  private String feedId;

  public static GtfsFeedConfigBuilder of(NodeAdapter config) {
    GtfsFeedConfigBuilder gtfsFeedConfigBuilder = new GtfsFeedConfigBuilder();
    gtfsFeedConfigBuilder.source =
      config.of("source").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asUri();
    gtfsFeedConfigBuilder.feedId =
      config
        .of("feedId")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asString(null);

    return gtfsFeedConfigBuilder;
  }

  public GtfsFeedConfigBuilder withSource(URI source) {
    this.source = source;
    return this;
  }

  public GtfsFeedConfig build() {
    return new GtfsFeedConfig(this);
  }

  public URI getSource() {
    return source;
  }

  public String getFeedId() {
    return feedId;
  }
}
