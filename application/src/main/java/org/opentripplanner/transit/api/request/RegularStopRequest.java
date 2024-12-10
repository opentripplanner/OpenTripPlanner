package org.opentripplanner.transit.api.request;

import javax.annotation.Nullable;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.transit.model.site.RegularStop;

/**
 * A request for {@link RegularStop}s.
 * <p/>
 * This request is used to retrieve {@link RegularStop}s that match the provided criteria.
 */
public class RegularStopRequest {

  private final Envelope envelope;
  private final String agency;
  private final boolean filterByInUse;

  protected RegularStopRequest(Envelope envelope, String agency, boolean filterByInUse) {
    this.envelope = envelope;
    this.agency = agency;
    this.filterByInUse = filterByInUse;
  }

  public static RegularStopRequestBuilder of(Envelope envelope) {
    return new RegularStopRequestBuilder(envelope);
  }

  public Envelope envelope() {
    return envelope;
  }

  @Nullable
  public String agency() {
    return agency;
  }

  public boolean filterByInUse() {
    return filterByInUse;
  }
}
