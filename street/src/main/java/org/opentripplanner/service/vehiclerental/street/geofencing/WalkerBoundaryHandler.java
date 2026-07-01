package org.opentripplanner.service.vehiclerental.street.geofencing;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.VehicleRentalState;

/**
 * Handles HAVE_RENTED walkers crossing zone boundaries during arriveBy search. The walker
 * already dropped the vehicle (in forward time) and is walking between the drop point and
 * destination.
 *
 * <p>At a paired boundary the walker actually crossed in forward time, dispatches to
 * {@link GeofencingBoundaryEnforcement#arriveByAtBoundary} to produce a walking continuation. Renting
 * branches for "the rental could have been picked up here" are deferred to the next edge by
 * {@link DeferredForkHandler}.
 *
 * <p>The boundary direction that fires depends on the zone type's legal drop position:
 * <ul>
 *   <li><b>Business area</b> — drop must be inside, so the walker exits the BA in forward time
 *       (fromBoundary entering=false).</li>
 *   <li><b>Restricted zone</b> — drop must be outside, so if the destination is inside the
 *       walker enters the zone in forward time (fromBoundary entering=true).</li>
 * </ul>
 */
public class WalkerBoundaryHandler {

  private WalkerBoundaryHandler() {}

  /**
   * Dispatches HAVE_RENTED walker enforcement at paired zone boundaries.
   *
   * @return the enforcement result, or {@code null} if no boundary triggered (state isn't a
   *     HAVE_RENTED walker, no paired boundaries, or no boundary direction matched the
   *     walker-exit-in-forward condition).
   */
  @Nullable
  public static State[] apply(
    State s0,
    List<GeofencingBoundaryExtension> fromBoundaries,
    List<GeofencingBoundaryExtension> toBoundaries,
    EdgeTraversal edge
  ) {
    if (s0.getVehicleRentalState() != VehicleRentalState.HAVE_RENTED) {
      return null;
    }
    for (var boundary : fromBoundaries) {
      if (!hasPair(boundary, toBoundaries)) {
        continue;
      }
      var zone = boundary.zone();
      if (!zone.hasRestriction() && !zone.isBusinessArea()) {
        continue;
      }
      // BA: walker exits in forward time (fromBoundary entering=false).
      // Restricted: walker enters in forward time (fromBoundary entering=true).
      boolean walkerCrossedThisBoundary = zone.isBusinessArea()
        ? !boundary.entering()
        : boundary.entering();
      if (!walkerCrossedThisBoundary) {
        continue;
      }
      var enforcement = GeofencingBoundaryEnforcement.forZone(zone);
      var result = enforcement.arriveByAtBoundary(zone, s0, edge);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  private static boolean hasPair(
    GeofencingBoundaryExtension boundary,
    List<GeofencingBoundaryExtension> toBoundaries
  ) {
    for (var tovBoundary : toBoundaries) {
      if (tovBoundary.isPairedWith(boundary)) {
        return true;
      }
    }
    return false;
  }
}
