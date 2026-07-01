package org.opentripplanner.street.search;

import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.opentripplanner.astar.spi.RemainingWeightHeuristic;
import org.opentripplanner.street.geometry.SphericalDistanceLibrary;
import org.opentripplanner.street.model.VehicleRoutingOptimizeType;
import org.opentripplanner.street.model.edge.Edge;
import org.opentripplanner.street.model.edge.FreeEdge;
import org.opentripplanner.street.model.vertex.Vertex;
import org.opentripplanner.street.search.request.BikeRequest;
import org.opentripplanner.street.search.request.ScooterRequest;
import org.opentripplanner.street.search.request.StreetSearchRequest;
import org.opentripplanner.street.search.request.TimeSlopeSafetyTriangle;
import org.opentripplanner.street.search.request.WalkRequest;
import org.opentripplanner.street.search.state.State;
import org.opentripplanner.street.search.state.VehicleRentalState;
import org.opentripplanner.street.service.StreetLimitationParametersService;

/**
 * <p>A Euclidean remaining weight strategy.</p>
 *
 * <p>Generates an admissible heuristic by simply calculating the direct distance to the target,
 * and calculating the minimum possible weight for that. Any path which diverts from the straight
 * line or does not travel at the maximum possible "speed" for the given modes will have a larger
 * weight. "Speed" is in scare quotes because we have to consider the effect of safety on the
 * cost for non-car modes, so instead of maximum speed we actually use minimum cost per distance,
 * where cost is calculated in a mode-appropriate way.</p>
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
  public void initialize(Set<Vertex> toVertices, StreetSearchRequest req) {
    Vertex target = toVertices.iterator().next();
    minimumCostPerDistance = getMinimumCostPerDistance(req);
    walkingCostPerDistance = getWalkingCostPerDistance(req.walk());
    this.arriveBy = req.arriveBy();

    if (target.getDegreeIn() == 1) {
      Edge edge = target.getIncoming().iterator().next();
      if (edge instanceof FreeEdge) {
        target = edge.getFromVertex();
      }
    }

    lat = target.getLat();
    lon = target.getLon();
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

  /**
   * @return the minimum of the lower limits of pace for the applicable modes
   */
  private double getMinimumCostPerDistance(StreetSearchRequest req) {
    var streetMode = req.mode();
    double drivingPace = streetMode.includesDriving()
      ? 1.0 / streetLimitationParametersService.maxCarSpeed()
      : Double.MAX_VALUE;
    double cyclingPace = streetMode.includesBiking()
      ? getCyclingCostPerDistance(req.bike())
      : Double.MAX_VALUE;
    double scooterPace = streetMode.includesScooter()
      ? getScooterCostPerDistance(req.scooter())
      : Double.MAX_VALUE;
    double walkingPace = streetMode.includesWalking()
      ? getWalkingCostPerDistance(req.walk())
      : Double.MAX_VALUE;
    return Stream.of(drivingPace, cyclingPace, scooterPace, walkingPace)
      .min(Comparator.comparingDouble(x -> x))
      .orElseThrow();
  }

  private double getWalkingCostPerDistance(WalkRequest walkRequest) {
    return getCostPerDistance(
      walkRequest.speed(),
      walkRequest.reluctance(),
      scaleSafety(streetLimitationParametersService.getBestWalkSafety(), walkRequest.safetyFactor())
    );
  }

  private double getCyclingCostPerDistance(BikeRequest bikeRequest) {
    return getCostPerDistance(
      bikeRequest.speed(),
      bikeRequest.reluctance(),
      getEffectiveSafetyForOptimizeType(
        bikeRequest.optimizeType(),
        bikeRequest.optimizeTriangle(),
        streetLimitationParametersService.getBestBikeSafety()
      )
    );
  }

  private double getScooterCostPerDistance(ScooterRequest scooterRequest) {
    return getCostPerDistance(
      scooterRequest.speed(),
      scooterRequest.reluctance(),
      getEffectiveSafetyForOptimizeType(
        scooterRequest.optimizeType(),
        scooterRequest.optimizeTriangle(),
        streetLimitationParametersService.getBestBikeSafety()
      )
    );
  }

  private double getEffectiveSafetyForOptimizeType(
    VehicleRoutingOptimizeType optimizeType,
    TimeSlopeSafetyTriangle triangle,
    double safety
  ) {
    return switch (optimizeType) {
      case SHORTEST_DURATION -> 1.0;
      case SAFE_STREETS -> safety;
      case FLAT_STREETS -> 1.0;
      case TRIANGLE -> scaleSafety(
        safety,
        Objects.requireNonNull(
          triangle,
          "triangle must not be null if vehicleRoutingOptimizeType is TRIANGLE."
        ).safety()
      );
    };
  }

  private static double scaleSafety(double safety, double safetyFactor) {
    return 1 + (safety - 1) * safetyFactor;
  }

  private static double getCostPerDistance(
    double speed,
    double reluctance,
    double effectiveMinimumSafety
  ) {
    return (1.0 / speed) * reluctance * effectiveMinimumSafety;
  }
}
