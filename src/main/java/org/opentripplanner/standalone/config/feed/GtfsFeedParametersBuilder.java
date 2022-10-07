package org.opentripplanner.standalone.config.feed;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.net.URI;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

/**
 * Configure a GTFS feed.
 */
public class GtfsFeedParametersBuilder {

  private URI source;
  private String feedId;

  public static GtfsFeedParametersBuilder of(NodeAdapter config) {
    GtfsFeedParametersBuilder builder = new GtfsFeedParametersBuilder();
    builder.source =
      config.of("source").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asUri();
    builder.feedId =
      config
        .of("feedId")
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .asString(null);

    return builder;
  }

  public GtfsFeedParametersBuilder withSource(URI source) {
    this.source = source;
    return this;
  }

  public GtfsFeedParameters build() {
    return new GtfsFeedParameters(this);
  }

  public URI getSource() {
    return source;
  }

  public String getFeedId() {
    return feedId;
  }
}
