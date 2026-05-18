package org.opentripplanner.service.vehiclerental.street.geofencing;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.VehicleRentalState;

/**
 * Intercepts rental edge traversals to enforce geofencing zone restrictions. Called from
 * {@code StreetEdge.traverse()} — returns {@code State[]} to override the normal traversal,
 * or {@code null} to let it proceed.
 *
 * <p>Delegates to three handlers:
 * <ul>
 *   <li>{@link DeferredForkHandler} — completes renting branches deferred from a prior edge</li>
 *   <li>{@link GeofencingEnforcement} — zone-type-specific decisions (fork, block, drop)
 *       via {@link RestrictedZoneEnforcement} and {@link BusinessAreaEnforcement}</li>
 *   <li>{@link NetworkCommitmentHandler} — forks generic arrive-by states into per-network
 *       committed branches at zone boundaries</li>
 * </ul>
 *
 * <p>Forward enforcement checks <b>tov</b> boundaries (approaching/inside a zone) and fromv
 * for business area exit. ArriveBy enforcement checks <b>fromv</b> boundaries for committed
 * renting states and HAVE_RENTED walkers.
 */
public class GeofencingInterceptor {

  private GeofencingInterceptor() {}

  @Nullable
  public static State[] apply(
    State s0,
    List<GeofencingBoundaryExtension> fromBoundaries,
    List<GeofencingBoundaryExtension> toBoundaries,
    EdgeTraversal edge
  ) {
    // Pre-guard: renting states inside no-traversal zones must drop or be blocked.
    // Station rentals can't legally drop mid-street, so they're blocked outright.
    if (s0.isRentingVehicle() && s0.isTraversalBannedByCurrentZones()) {
      if (s0.isRentingVehicleFromStation() || s0.isDropOffBannedByCurrentZones()) {
        return State.empty();
      }
      return forceDrop(s0, edge);
    }

    if (s0.getRequest().arriveBy()) {
      return evaluateArriveBy(s0, fromBoundaries, toBoundaries, edge);
    } else {
      return evaluateForward(s0, fromBoundaries, toBoundaries, edge);
    }
  }

  /**
   * Forward enforcement: check tov boundaries for zone approach/entry, and fromv boundaries
   * for business area exit.
   */
  @Nullable
  private static State[] evaluateForward(
    State s0,
    List<GeofencingBoundaryExtension> fromBoundaries,
    List<GeofencingBoundaryExtension> toBoundaries,
    EdgeTraversal edge
  ) {
    if (!s0.isRentingVehicle()) {
      return null;
    }
    String network = s0.getVehicleRentalNetwork();

    // Check tov boundaries: approaching or inside a zone
    for (var boundary : toBoundaries) {
      var zone = boundary.zone();
      if (network != null && !zone.id().getFeedId().equals(network)) {
        continue;
      }

      if (boundary.entering()) {
        // tov is a boundary vertex (outside zone, entering direction) — fork at approach
        var enforcement = GeofencingEnforcement.forZone(zone);
        var result = enforcement.evaluate(zone, true, false, s0, edge);
        if (result != null) {
          return result;
        }
      } else {
        // tov is inside a zone (exiting direction)
        if (Boolean.TRUE.equals(zone.traversalBanned())) {
          return State.empty();
        }
        // BA exit (primary): tov is the last vertex inside the BA. Ride to it and
        // drop there, so the drop-off is inside the BA (not one vertex outside).
        // This fires on the edge BEFORE the boundary crossing.
        if (zone.isBusinessArea()) {
          var result = BusinessAreaEnforcement.INSTANCE.evaluateForwardExitingAtBoundary(s0, edge);
          if (result != null && result.length > 0) {
            return result;
          }
        }
      }
    }

    // BA exit (fallback): fromv is the exit boundary vertex. This handles the case
    // where the state starts at the boundary (no previous edge for the tov check).
    // The rider is forced to walk; drop-off is at tov (one vertex outside).
    for (var boundary : fromBoundaries) {
      var zone = boundary.zone();
      if (!zone.isBusinessArea()) {
        continue;
      }
      if (network != null && !zone.id().getFeedId().equals(network)) {
        continue;
      }
      if (!boundary.entering()) {
        var enforcement = GeofencingEnforcement.forZone(zone);
        var result = enforcement.evaluate(zone, false, false, s0, edge);
        if (result != null) {
          return result;
        }
      }
    }

    return null;
  }

