package org.opentripplanner.transit.api.request;

import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.transit.model.site.RegularStop;

/**
 * A request for {@link RegularStop}s.
 * <p/>
 * This request is used to retrieve {@link RegularStop}s that match the provided criteria.
 */
public class RegularStopRequest {

  private final Envelope envelope;
  private final String feedId;
  private final boolean filterByInUse;

  protected RegularStopRequest(Envelope envelope, String feedId, boolean filterByInUse) {
    this.envelope = envelope;
    this.feedId = feedId;
    this.filterByInUse = filterByInUse;
  }

  public static RegularStopRequestBuilder of() {
    return new RegularStopRequestBuilder();
  }

  public Envelope envelope() {
    return envelope;
  }

  public String feedId() {
    return feedId;
  }

  public boolean filterByInUse() {
    return filterByInUse;
  }
}
