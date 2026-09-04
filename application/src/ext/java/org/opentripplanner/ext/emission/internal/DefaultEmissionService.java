package org.opentripplanner.ext.emission.internal;

import jakarta.inject.Inject;
import java.util.Optional;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.emission.EmissionService;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.lang.Sandbox;

@Sandbox
public class DefaultEmissionService implements EmissionService {

  private final EmissionRepository emissionRepository;

  @Inject
  public DefaultEmissionService(EmissionRepository emissionRepository) {
    this.emissionRepository = emissionRepository;
  }

  @Override
  public Emission calculateCarPassengerEmission(double distance_m) {
    return emissionRepository.carAvgPassengerEmissionPerMeter().multiply(distance_m);
  }

  @Override
  public Optional<Emission> calculateTransitPassengerEmissionForRoute(
    Route route,
    double distance_m
  ) {
    return emissionRepository
      .routePassengerEmissionsPerMeter(route.getId())
      .map(e -> e.multiply(distance_m));
  }

  @Override
  public Optional<Emission> calculateTransitPassengerEmissionForTripHops(
    Trip trip,
    int boardStopPosInPattern,
    int alightStopPosInPattern,
    double distance_m
  ) {
    var routeEmission = calculateTransitPassengerEmissionForRoute(trip.getRoute(), distance_m);
    if (routeEmission.isPresent()) {
      return routeEmission;
    }

    // Calculate emissions based the emissions for each section of a trip, if not found
    // zero is returned.
    var emission = emissionRepository.tripPatternEmissions(trip.getId());
    if (emission != null) {
      return Optional.of(emission.section(boardStopPosInPattern, alightStopPosInPattern));
    }
    return Optional.empty();
  }
}
