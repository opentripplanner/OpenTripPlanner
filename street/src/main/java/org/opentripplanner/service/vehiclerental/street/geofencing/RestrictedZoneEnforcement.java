package org.opentripplanner.service.vehiclerental.street.geofencing;

import javax.annotation.Nullable;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.VehicleRentalState;

/**
 * Enforcement for restricted geofencing zones (no-drop-off and no-traversal).
 */
final class RestrictedZoneEnforcement implements GeofencingBoundaryEnforcement {

  static final RestrictedZoneEnforcement INSTANCE = new RestrictedZoneEnforcement();

  private RestrictedZoneEnforcement() {}

  /**
   * Forward search: the rider must decide before entering — drop here (and walk on) or continue
   * riding. Forks floating rentals into a drop + ride branch; blocks station rentals at
   * no-traversal zones since they can't legally drop mid-street.
   */
  @Override
  @Nullable
  public State[] forwardApproachingEntry(GeofencingZone zone, State state, EdgeTraversal edge) {
    if (!state.isRentingVehicle()) {
      return null;
    }
    // Station rentals can't legally drop mid-street, so the floating-style fork doesn't apply.
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
   * ArriveBy search: a committed renting state was inside the zone (current edge crosses
   * outward in forward time). For a no-traversal zone that's illegal — a rented vehicle
   * couldn't legally have been there. Block.
   *
   * <p>Only committed (network-bound) states are checked here. HAVE_RENTED walkers are handled
   * by {@link #arriveByAtBoundary}; generic (null-network) renting states by
   * {@link NetworkCommitmentHandler}.
   */
  @Override
  @Nullable
  public State[] arriveByCrossingExit(GeofencingZone zone, State state, EdgeTraversal edge) {
    if (
      state.isRentingVehicle() &&
      state.getVehicleRentalNetwork() != null &&
      Boolean.TRUE.equals(zone.traversalBanned())
    ) {
      return State.empty();
    }
    return null;
  }

  /**
   * ArriveBy search: a HAVE_RENTED walker is at a boundary they crossed in forward time
   * (rider dropped outside the restricted zone then walked into it to reach a destination
   * inside). Produce a walking continuation; renting branches are deferred to the next edge
   * by {@link DeferredForkHandler}.
   */
  @Override
  @Nullable
  public State[] arriveByAtBoundary(GeofencingZone zone, State state, EdgeTraversal edge) {
    if (state.getVehicleRentalState() != VehicleRentalState.HAVE_RENTED) {
      return null;
    }
    var walking = edge.traverse(state, TraverseMode.WALK);
    if (walking != null) {
      return walking.makeStateArray();
    }
    return State.empty();
  }

  /**
   * Fork at a no-traversal zone entry: drop branch + ride branch. The ride branch keeps the
   * renting option open so the search can route AROUND the zone; if it enters the zone it
   * dies on the next edge in {@link TraversalBanHandler}. The drop is vetoed if the landing
   * vertex is itself drop-banned (e.g. a no-drop-off boundary coinciding with this one).
   */
  private State[] forwardEnteringNoTraversal(State state, EdgeTraversal edge) {
    if (state.isDropOffBannedByCurrentZones()) {
      return State.empty();
    }

    var dropEditor = edge.traverse(state, state.currentMode());
    State dropState = null;
    if (dropEditor != null && !dropEditor.isDropOffBannedByCurrentZones()) {
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
   * Fork at a no-drop-off zone entry: drop branch + ride branch. Returns {@code null} if drop
   * is already banned (passes through). Post-traversal veto discards the drop branch if the
   * edge itself crossed into a no-drop-off zone.
   */
  @Nullable
  private State[] forwardEnteringNoDropOff(State state, EdgeTraversal edge) {
    if (state.isDropOffBannedByCurrentZones()) {
      return null;
    }

    var dropEditor = edge.traverse(state, state.currentMode());
    State dropState = null;
    if (dropEditor != null) {
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
}
