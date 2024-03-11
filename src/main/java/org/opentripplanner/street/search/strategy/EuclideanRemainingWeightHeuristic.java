package org.opentripplanner.street.search.strategy;

import java.util.Set;
import org.opentripplanner.astar.spi.RemainingWeightHeuristic;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.street.model.StreetConstants;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.FreeEdge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.VehicleRentalState;

/**
 * A Euclidean remaining weight strategy.
 */
public class EuclideanRemainingWeightHeuristic implements RemainingWeightHeuristic<State> {

  private static final Float DEFAULT_MAX_CAR_SPEED = StreetConstants.DEFAULT_MAX_CAR_SPEED;

  private double lat;
  private double lon;
  private double maxStreetSpeed;
  private double walkingSpeed;
  private boolean arriveBy;
  private float maxCarSpeed;

  public EuclideanRemainingWeightHeuristic() {
    this(DEFAULT_MAX_CAR_SPEED);
  }

  public EuclideanRemainingWeightHeuristic(Float maxCarSpeed) {
    this.maxCarSpeed = maxCarSpeed != null ? maxCarSpeed : DEFAULT_MAX_CAR_SPEED;
  }

  // TODO This currently only uses the first toVertex. If there are multiple toVertices, it will
  //      not work correctly.
  public void initialize(
    StreetMode streetMode,
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
      return maxCarSpeed;
    }
    if (streetMode.includesBiking()) {
      return preferences.bike().speed();
    }
    if (streetMode.includesScooter()) {
      return preferences.scooter().speed();
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
