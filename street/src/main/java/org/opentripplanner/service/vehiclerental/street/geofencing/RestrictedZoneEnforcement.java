package org.opentripplanner.service.vehiclerental.street.geofencing;

import javax.annotation.Nullable;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.VehicleRentalState;

/**
 * Enforcement for restricted geofencing zones (no-drop-off and no-traversal). Logic varies by
 * search direction and zone restriction type.
 *
 * <h3>Forward + entering restricted zone</h3>
 * <ul>
 *   <li>No-traversal: fork — one branch drops vehicle here, one continues riding.
 *       If drop-off is also banned (overlapping zones), return empty (dead end).</li>
 *   <li>No-drop-off: fork — one branch drops here, one continues riding.
 *       Post-traversal veto: if the traversal entered a no-drop-off zone during the edge
 *       (adjacent zone boundaries), discard the drop branch.</li>
 * </ul>
 *
 * <h3>ArriveBy + exiting restricted zone (= entering in forward time)</h3>
 * Walking branch only (HAVE_RENTED walker at boundary). Renting branches are deferred to the
 * next edge by {@link DeferredForkHandler}.
 *
 * <h3>ArriveBy + entering restricted zone (committed state)</h3>
 * No-traversal: block. The committed state can't ride into a no-traversal zone.
 */
final class RestrictedZoneEnforcement implements GeofencingEnforcement {

  static final RestrictedZoneEnforcement INSTANCE = new RestrictedZoneEnforcement();

  private RestrictedZoneEnforcement() {}

  @Override
  @Nullable
  public State[] evaluate(
    GeofencingZone zone,
    boolean entering,
    boolean arriveBy,
    State state,
    EdgeTraversal edge
  ) {
    if (entering) {
      return evaluateEntering(zone, arriveBy, state, edge);
    } else {
      return evaluateExiting(zone, arriveBy, state, edge);
    }
  }

  /**
   * State is entering a restricted zone (in forward/geographic terms).
   */
  @Nullable
  private State[] evaluateEntering(
    GeofencingZone zone,
    boolean arriveBy,
    State state,
    EdgeTraversal edge
  ) {
    if (arriveBy) {
      // ArriveBy entering restricted zone: block committed renting states from riding
      // into a no-traversal zone. The committed state can't legally traverse it.
      if (
        state.isRentingVehicle() &&
        state.getVehicleRentalNetwork() != null &&
        Boolean.TRUE.equals(zone.traversalBanned())
      ) {
        return State.empty();
      }
      return null;
    }

    // Forward entering
    if (!state.isRentingVehicle()) {
      return null;
    }

    // Station rentals can't legally drop mid-street, so the floating-style fork doesn't apply.
    // No-traversal: block outright. No-drop-off: irrelevant (no street drops anyway).
    if (state.isRentingVehicleFromStation()) {
      if (Boolean.TRUE.equals(zone.traversalBanned())) {
        return State.empty();
      }
      return null;
    }

    if (Boolean.TRUE.equals(zone.traversalBanned())) {
      return forwardEnteringNoTraversal(state, edge);
    }
    if (Boolean.TRUE.equals(zone.dropOffBanned())) {
      return forwardEnteringNoDropOff(state, edge);
    }
    return null;
  }

  /**
   * Forward entering a no-traversal zone: fork — drop vehicle + continue riding.
   * The ride branch will be blocked at the next edge by the pre-enforcement traversal ban guard.
   */
  private State[] forwardEnteringNoTraversal(State state, EdgeTraversal edge) {
    // If drop-off is also banned here (overlapping no-drop-off zone), dead end
    if (state.isDropOffBannedByCurrentZones()) {
      return State.empty();
    }

    var dropEditor = edge.traverse(state, state.currentMode());
    State dropState = null;
    if (dropEditor != null) {
      dropEditor.dropFloatingVehicle(
        state.vehicleRentalFormFactor(),
        state.rentalVehiclePropulsionType(),
        state.getVehicleRentalNetwork(),
        false
      );
      dropState = dropEditor.makeState();
    }

    var rideEditor = edge.traverse(state, state.currentMode());
    State rideState = rideEditor != null ? rideEditor.makeState() : null;

    return State.ofNullable(dropState, rideState);
  }

  /**
   * Forward entering a no-drop-off zone: fork — drop vehicle + continue riding.
   * If already inside a restricted zone, just pass through (can't drop here anyway).
   * Post-traversal veto: if the traversal entered a no-drop-off zone during the edge,
   * discard the drop branch.
   */
  @Nullable
  private State[] forwardEnteringNoDropOff(State state, EdgeTraversal edge) {
    // Already inside a no-drop-off zone — can't drop here, just ride through
    if (state.isDropOffBannedByCurrentZones()) {
      return null;
    }

    var dropEditor = edge.traverse(state, state.currentMode());
    State dropState = null;
    if (dropEditor != null) {
      // Post-traversal veto: the traversal updated zone state. If the state entered a
      // no-drop-off zone during this edge (adjacent zone boundaries), discard the drop.
      if (!dropEditor.isDropOffBannedByCurrentZones()) {
        dropEditor.dropFloatingVehicle(
          state.vehicleRentalFormFactor(),
          state.rentalVehiclePropulsionType(),
          state.getVehicleRentalNetwork(),
          false
        );
        dropState = dropEditor.makeState();
      }
    }

    var rideEditor = edge.traverse(state, state.currentMode());
    State rideState = rideEditor != null ? rideEditor.makeState() : null;

    return State.ofNullable(dropState, rideState);
  }

  /**
   * State is exiting a restricted zone (in forward/geographic terms).
   * In arriveBy, this means the real-time trip enters the zone — the walker (HAVE_RENTED)
   * is crossing out of the restricted zone. Produce a walking branch only; renting branches
   * are deferred to the next edge by {@link DeferredForkHandler}.
   */
  @Nullable
  private State[] evaluateExiting(
    GeofencingZone zone,
    boolean arriveBy,
    State state,
    EdgeTraversal edge
  ) {
    if (!arriveBy) {
      return null;
    }

    // ArriveBy exiting restricted zone: only applies to HAVE_RENTED walkers
    if (state.getVehicleRentalState() != VehicleRentalState.HAVE_RENTED) {
      return null;
    }

    // Produce walking branch — renting branches deferred to next edge
    var walking = edge.traverse(state, TraverseMode.WALK);
    if (walking != null) {
      return walking.makeStateArray();
    }
    return State.empty();
  }
}
