package org.opentripplanner.ext.carpooling.filter;

import java.time.Duration;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.ext.carpooling.util.CarReachableVertexSnapper.SnapResult;
import org.opentripplanner.ext.carpooling.util.GraphPathUtils;

/**
 * The passenger as the pre-filters see it: the request, and the car-reachable vertices the
 * passenger was snapped to, with the walks to and from them. A direct request has both; an access
 * request only the pickup at the origin, an egress request only the dropoff at the destination.
 *
 * @param pickup where the car picks the passenger up, or {@code null} for egress
 * @param dropoff where the car drops the passenger off, or {@code null} for access
 */
public record SnappedPassenger(
  CarpoolingRequest request,
  @Nullable SnapResult pickup,
  @Nullable SnapResult dropoff
) {
  public SnappedPassenger {
    Objects.requireNonNull(request, "request");
  }

  /** The walk from the origin to the pickup; zero when there is none or no pickup. */
  public Duration walkToPickup() {
    return pickup == null ? Duration.ZERO : GraphPathUtils.durationOrZero(pickup.walkPath());
  }

  /** The walk from the dropoff to the destination; zero when there is none or no dropoff. */
  public Duration walkFromDropoff() {
    return dropoff == null ? Duration.ZERO : GraphPathUtils.durationOrZero(dropoff.walkPath());
  }
}
