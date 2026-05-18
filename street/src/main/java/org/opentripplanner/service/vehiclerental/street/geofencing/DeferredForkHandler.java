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
 * Handles the deferred renting fork — creates RENTING_FLOATING states one edge after a zone
 * boundary, so their backEdge is safely outside the zone in the forward itinerary.
 *
 * <h3>Why deferral is needed</h3>
 * In arrive-by searches, when enforcement triggers a boundary fork at vertex C, we want to
 * create RENTING_FLOATING states representing "rider dropped off here in forward time." But
 * if created at C, their backEdge is C→D (pointing into the zone). When reversed to forward
 * time, the itinerary shows the drop-off at D (inside zone) instead of C.
 *
 * <h3>How it works</h3>
 * The enforcement returns only the walking branch at the boundary. On the next edge, this
 * handler detects the zone exit by comparing the backState's zone set with the current state's.
 * If restricted zones were exited, it creates renting branches here where the backEdge is
 * safely outside the zone.
 *
 * <h3>Future</h3>
 * A future fix could move this responsibility to the itinerary builder (detect that a rental
 * leg's last edge points into a restricted zone and adjust the drop-off location), eliminating
 * the deferral entirely.
 */
class DeferredForkHandler {

  private DeferredForkHandler() {}

  /**
   * Check if the previous edge's enforcement deferred renting branch creation, and if so,
   * create those branches now.
   *
   * @return State[] with walking + renting branches, or null if no deferred fork needed
   */
  @Nullable
  public static State[] applyDeferredFork(State s0, EdgeTraversal edge) {
    if (!isDeferredRentingForkTrigger(s0)) {
      return null;
    }
    return performDeferredRentingFork(s0, edge);
  }

  /**
   * Whether a HAVE_RENTED walker just crossed a zone boundary that requires a deferred fork.
   * Two cases:
   * <ul>
   *   <li>Zone loss (restricted zones): backState had zones s0 doesn't — walker exited zone
   *       in arrive-by, deferred fork is outside the zone.</li>
   *   <li>Zone gain (business areas): s0 has BA zones backState didn't — walker entered BA
   *       in arrive-by (= exited in forward time), deferred fork is inside the BA.</li>
   * </ul>
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
   * Create renting branches one edge after the zone boundary. The walker just exited a
   * restricted zone or business area. Now at the first edge outside the zone, create per-network
   * and generic RENTING_FLOATING states with backEdges safely outside the zone.
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
