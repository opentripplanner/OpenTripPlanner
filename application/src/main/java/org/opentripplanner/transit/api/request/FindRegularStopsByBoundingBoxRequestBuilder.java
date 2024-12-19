package org.opentripplanner.transit.api.request;

import javax.annotation.Nullable;
import org.locationtech.jts.geom.Envelope;

public class FindRegularStopsByBoundingBoxRequestBuilder {

  private final Envelope envelope;

  @Nullable
  private String feedId;

  private boolean filterByInUse = false;

  FindRegularStopsByBoundingBoxRequestBuilder(Envelope envelope) {
    this.envelope = envelope;
  }

  public FindRegularStopsByBoundingBoxRequestBuilder withFeedId(@Nullable String feedId) {
    this.feedId = feedId;
    return this;
  }

  public FindRegularStopsByBoundingBoxRequestBuilder filterByInUse(boolean filterByInUse) {
    this.filterByInUse = filterByInUse;
    return this;
  }

  public FindRegularStopsByBoundingBoxRequest build() {
    return new FindRegularStopsByBoundingBoxRequest(envelope, feedId, filterByInUse);
  }
}
