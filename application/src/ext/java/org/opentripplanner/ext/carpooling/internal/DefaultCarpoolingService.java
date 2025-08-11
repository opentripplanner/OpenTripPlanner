package org.opentripplanner.ext.carpooling.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.CarpoolingService;
import org.opentripplanner.ext.carpooling.model.CarpoolTransitLeg;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.framework.geometry.SphericalDistanceLibrary;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.response.InputField;
import org.opentripplanner.routing.api.response.RoutingError;
import org.opentripplanner.routing.api.response.RoutingErrorCode;
import org.opentripplanner.routing.error.RoutingValidationException;
import org.opentripplanner.transit.model.site.AreaStop;

public class DefaultCarpoolingService implements CarpoolingService {

  private final CarpoolingRepository repository;

  public DefaultCarpoolingService(CarpoolingRepository repository) {
    this.repository = repository;
  }

  /**
   * TERMINOLOGY
   * - Boarding and alighting area stops
   *
   *
   * ALGORITHM OUTLINE
   *
   * <pre>
   *   DIRECT_DISTANCE = SphericalDistanceLibrary.fastDistance(fromLocation, toLocation)
   *   // 3000m is about 45 minutes of walking
   *   MAX_WALK_DISTANCE = max(DIRECT_DISTANCE, 3000m)
   *   MAX_COST = MAX_WALK_DISTANCE * walkReluctance + DIRECT_DISTANCE - MAX_WALK_DISTANCE
   *
   * Search for access / egress candidates (AreaStops) using
   * - accessDistance = SphericalDistanceLibrary.fastDistance(fromLocation, stop.center);
   * - Drop candidates where accessDistance greater then MAX_WALK_DISTANCE and is not within time constraints
   * - egressDistance = SphericalDistanceLibrary.fastDistance(toLocation, stop.center);
   * - Drop candidates where (accessDistance + egressDistance) greater then MAX_WALK_DISTANCE (no time check)
   * - Sort candidates on estimated cost, where we use direct distance instead of actual distance
   *
   * FOR EACH CANDIDATE (C)
   * - Use AStar to find the actual distance for:
   *   - access path
   *   - transit path
   *   - egress path
   * - Drop candidates where (access+carpool+egress) cost > MAX_COST
   * [- Abort when no more optimal results can be obtained (pri2)]
   *
   * Create Itineraries for the top 3 results and return
   * </pre>
   */
  @Override
  public List<Itinerary> route(RouteRequest request) throws RoutingValidationException {
    if (Objects.requireNonNull(request.from()).lat == null || Objects.requireNonNull(request.from()).lng == null) {
      throw new RoutingValidationException(
        List.of(new RoutingError(RoutingErrorCode.LOCATION_NOT_FOUND, InputField.FROM_PLACE))
      );
    }
    if (Objects.requireNonNull(request.to()).lat == null || Objects.requireNonNull(request.to()).lng == null
    ) {
      throw new RoutingValidationException(
        List.of(new RoutingError(RoutingErrorCode.LOCATION_NOT_FOUND, InputField.TO_PLACE))
      );
    }

    double fromLat = request.from().lat;
    double fromLng = request.from().lng;
    double toLat = request.to().lat;
    double toLng = request.to().lng;

    // Calculate direct distance and maximum walk distance
    double directDistance = SphericalDistanceLibrary.fastDistance(fromLat, fromLng, toLat, toLng);

    // 3000m is about 45 minutes of walking
    double maxWalkDistance = Math.max(directDistance, 3000.0);
    double walkReluctance = request.preferences().walk().reluctance();
    double maxCost = maxWalkDistance * walkReluctance + directDistance - maxWalkDistance;

    // Find candidate area stops (carpooling boarding/alighting points)
    Collection<CarpoolTrip> availableTrips = repository.getCarpoolTrips();
    List<CarpoolTripCandidate> candidates = new ArrayList<>();

    for (CarpoolTrip trip : availableTrips) {
      AreaStop boardingArea = trip.getBoardingArea();
      AreaStop alightingArea = trip.getAlightingArea();

      if (boardingArea != null && alightingArea != null) {
        // Calculate access distance from origin to boarding area
        double accessDistance = SphericalDistanceLibrary.fastDistance(
          fromLat,
          fromLng,
          boardingArea.getCoordinate().latitude(),
          boardingArea.getCoordinate().longitude()
        );

        // Drop candidates where access distance is too far
        if (accessDistance > maxWalkDistance) {
          continue;
        }

        // Calculate egress distance from alighting area to destination
        double egressDistance = SphericalDistanceLibrary.fastDistance(
          alightingArea.getCoordinate().latitude(),
          alightingArea.getCoordinate().longitude(),
          toLat,
          toLng
        );

        // Drop candidates where total walking distance is too far
        if ((accessDistance + egressDistance) > maxWalkDistance) {
          continue;
        }

        // Calculate estimated cost using direct distances
        double estimatedCost =
          accessDistance * walkReluctance + directDistance + egressDistance * walkReluctance;

        candidates.add(
          new CarpoolTripCandidate(trip, accessDistance, egressDistance, estimatedCost)
        );
      }
    }

    // Sort candidates by estimated cost
    candidates.sort(Comparator.comparingDouble(a -> a.estimatedCost));

    // Create itineraries for the top candidates (limit to 3)
    List<Itinerary> itineraries = new ArrayList<>();
    int maxResults = Math.min(candidates.size(), 3);

    for (int i = 0; i < maxResults; i++) {
      CarpoolTripCandidate candidate = candidates.get(i);

      // For now, create a simplified itinerary with basic structure
      // TODO: Implement actual A* routing for precise distances and times
      Itinerary itinerary = createItineraryForCandidate(candidate, request);
      if (itinerary != null) {
        itineraries.add(itinerary);
      }
    }

    return itineraries;
  }

  private Itinerary createItineraryForCandidate(
    CarpoolTripCandidate candidate,
    RouteRequest request
  ) {
    // This is a simplified implementation - would need full A* routing for production
    CarpoolTrip trip = candidate.trip;

    // Create basic itinerary structure
    // In a full implementation, this would use A* to calculate actual walking paths
    // and create proper legs with geometry and timing

    // Create a carpooling transit leg
    CarpoolTransitLeg carpoolLeg = CarpoolTransitLeg.of()
      .withStartTime(trip.getStartTime())
      .withEndTime(trip.getEndTime())
      .withTrip(trip.getTrip())
      .withGeneralizedCost((int) candidate.estimatedCost)
      .build();

    // TODO: Add walking leg from origin to pickup area
    // TODO: Add carpooling transit leg
    // TODO: Add walking leg from dropoff area to destination

    // For now, return null to indicate incomplete implementation
    return Itinerary.ofDirect(List.of(carpoolLeg))
      .withGeneralizedCost(Cost.costOfSeconds(carpoolLeg.generalizedCost()))
      .build();
  }

  private static class CarpoolTripCandidate {

    final CarpoolTrip trip;
    final double accessDistance;
    final double egressDistance;
    final double estimatedCost;

    CarpoolTripCandidate(
      CarpoolTrip trip,
      double accessDistance,
      double egressDistance,
      double estimatedCost
    ) {
      this.trip = trip;
      this.accessDistance = accessDistance;
      this.egressDistance = egressDistance;
      this.estimatedCost = estimatedCost;
    }
  }
}
