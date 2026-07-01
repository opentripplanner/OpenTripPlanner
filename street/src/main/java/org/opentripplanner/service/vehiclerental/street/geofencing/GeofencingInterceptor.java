package org.opentripplanner.service.vehiclerental.street.geofencing;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.street.search.state.State;

/**
 * Intercepts rental edge traversals to enforce geofencing zone restrictions. Called from
 * {@code StreetEdge.traverse()} — returns {@code State[]} to override the normal traversal,
 * or {@code null} to let it proceed.
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
    var result = TraversalBanHandler.apply(s0);
    if (result != null) {
      return result;
    }

    if (s0.getRequest().arriveBy()) {
      return evaluateArriveBy(s0, fromBoundaries, toBoundaries, edge);
    } else {
      return evaluateForward(s0, fromBoundaries, toBoundaries, edge);
    }
  }

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

    for (var boundary : toBoundaries) {
      var zone = boundary.zone();
      if (network != null && !zone.id().getFeedId().equals(network)) {
        continue;
      }
      var enforcement = GeofencingBoundaryEnforcement.forZone(zone);
      var result = boundary.entering()
        ? enforcement.forwardApproachingEntry(zone, s0, edge)
        : enforcement.forwardApproachingExit(zone, s0, edge);
      if (result != null) {
        return result;
      }
    }

    // Only dispatch outward-crossing — inward-crossing entry decisions fire one edge earlier
    // via forwardApproachingEntry.
    for (var boundary : fromBoundaries) {
      if (boundary.entering()) {
        continue;
      }
      var zone = boundary.zone();
      if (network != null && !zone.id().getFeedId().equals(network)) {
        continue;
      }
      var enforcement = GeofencingBoundaryEnforcement.forZone(zone);
      var result = enforcement.forwardCrossingExit(zone, s0, edge);
      if (result != null) {
        return result;
      }
    }

    return null;
  }

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

    if (s0.isRentingVehicle() && network != null) {
      for (var boundary : fromBoundaries) {
        var zone = boundary.zone();
        if (!zone.id().getFeedId().equals(network)) {
          continue;
        }
        if (boundary.entering()) {
          continue;
        }
        var enforcement = GeofencingBoundaryEnforcement.forZone(zone);
        var result = enforcement.arriveByCrossingExit(zone, s0, edge);
        if (result != null) {
          return result;
        }
      }
    }

    var walkerResult = WalkerBoundaryHandler.apply(s0, fromBoundaries, toBoundaries, edge);
    if (walkerResult != null) {
      return walkerResult;
    }

    if (s0.isRentingFloatingVehicle() && network == null) {
      return NetworkCommitmentHandler.applyNetworkCommitment(s0, fromBoundaries, edge);
    }

    return null;
  }
}
