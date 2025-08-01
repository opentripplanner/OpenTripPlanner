package org.opentripplanner.street.search.strategy;

import static org.opentripplanner.street.model.edge.StreetEdgeReluctanceCalculator.getSafetyForSafestStreet;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;
import org.opentripplanner.astar.spi.RemainingWeightHeuristic;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.preference.RoutingPreferences;
import org.opentripplanner.routing.api.request.preference.TimeSlopeSafetyTriangle;
import org.opentripplanner.routing.core.VehicleRoutingOptimizeType;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.FreeEdge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.VehicleRentalState;
import org.opentripplanner.street.service.StreetLimitationParametersService;

/**
 * A Euclidean remaining weight strategy.
 */
public class EuclideanRemainingWeightHeuristic implements RemainingWeightHeuristic<State> {

  private double lat;
  private double lon;
  private double minimumCostPerDistance;
  private double walkingCostPerDistance;

  private boolean arriveBy;
  private final StreetLimitationParametersService streetLimitationParametersService;

  public EuclideanRemainingWeightHeuristic() {
    this(StreetLimitationParametersService.DEFAULT);
  }

  public EuclideanRemainingWeightHeuristic(
    StreetLimitationParametersService streetLimitationParametersService
  ) {
    this.streetLimitationParametersService = streetLimitationParametersService;
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
    minimumCostPerDistance = getMinimumCostPerDistance(preferences, streetMode);
    walkingCostPerDistance = getWalkingCostPerDistance(preferences);
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

  private double scaleSafety(double safety, double safetyFactor) {
    return 1 + (safety - 1) * safetyFactor;
  }

  private double getEffectiveSafetyForOptimization(
    VehicleRoutingOptimizeType optimizeType,
    TimeSlopeSafetyTriangle triangle,
    double safety
  ) {
    return switch (optimizeType) {
      case SHORTEST_DURATION -> 1.0;
      case SAFE_STREETS -> safety;
      case FLAT_STREETS -> 1.0;
      case SAFEST_STREETS -> getSafetyForSafestStreet(safety);
      case TRIANGLE -> scaleSafety(safety, triangle.safety());
    };
  }

  /**
   * @return the minimum of (pace × reluctance × safety) for the applicable modes
   */
  private double getMinimumCostPerDistance(RoutingPreferences preferences, StreetMode streetMode) {
    double drivingPace = streetMode.includesDriving()
      ? 1.0 / streetLimitationParametersService.getMaxCarSpeed()
      : Double.MAX_VALUE;
    double bestBikeSafety = streetLimitationParametersService.getBestBikeSafety();
    double cyclingPace = streetMode.includesBiking()
      ? (1.0 / preferences.bike().speed()) *
      preferences.bike().reluctance() *
      getEffectiveSafetyForOptimization(
        preferences.bike().optimizeType(),
        preferences.bike().optimizeTriangle(),
        bestBikeSafety
      )
      : Double.MAX_VALUE;
    double scooterPace = streetMode.includesScooter()
      ? (1.0 / preferences.scooter().speed()) *
      preferences.scooter().reluctance() *
      getEffectiveSafetyForOptimization(
        preferences.scooter().optimizeType(),
        preferences.scooter().optimizeTriangle(),
        bestBikeSafety
      )
      : Double.MAX_VALUE;
    double walkingPace = streetMode.includesWalking()
      ? getWalkingCostPerDistance(preferences)
      : Double.MAX_VALUE;
    return Stream.of(drivingPace, cyclingPace, scooterPace, walkingPace)
      .min(Comparator.comparingDouble(x -> x))
      .orElseThrow();
  }

  private double getWalkingCostPerDistance(RoutingPreferences preferences) {
    return (
      (1.0 / preferences.walk().speed()) *
      preferences.walk().reluctance() *
      scaleSafety(
        streetLimitationParametersService.getBestWalkSafety(),
        preferences.walk().safetyFactor()
      )
    );
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

    final double costPerDistance = useWalkSpeed ? walkingCostPerDistance : minimumCostPerDistance;
    return euclideanDistance * costPerDistance;
  }
}
