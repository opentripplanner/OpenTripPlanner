package org.opentripplanner.service.vehiclerental.street.geofencing;

import javax.annotation.Nullable;
import org.opentripplanner.street.search.state.State;

/**
 * Set-level invariant: a renting state inside a traversal-banned zone set must not progress.
 *
 * <p>Distinct from the per-zone {@link GeofencingBoundaryEnforcement} strategies — the answer depends
 * on the priority-resolved view of {@code currentZones} for the state's network, not on any
 * single zone in isolation, so this check doesn't fit the per-zone dispatch shape.
 */
class TraversalBanHandler {

  private TraversalBanHandler() {}

  /** Block if the state is renting and the resolved zones forbid traversal. */
  @Nullable
  public static State[] apply(State state) {
    if (state.isRentingVehicle() && state.isTraversalBannedByCurrentZones()) {
      return State.empty();
    }
    return null;
  }
}
