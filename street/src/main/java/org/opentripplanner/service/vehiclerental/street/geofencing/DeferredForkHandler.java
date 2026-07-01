package org.opentripplanner.service.vehiclerental.street.geofencing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.service.vehiclerental.model.GeofencingZone;
import org.opentripplanner.street.mapping.StreetModeToRentalTraverseModeMapper;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.VehicleRentalState;

/**
 * Creates the deferred RENTING_FLOATING fork one edge after a zone boundary, so the renting
 * state's backEdge is safely outside the zone in the forward itinerary.
 *
 * <p>Forking at the boundary itself would attach the drop event to an edge pointing into the
 * zone (the renting state's backEdge would be the boundary edge), so the rendered itinerary
 * would show the drop on the wrong side. Instead, the strategy returns only the walking branch
 * at the boundary; this handler creates the renting branches on the next edge, where the
 * backEdge is safely outside.
 */
class DeferredForkHandler {

  private DeferredForkHandler() {}

  /** If the previous edge deferred a renting fork, create the branches now. */
  @Nullable
  public static State[] applyDeferredFork(State s0, EdgeTraversal edge) {
    if (!isDeferredRentingForkTrigger(s0)) {
      return null;
    }
    return performDeferredRentingFork(s0, edge);
  }

  /**
   * Trigger when a HAVE_RENTED walker just crossed a zone boundary the previous edge deferred:
   * restricted-zone loss (walker exited in arriveBy) or BA gain (walker entered BA in arriveBy
   * = exited in forward time).
   */
  private static boolean isDeferredRentingForkTrigger(State s0) {
    if (s0.getVehicleRentalState() != VehicleRentalState.HAVE_RENTED) {
      return false;
    }
    var backState = s0.getBackState();
    if (backState == null) {
      return false;
    }
    // Zone loss: backState had restricted/BA zones that s0 doesn't
    for (var zone : backState.getCurrentGeofencingZones()) {
      if (
        (zone.hasRestriction() || zone.isBusinessArea()) &&
        !s0.getCurrentGeofencingZones().contains(zone)
      ) {
        return true;
      }
    }
    // Zone gain: s0 has BA zones that backState didn't (walker entered BA in arrive-by)
    for (var zone : s0.getCurrentGeofencingZones()) {
      if (zone.isBusinessArea() && !backState.getCurrentGeofencingZones().contains(zone)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Build the walking continuation plus per-network and generic RENTING_FLOATING states for the
   * networks the walker just exited.
   */
  private static State[] performDeferredRentingFork(State s0, EdgeTraversal edge) {
    var request = s0.getRequest();
    var states = new ArrayList<State>();

    var walking = edge.traverse(s0, org.opentripplanner.street.search.TraverseMode.WALK);
    if (walking != null) {
      var walkState = walking.makeState();
      if (walkState != null) {
        states.add(walkState);
      }
    }

    // Collect networks from zones the walker just exited
    var forkNetworks = collectExitedNetworks(s0);

    var rentalMode = StreetModeToRentalTraverseModeMapper.map(request.mode());

    boolean hasNetworkStates = false;
    for (String network : forkNetworks) {
      if (!isNetworkAllowedByRequest(network, request)) {
        continue;
      }
      // Pre-traversal veto: if the walker is currently inside a no-drop-off zone for
      // this network (e.g., adjacent zones), don't create a renting branch here.
      if (
        GeofencingZone.resolveField(
          s0.getCurrentGeofencingZones(),
          network,
          GeofencingZone::dropOffBanned
        )
      ) {
        continue;
      }
      var edit = edge.traverse(s0, rentalMode);
      if (edit != null) {
        // Post-traversal veto: if the traversal entered a no-drop-off zone during this edge,
        // discard this network's branch
        if (edit.isDropOffBannedForNetwork(network)) {
          continue;
        }
        edit.dropFloatingVehicle(
          s0.vehicleRentalFormFactor(),
          s0.rentalVehiclePropulsionType(),
          network,
          true
        );
        var state = edit.makeState();
        if (state != null) {
          states.add(state);
          hasNetworkStates = true;
        }
      }
    }

    if (hasNetworkStates) {
      var edit = edge.traverse(s0, rentalMode);
      if (edit != null) {
        edit.dropFloatingVehicle(
          s0.vehicleRentalFormFactor(),
          s0.rentalVehiclePropulsionType(),
          null,
          true
        );
        var state = edit.makeState();
        if (state != null) {
          states.add(state);
        }
      }
    }

    return states.toArray(State[]::new);
  }

  private static Set<String> collectExitedNetworks(State s0) {
    var forkNetworks = new HashSet<String>();
    // Zone loss: restricted/BA zones backState had that s0 doesn't
    for (var zone : s0.getBackState().getCurrentGeofencingZones()) {
      if (
        (zone.hasRestriction() || zone.isBusinessArea()) &&
        !s0.getCurrentGeofencingZones().contains(zone)
      ) {
        forkNetworks.add(zone.id().getFeedId());
      }
    }
    // Zone gain: BA zones s0 has that backState didn't (walker entered BA in arrive-by)
    for (var zone : s0.getCurrentGeofencingZones()) {
      if (zone.isBusinessArea() && !s0.getBackState().getCurrentGeofencingZones().contains(zone)) {
        forkNetworks.add(zone.id().getFeedId());
      }
    }
    return forkNetworks;
  }

  private static boolean isNetworkAllowedByRequest(
    String network,
    org.opentripplanner.street.search.request.StreetSearchRequest request
  ) {
    return NetworkCommitmentHandler.isNetworkAllowedByRequest(network, request);
  }
}
