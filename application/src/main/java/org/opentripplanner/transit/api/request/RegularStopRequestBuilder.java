package org.opentripplanner.transit.api.request;

import org.locationtech.jts.geom.Envelope;

public class RegularStopRequestBuilder {

  private Envelope envelope;
  private String agency;
  private boolean filterByInUse = false;

  protected RegularStopRequestBuilder(Envelope envelope) {
    this.envelope = envelope;
  }

  public RegularStopRequestBuilder withFeed(String agency) {
    this.agency = agency;
    return this;
  }

  public RegularStopRequestBuilder filterByInUse(boolean filterByInUse) {
    this.filterByInUse = filterByInUse;
    return this;
  }

  public RegularStopRequest build() {
    return new RegularStopRequest(envelope, agency, filterByInUse);
  }
}
