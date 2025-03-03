package org.opentripplanner.routing.api.request.via;

import java.util.Collection;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * One of the listed stop locations or one of its children must be visited. An on-board
 * intermediate stop visit is ok, as well as boarding or alighting at one of the stops.
 */
public class PassThroughViaLocation extends AbstractViaLocation {

  @SuppressWarnings("DataFlowIssue")
  public PassThroughViaLocation(@Nullable String label, Collection<FeedScopedId> stopLocationIds) {
    super(label, stopLocationIds);
    if (stopLocationIds.isEmpty()) {
      throw new IllegalArgumentException(
        "A pass-through via-location must have at least one stop location." +
        (label == null ? "" : " Label: " + label)
      );
    }
  }

  @Override
  public boolean isPassThroughLocation() {
    return true;
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(PassThroughViaLocation.class)
      .addObj("label", label())
      .addCol("stopLocationIds", stopLocationIds())
      .toString();
  }
}
