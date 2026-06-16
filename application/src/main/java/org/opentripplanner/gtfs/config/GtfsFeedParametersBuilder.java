package org.opentripplanner.gtfs.config;

import java.net.URI;
import javax.annotation.Nullable;

public class GtfsFeedParametersBuilder extends GtfsDefaultParametersBuilder {

  @Nullable
  private String feedId;

  private URI source;

  private boolean ignoreHttps;

  private boolean ignoreZipExtension;

  public GtfsFeedParametersBuilder(GtfsDefaultParameters original) {
    super(original);
  }

  public GtfsFeedParametersBuilder withFeedId(@Nullable String feedId) {
    this.feedId = feedId;
    return this;
  }

  @Nullable
  String feedId() {
    return feedId;
  }

  public GtfsFeedParametersBuilder withSource(URI source) {
    this.source = source;
    return this;
  }

  URI source() {
    return source;
  }

  public GtfsFeedParametersBuilder withIgnoreHttps(boolean ignoreHttps) {
    this.ignoreHttps = ignoreHttps;
    return this;
  }

  boolean ignoreHttps() {
    return ignoreHttps;
  }

  public GtfsFeedParametersBuilder withIgnoreZipExtension(boolean ignoreZipExtension) {
    this.ignoreZipExtension = ignoreZipExtension;
    return this;
  }

  boolean ignoreZipExtension() {
    return ignoreZipExtension;
  }

  @Override
  public GtfsFeedParameters build() {
    return new GtfsFeedParameters(
      feedId,
      source,
      ignoreHttps,
      ignoreZipExtension,
      stationTransferPreference(),
      discardMinTransferTimes(),
      blockBasedInterlining(),
      maxInterlineDistance()
    );
  }
}
