package org.opentripplanner.transit.api.request;

import java.util.List;
import org.locationtech.jts.geom.Envelope;

public class RegularStopRequestBuilder {

  private Envelope envelope;
  private List<String> feedIds;
  private boolean filterByInUse;

  protected RegularStopRequestBuilder() {}

  public RegularStopRequestBuilder withEnvelope(Envelope envelope) {
    this.envelope = envelope;
    return this;
  }

  public RegularStopRequestBuilder withFeeds(List<String> feedIds) {
    this.feedIds = feedIds;
    return this;
  }

  public RegularStopRequestBuilder filterByInUse(boolean filterByInUse) {
    this.filterByInUse = filterByInUse;
    return this;
  }

  public RegularStopRequest build() {
    return new RegularStopRequest(envelope, feedIds, filterByInUse);
  }
}
