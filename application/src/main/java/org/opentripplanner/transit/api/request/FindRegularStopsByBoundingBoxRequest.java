package org.opentripplanner.transit.api.request;

import javax.annotation.Nullable;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.transit.model.site.RegularStop;

/**
 * A request for {@link RegularStop}s within a bounding box.
 * <p/>
 * This request is used to retrieve {@link RegularStop}s that are within a provided bounding box and
 * match the other criteria.
 */
public class FindRegularStopsByBoundingBoxRequest {

  private final Envelope envelope;

  @Nullable
  private final String feedId;

  private final boolean filterByInUse;

  FindRegularStopsByBoundingBoxRequest(
    Envelope envelope,
    @Nullable String feedId,
    boolean filterByInUse
  ) {
    this.envelope = envelope;
    this.feedId = feedId;
    this.filterByInUse = filterByInUse;
  }

  public static FindRegularStopsByBoundingBoxRequestBuilder of(Envelope envelope) {
    return new FindRegularStopsByBoundingBoxRequestBuilder(envelope);
  }

  public Envelope envelope() {
    return envelope;
  }

  @Nullable
  public String feedId() {
    return feedId;
  }

  public boolean filterByInUse() {
    return filterByInUse;
  }
}
