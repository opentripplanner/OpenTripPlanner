package org.opentripplanner.ext.flex.flexpathcalculator;

import org.opentripplanner.street.model.path.LazyStreetPath;
import org.opentripplanner.street.search.state.State;

/**
 * Extracts the geometry and distance from a {@link State} chain produced by
 * an A* street search. The state chain is a linked list from the final state back to the origin via
 * {@link State#getBackState()}/{@link State#getBackEdge()}.
 * <p>
 * This utility encapsulates the direction-dependent ordering: for depart-after searches the chain
 * yields a geometry in reverse chronological order (newest first), while for arriveBy searches the chain
 * already yields edges in chronological order.
 * Implementation note: an earlier design relied on eagerly materializing the list of edges in
 * chronological order (allocating a full copy and, for arriveBy searches, reversing the state
 * chain). The current implementation walks {@link State#listBackEdges()} lazily instead, which is
 * optimized for reducing memory allocation and CPU usage.
 */
class StateToFlexPathMapper {

  /**
   * Walk the state chain and collect edges in chronological order (origin → destination), summing
   * up the distance along the way.
   */
  static FlexPath map(LazyStreetPath path) {
    // computing the linestring from the graph path is a surprisingly expensive operation
    // so we delay it until it's actually needed. since most flex paths are never shown to the user
    // this improves performance quite a bit.

    return new FlexPath(
      (int) path.getTraversalDistanceMeters(),
      (int) path.getElapsedTimeSeconds(),
      path::getGeometry
    );
  }
}
