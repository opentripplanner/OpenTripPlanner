package org.opentripplanner.service.vehiclerental.street.geofencing;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.opentripplanner.street.mapping.StreetModeToRentalTraverseModeMapper;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.state.State;

/**
 * Handles network commitment for generic (null-network) RENTING_FLOATING states in arrive-by
 * searches. When a generic state crosses a zone boundary, this handler forks committed branches
 * for each new network and continues the generic with updated committedNetworks.
 *
 * <p>Also handles no-traversal zone network recording: when a generic state enters a
 * no-traversal zone, the zone's network is added to committedNetworks (since a committed
 * branch for that network could never legally traverse this path).
 */
class NetworkCommitmentHandler {

  private NetworkCommitmentHandler() {}

  /** Commit network branches if a generic arriveBy state crosses a zone boundary. */
  @Nullable
  public static State[] applyNetworkCommitment(
    State s0,
    List<GeofencingBoundaryExtension> fromBoundaries,
    EdgeTraversal edge
  ) {
    var noTraversalNetworks = collectNoTraversalNetworks(s0, fromBoundaries);
    if (!noTraversalNetworks.isEmpty()) {
      var editor = edge.traverse(s0, s0.currentMode());
      if (editor != null) {
        for (var n : noTraversalNetworks) {
          editor.addCommittedNetwork(n);
        }
        return State.ofNullable(editor.makeState());
      }
      return State.empty();
    }

    // Zone crossing fork: commit per-network branches
    return commitNetworks(s0, edge);
  }

  /**
   * Collect networks from no-traversal zones that a generic state would enter.
   * These are recorded in committedNetworks to avoid wasteful forks later.
   */
  private static Set<String> collectNoTraversalNetworks(
    State s0,
    List<GeofencingBoundaryExtension> fromBoundaries
  ) {
    Set<String> networks = null;
    for (var boundary : fromBoundaries) {
      if (Boolean.TRUE.equals(boundary.zone().traversalBanned()) && !boundary.entering()) {
        String n = boundary.zone().id().getFeedId();
        if (!s0.getCommittedNetworks().contains(n)) {
          if (networks == null) {
            networks = new HashSet<>();
          }
          networks.add(n);
        }
      }
    }
    return networks != null ? networks : Set.of();
  }

  @Nullable
  private static State[] commitNetworks(State s0, EdgeTraversal edge) {
    var editor = edge.traverse(s0, s0.currentMode());
    var traversedState = editor != null ? editor.makeState() : null;
    if (traversedState == null) {
      return null;
    }
    var forkNetworks = new HashSet<String>();
    var commitOnlyNetworks = new HashSet<String>();
    classifyNewZoneNetworks(s0, traversedState, forkNetworks, commitOnlyNetworks);
    if (forkNetworks.isEmpty() && commitOnlyNetworks.isEmpty()) {
      return null;
    }
    return forkCommittedBranches(s0, forkNetworks, commitOnlyNetworks, edge);
  }

  /**
   * Classify new zone networks into those that need committed forks and those that only need
   * to be recorded in committedNetworks. Business area networks are commit-only: the committed
   * renting branch for a BA network is created via the HAVE_RENTED walker path
   * ({@link BusinessAreaEnforcement} + {@link DeferredForkHandler}), which correctly enforces
   * boundary drop-off. Forking a committed rider here would create an illegal path that drops
   * off outside the business area.
   */
  private static void classifyNewZoneNetworks(
    State before,
    State after,
    HashSet<String> forkNetworks,
    HashSet<String> commitOnlyNetworks
  ) {
    for (var zone : after.getCurrentGeofencingZones()) {
      if (!before.getCurrentGeofencingZones().contains(zone)) {
        String network = zone.id().getFeedId();
        if (
          before.getCommittedNetworks().contains(network) ||
          Boolean.TRUE.equals(zone.traversalBanned())
        ) {
          continue;
        }
        if (zone.isBusinessArea()) {
          commitOnlyNetworks.add(network);
        } else {
          forkNetworks.add(network);
        }
      }
    }
    // If a network has both BA and non-BA zones at this boundary, fork it
    commitOnlyNetworks.removeAll(forkNetworks);
  }

  private static State[] forkCommittedBranches(
    State s0,
    HashSet<String> forkNetworks,
    HashSet<String> commitOnlyNetworks,
    EdgeTraversal edge
  ) {
    var states = new ArrayList<State>();
    var request = s0.getRequest();

    for (String network : forkNetworks) {
      if (!isNetworkAllowedByRequest(network, request)) {
        continue;
      }
      var networkBranch = edge.traverse(s0, s0.currentMode());
      if (networkBranch != null) {
        networkBranch.bindToNetwork(network);
        var commitState = networkBranch.makeState();
        if (commitState != null) {
          states.add(commitState);
        }
      }
    }

    var generic = edge.traverse(s0, s0.currentMode());
    if (generic != null) {
      for (String network : commitOnlyNetworks) {
        generic.addCommittedNetwork(network);
      }
      var genericContinue = generic.makeState();
      if (genericContinue != null) {
        states.add(genericContinue);
      }
    }

    return states.toArray(State[]::new);
  }

  static boolean isNetworkAllowedByRequest(String network, StreetSearchRequest request) {
    var rentalMode = StreetModeToRentalTraverseModeMapper.map(request.mode());
    var rentalRequest = request.rental(rentalMode);
    if (rentalRequest == null) {
      return true;
    }
    var allowedNetworks = rentalRequest.allowedNetworks();
    var bannedNetworks = rentalRequest.bannedNetworks();
    if (!allowedNetworks.isEmpty()) {
      return allowedNetworks.contains(network);
    }
    return !bannedNetworks.contains(network);
  }
}
