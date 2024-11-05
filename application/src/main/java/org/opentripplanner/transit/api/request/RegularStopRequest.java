package org.opentripplanner.transit.api.request;

import java.util.List;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.transit.model.site.RegularStop;

/**
 * A request for {@link RegularStop}s.
 *
 * This request is used to retrieve {@link RegularStop}s that match the provided criteria.
 */
public class RegularStopRequest {

  private final Envelope envelope;
  private final List<String> feedIds;
  private final boolean filterByInUse;

  protected RegularStopRequest(Envelope envelope, List<String> feedIds, boolean filterByInUse) {
    this.envelope = envelope;
    this.feedIds = feedIds;
    this.filterByInUse = filterByInUse;
  }

  public static RegularStopRequestBuilder of() {
    return new RegularStopRequestBuilder();
  }

  public Envelope envelope() {
    return envelope;
  }

  public List<String> feedIds() {
    return feedIds;
  }

  public boolean filterByInUse() {
    return filterByInUse;
  }
}
