package org.opentripplanner.routing.algorithm.astar.strategies;

import java.util.Set;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.VehicleRentalState;
import org.opentripplanner.routing.edgetype.FreeEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;

/**
 * A Euclidean remaining weight strategy.
 */
public class EuclideanRemainingWeightHeuristic implements RemainingWeightHeuristic {

  private double lat;
  private double lon;
  private double maxStreetSpeed;
  private double walkingSpeed;
  private boolean arriveBy;

  // TODO This currently only uses the first toVertex. If there are multiple toVertices, it will
  //      not work correctly.
  @Override
  public void initialize(
    StreetMode streetMode,
    Set<Vertex> fromVertices,
    Set<Vertex> toVertices,
    boolean arriveBy,
    RoutingPreferences preferences
  ) {
    Vertex target = toVertices.iterator().next();
    maxStreetSpeed = getStreetSpeedUpperBound(preferences, streetMode);
    walkingSpeed = preferences.walk().speed();
    this.arriveBy = arriveBy;

    if (target.getDegreeIn() == 1) {
      Edge edge = target.getIncoming().iterator().next();
      if (edge instanceof FreeEdge) {
        target = edge.getFromVertex();
      }
    }

    lat = target.getLat();
    lon = target.getLon();
  }

  /** @return The highest speed for all possible road-modes. */
  private double getStreetSpeedUpperBound(RoutingPreferences preferences, StreetMode streetMode) {
    // Assume carSpeed > bikeSpeed > walkSpeed
    if (streetMode.includesDriving()) {
      return preferences.car().speed();
    }
    if (streetMode.includesBiking()) {
      return preferences.bike().speed();
    }
    return preferences.walk().speed();
  }

  /**
   * On a non-transit trip, the remaining weight is simply distance / street speed.
   */
  @Override
  public double estimateRemainingWeight(State s) {
    Vertex sv = s.getVertex();
    double euclideanDistance = SphericalDistanceLibrary.fastDistance(
      sv.getLat(),
      sv.getLon(),
      lat,
      lon
    );

    // After parking or finishing the rental of a vehicle, you can't ever move faster than walking speed.
    boolean useWalkSpeed;
    if (arriveBy) {
      useWalkSpeed = s.getVehicleRentalState() == VehicleRentalState.BEFORE_RENTING;
    } else {
      useWalkSpeed =
        s.isVehicleParked() || s.getVehicleRentalState() == VehicleRentalState.HAVE_RENTED;
    }

    final double streetSpeed = useWalkSpeed ? walkingSpeed : maxStreetSpeed;
    return euclideanDistance / streetSpeed;
  }
}
