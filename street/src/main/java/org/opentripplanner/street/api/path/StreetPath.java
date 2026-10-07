package org.opentripplanner.street.api.path;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.core.model.basic.Distance;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.search.state.State;

/// A path within the street network, the result of a street search.
///
/// The states of the search are deliberately not exposed. To get the details of the path, split
/// it into [StreetLeg]s using [#computeLegs(double)].
public interface StreetPath {
  /// Build a chronologically ordered path by following the back-state chain of `finalState` all
  /// the way back to the origin of the search. Paths from arriveBy searches are reversed, so the
  /// path always runs forward in time.
  static StreetPath of(State finalState) {
    return new DefaultStreetPath(finalState);
  }

  /// Create a path from the states and edges of a traversal which already runs forward in time.
  /// There must be exactly one edge between each pair of consecutive states.
  static StreetPath of(List<State> states, List<Edge> edges) {
    return new DefaultStreetPath(states, edges);
  }

  /// The generalized cost (weight) of traversing the path.
  Cost generalizedCost();

  /// The time it takes to traverse the path, rounded up to whole seconds.
  Duration duration();

  /// The distance traversed along the path.
  Distance traversalDistance();

  /// Split this path into legs. Each change of street mode, like picking up a rental vehicle or
  /// parking a car, starts a new leg. Walking a bike does not.
  ///
  /// The expensive parts of each leg, like the turn-by-turn directions, are computed lazily when
  /// accessed.
  ///
  /// @param ellipsoidToGeoidDifference The difference between the ellipsoid and the geoid
  ///                                   elevation of the graph, applied to the elevations of the
  ///                                   legs if requested.
  List<StreetLeg> computeLegs(double ellipsoidToGeoidDifference);
}
