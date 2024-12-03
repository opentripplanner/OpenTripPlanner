package org.opentripplanner.transit.api.request;

import org.locationtech.jts.geom.Envelope;

public class RegularStopRequestBuilder {

  private Envelope envelope;
  private String feedId;
  private boolean filterByInUse;

  protected RegularStopRequestBuilder() {}

  public RegularStopRequestBuilder withEnvelope(Envelope envelope) {
    this.envelope = envelope;
    return this;
  }

  public RegularStopRequestBuilder withFeed(String feedId) {
    this.feedId = feedId;
    return this;
  }

  public RegularStopRequestBuilder filterByInUse(boolean filterByInUse) {
    this.filterByInUse = filterByInUse;
    return this;
  }

  public RegularStopRequest build() {
    return new RegularStopRequest(envelope, feedId, filterByInUse);
  }
}
