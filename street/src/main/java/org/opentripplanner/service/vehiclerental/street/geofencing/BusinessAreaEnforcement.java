package org.opentripplanner.service.vehiclerental.street.geofencing;

import javax.annotation.Nullable;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.VehicleRentalState;

/**
 * Enforcement for business area geofencing zones. The logic is inverted compared to restricted
 * zones: exiting the BA is restricted (the rider can't leave the operating area on the vehicle),
 * while entering is allowed. Station rentals can't legally drop mid-street and are blocked
 * at any BA exit.
 */
final class BusinessAreaEnforcement implements GeofencingBoundaryEnforcement {

  static final BusinessAreaEnforcement INSTANCE = new BusinessAreaEnforcement();

  private BusinessAreaEnforcement() {}

  /**
   * Forward search: the rider rides to the last in-BA vertex and drops the vehicle there before
   * leaving the operating area. Station rentals are blocked (can't drop mid-street).
   */
  @Override
  @Nullable
  public State[] forwardApproachingExit(GeofencingZone zone, State state, EdgeTraversal edge) {
    if (!state.isRentingVehicle()) {
      return null;
    }
    if (state.isRentingVehicleFromStation()) {
      return State.empty();
    }
    return forwardExit(state, edge);
  }

  /**
   * Forward search: fallback for renting states placed at the boundary vertex itself, where no
   * prior edge could fire {@link #forwardApproachingExit}. The renting branch is blocked
   * — pure walking from the corresponding {@code BEFORE_RENTING} branch dominates the
   * walk-and-drop alternative in any reasonable cost model.
   */
  @Override
  @Nullable
  public State[] forwardCrossingExit(GeofencingZone zone, State state, EdgeTraversal edge) {
    if (!state.isRentingVehicle()) {
      return null;
    }
    return State.empty();
  }

  /**
   * ArriveBy search: a HAVE_RENTED walker is at a boundary they crossed in forward time
   * (rider dropped inside the BA then walked out to the destination). Produce a walking
   * continuation; renting branches are deferred to the next edge by
   * {@link DeferredForkHandler}.
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

  /** Ride to the last in-zone vertex in the rider's current mode and drop the floating vehicle there. */
  private State[] forwardExit(State state, EdgeTraversal edge) {
    if (state.isDropOffBannedByCurrentZones()) {
      return State.empty();
    }
    var editor = edge.traverse(state, state.currentMode());
    if (editor == null) {
      return State.empty();
    }
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
}