  /**
   * ArriveBy enforcement: check fromv boundaries for committed renting states and HAVE_RENTED
   * walkers, then handle generic state network commitment.
   */
  @Nullable
  private static State[] evaluateArriveBy(
    State s0,
    List<GeofencingBoundaryExtension> fromBoundaries,
    List<GeofencingBoundaryExtension> toBoundaries,
    EdgeTraversal edge
  ) {
    var deferredResult = DeferredForkHandler.applyDeferredFork(s0, edge);
    if (deferredResult != null) {
      return deferredResult;
    }

    if (fromBoundaries.isEmpty()) {
      return null;
    }

    String network = s0.getVehicleRentalNetwork();

    // Committed renting states: check fromv boundaries (no pairing needed)
    if (s0.isRentingVehicle() && network != null) {
      for (var boundary : fromBoundaries) {
        var zone = boundary.zone();
        if (!zone.id().getFeedId().equals(network)) {
          continue;
        }
        // entering=false on fromv → effectiveEntering = true in arriveBy (entering zone)
        if (!boundary.entering() && Boolean.TRUE.equals(zone.traversalBanned())) {
          return State.empty();
        }
      }
    }

    // HAVE_RENTED walkers: check fromv paired boundaries for zone enforcement.
    // Restricted zones and business areas need opposite direction handling:
    //  - Restricted: enforcement at FAR boundary (walker exits zone in arrive-by)
    //    fromv entering=true → effectiveEntering = !true = false → evaluateExiting
    //  - BA: enforcement at NEAR boundary (walker enters BA in arrive-by)
    //    fromv entering=false → effectiveEntering = false → evaluateExiting (= forward exit)
    if (s0.getVehicleRentalState() == VehicleRentalState.HAVE_RENTED) {
      for (var boundary : fromBoundaries) {
        if (!hasPairedBoundary(boundary, toBoundaries)) {
          continue;
        }
        var zone = boundary.zone();
        if (!zone.hasRestriction() && !zone.isBusinessArea()) {
          continue;
        }
        boolean effectiveEntering = zone.isBusinessArea()
          ? boundary.entering()
          : !boundary.entering();
        var enforcement = GeofencingEnforcement.forZone(zone);
        var result = enforcement.evaluate(zone, effectiveEntering, true, s0, edge);
        if (result != null) {
          return result;
        }
      }
    }

    // Generic states: network commitment at zone boundaries
    if (s0.isRentingFloatingVehicle() && network == null) {
      return NetworkCommitmentHandler.applyNetworkCommitment(s0, fromBoundaries, edge);
    }

    return null;
  }

  private static State[] forceDrop(State s0, EdgeTraversal edge) {
    var editor = edge.traverse(s0, org.opentripplanner.street.search.TraverseMode.WALK);
    if (editor != null) {
      if (editor.isDropOffBannedByCurrentZones()) {
        return State.empty();
      }
      editor.dropFloatingVehicle(
        s0.vehicleRentalFormFactor(),
        s0.rentalVehiclePropulsionType(),
        s0.getVehicleRentalNetwork(),
        s0.getRequest().arriveBy()
      );
      return State.ofNullable(editor.makeState());
    }
    return State.empty();
  }

  private static boolean hasPairedBoundary(
    GeofencingBoundaryExtension boundary,
    List<GeofencingBoundaryExtension> toBoundaries
  ) {
    for (var tovBoundary : toBoundaries) {
      if (
        tovBoundary.zone().equals(boundary.zone()) && tovBoundary.entering() != boundary.entering()
      ) {
        return true;
      }
    }
    return false;
  }
}
