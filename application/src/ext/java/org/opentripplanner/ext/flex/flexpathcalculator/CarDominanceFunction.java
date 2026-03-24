package org.opentripplanner.ext.flex.flexpathcalculator;

import java.io.Serializable;
import org.opentripplanner.astar.spi.DominanceFunction;
import org.opentripplanner.street.search.state.State;

/**
 * A class that determines when one search branch prunes another at the same Vertex, and ultimately
 * which solutions are retained. In the general case, one branch does not necessarily win out over
 * the other, i.e. multiple states can coexist at a single Vertex.
 * <p>
 * Even functions where one state always wins (least weight, fastest travel time) are applied within
 * a multi-state shortest path tree because bike rental, car or bike parking, and turn restrictions
 * all require multiple incomparable states at the same vertex. These need the graph to be
 * "replicated" into separate layers, which is achieved by applying the main dominance logic (lowest
 * weight, lowest cost, Pareto) conditionally, only when the two states have identical bike/car/turn
 * direction status.
 * <p>
 * Dominance functions are serializable so that routing requests may passed between machines in
 * different JVMs, for instance in OTPA Cluster.
 */
public class CarDominanceFunction implements Serializable, DominanceFunction<State> {

  /**
   * For bike rental, parking, and approaching turn-restricted intersections states are
   * incomparable: they exist on separate planes. The core state dominance logic is wrapped in this
   * public function and only applied when the two states have all these variables in common (are on
   * the same plane).
   */
  @Override
  public boolean betterOrEqualAndComparable(State a, State b) {
    // Since a Vertex may be arrived at using a no-thru restricted path and one without such
    // restrictions, treat the two as separate so one doesn't dominate the other.
    if (a.hasEnteredNoThruTrafficArea() != b.hasEnteredNoThruTrafficArea()) {
      return false;
    }

    // These two states are comparable (they are on the same "plane" or "copy" of the graph).
    return a.getElapsedTimeSeconds() <= b.getElapsedTimeSeconds();
  }


}
