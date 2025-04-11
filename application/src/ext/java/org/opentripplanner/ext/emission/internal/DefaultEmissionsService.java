package org.opentripplanner.ext.emission.internal;

import jakarta.inject.Inject;
import java.util.Optional;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.ext.emission.EmissionsService;
import org.opentripplanner.framework.model.Gram;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.Sandbox;

@Sandbox
public class DefaultEmissionsService implements EmissionsService {

  private final EmissionRepository emissionRepository;

  @Inject
  public DefaultEmissionsService(EmissionRepository emissionRepository) {
    this.emissionRepository = emissionRepository;
  }

  @Override
  public Optional<Emission> getEmissionPerMeterForRoute(FeedScopedId feedScopedRouteId) {
    return this.emissionRepository.getCO2EmissionsById(feedScopedRouteId).map(co2 ->
        new Emission(new Gram(co2))
      );
  }

  @Override
  public Optional<Emission> getEmissionPerMeterForCar() {
    return this.emissionRepository.getCarAvgCo2PerMeter().map(co2 -> new Emission(new Gram(co2)));
  }
}
