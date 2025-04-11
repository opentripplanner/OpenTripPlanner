package org.opentripplanner.ext.emission.internal;

import jakarta.inject.Inject;
import java.util.Optional;
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
  public Optional<Emission> getEmissionPerMeterForRoute(FeedScopedId feedScopedRouteId) {
    return this.emissionRepository.getCO2EmissionsById(feedScopedRouteId).map(Emission::co2_g);
  }

  @Override
  public Optional<Emission> getEmissionPerMeterForCar() {
    return this.emissionRepository.getCarAvgCo2PerMeter().map(Emission::co2_g);
  }
}
