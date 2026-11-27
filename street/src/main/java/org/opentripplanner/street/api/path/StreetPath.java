package org.opentripplanner.street.api.path;

import java.time.Duration;
import java.util.List;
import org.opentripplanner.core.model.basic.Cost;

/// A path within the street network, the result of a street search.
///
/// The states of the search are deliberately not exposed. To get the details of the path, split
/// it into [StreetLeg]s using [#computeLegs(double)].
public interface StreetPath {
  /// The generalized cost (weight) of traversing the path.
  Cost generalizedCost();

  /// The time it takes to traverse the path, rounded up to whole seconds.
  Duration duration();

  /// The total elevation gained and lost along the path.
  ElevationChange elevationChange();

  /// Whether the path ends while still renting a vehicle picked up at a rental station.
  boolean arrivesWithRentedVehicleFromStation();

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
