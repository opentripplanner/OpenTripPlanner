package org.opentripplanner.gtfs.config;

import java.net.URI;
import javax.annotation.Nullable;

public class GtfsFeedParametersBuilder extends GtfsDefaultParametersBuilder {

  @Nullable
  private String feedId;

  private URI source;

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

  @Override
  public GtfsFeedParameters build() {
    return new GtfsFeedParameters(
      feedId,
      source,
      removeRepeatedStops(),
      stationTransferPreference(),
      discardMinTransferTimes(),
      blockBasedInterlining(),
      maxInterlineDistance()
    );
  }
}
