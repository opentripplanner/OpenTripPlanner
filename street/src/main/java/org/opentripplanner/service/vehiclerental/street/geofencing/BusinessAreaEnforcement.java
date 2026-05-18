package org.opentripplanner.service.vehiclerental.street.geofencing;

import javax.annotation.Nullable;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.VehicleRentalState;

/**
 * Enforcement for business area geofencing zones. The logic is inverted compared to restricted
 * zones: exiting the business area is restricted (the rider can't leave the operating area on
 * the vehicle), while entering is allowed.
 *
 * <h3>Forward + exiting business area</h3>
 * Force drop at boundary. If drop-off is also banned (overlapping no-drop-off zone), block.
 * Post-traversal veto: if the traversal entered a no-drop-off zone during the edge, block.
 *
 * <h3>ArriveBy + exiting business area (= entering in forward time)</h3>
 * Walking branch only (HAVE_RENTED walker at boundary). Renting branches are deferred to the
 * next edge by {@link DeferredForkHandler}.
 *
 * <h3>Entering business area (either direction)</h3>
 * Pass — entering is fine.
 */
final class BusinessAreaEnforcement implements GeofencingEnforcement {

  static final BusinessAreaEnforcement INSTANCE = new BusinessAreaEnforcement();

  private BusinessAreaEnforcement() {}

  @Override
  @Nullable
  public State[] evaluate(
    GeofencingZone zone,
    boolean entering,
    boolean arriveBy,
    State state,
    EdgeTraversal edge
  ) {
    // Entering a business area is always fine
    if (entering) {
      return null;
    }

    // Exiting a business area
    if (arriveBy) {
      return evaluateArriveByExiting(state, edge);
    } else {
      if (!state.isRentingVehicle()) {
        return null;
      }
      // Station rentals can't drop mid-street, so a mid-street BA exit is illegal.
      if (state.isRentingVehicleFromStation()) {
        return State.empty();
      }
      return evaluateForwardExiting(state, edge);
    }
  }

  /**
   * Forward exiting business area: force drop at boundary, traversing in WALK mode.
   * Used as a fallback when the state starts at the boundary vertex (fromv check).
   */
  private State[] evaluateForwardExiting(State state, EdgeTraversal edge) {
    // If drop-off is also banned (overlapping no-drop-off zone), dead end
    if (state.isDropOffBannedByCurrentZones()) {
      return State.empty();
    }

    var editor = edge.traverse(state, TraverseMode.WALK);
    if (editor != null) {
      // Post-traversal check: if the traversal entered a no-drop-off zone during the edge,
      // block the drop
      if (editor.isDropOffBannedByCurrentZones()) {
        return State.empty();
      }
      editor.dropFloatingVehicle(
        state.vehicleRentalFormFactor(),
        state.rentalVehiclePropulsionType(),
        state.getVehicleRentalNetwork(),
        false
      );
      return State.ofNullable(editor.makeState());
    }
    return State.empty();
  }

  /**
   * Forward exiting business area (primary): ride to the boundary vertex and drop there.
   * The tov is the last vertex inside the BA (entering=false), so the drop-off is inside.
   */
  State[] evaluateForwardExitingAtBoundary(State state, EdgeTraversal edge) {
    // Station rentals can't drop mid-street, so a mid-street BA exit is illegal.
    if (state.isRentingVehicleFromStation()) {
      return State.empty();
    }
    if (state.isDropOffBannedByCurrentZones()) {
      return State.empty();
    }

    var editor = edge.traverse(state, state.currentMode());
    if (editor != null) {
      if (editor.isDropOffBannedByCurrentZones()) {
        return State.empty();
      }
      editor.dropFloatingVehicle(
        state.vehicleRentalFormFactor(),
        state.rentalVehiclePropulsionType(),
        state.getVehicleRentalNetwork(),
        false
      );
      return State.ofNullable(editor.makeState());
    }
    return State.empty();
  }

  /**
   * ArriveBy exiting business area (= entering in forward time): walking branch only.
   * Renting branches are deferred to the next edge by {@link DeferredForkHandler}.
   */
  @Nullable
  private State[] evaluateArriveByExiting(State state, EdgeTraversal edge) {
    // Only applies to HAVE_RENTED walkers
    if (state.getVehicleRentalState() != VehicleRentalState.HAVE_RENTED) {
      return null;
    }

    var walking = edge.traverse(state, TraverseMode.WALK);
    if (walking != null) {
      return walking.makeStateArray();
    }
    return State.empty();
  }
}
