package org.opentripplanner.service.vehiclerental.street.geofencing;

import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.StateEditor;

/**
 * Abstraction over edge traversal for geofencing enforcement. Allows enforcement implementations
 * to traverse the current edge in any mode without depending on StreetEdge or exposing its
 * private methods.
 */
@FunctionalInterface
public interface EdgeTraversal {
  /**
   * Traverse the current edge, returning a StateEditor ready for modification.
   *
   * @param s0   the state to traverse from
   * @param mode the traverse mode (WALK for drop branches, rental mode for ride branches)
   * @return a StateEditor for the traversed state, or null if traversal is not possible
   */
  StateEditor traverse(State s0, TraverseMode mode);
}
