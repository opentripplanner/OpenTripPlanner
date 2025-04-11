package org.opentripplanner.ext.emission.internal;

import jakarta.inject.Inject;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.emission.EmissionService;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.transit.model.framework.FeedScopedId;
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
  public Emission calculateEmissionPerMeterForRoute(
    FeedScopedId feedScopedRouteId,
    double distance_m
  ) {
    // Calculate emissions based on average passenger emisions for the route
    var value = emissionRepository.routePassengerEmissionsPerMeter(feedScopedRouteId);
    if (!value.isZero()) {
      return value.multiply(distance_m);
    }
    return Emission.ZERO;
  }
}
