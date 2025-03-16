package org.opentripplanner.ext.emissions.internal;

import jakarta.inject.Inject;
import java.util.Optional;
import org.opentripplanner.ext.emissions.EmissionsRepository;
import org.opentripplanner.ext.emissions.EmissionsService;
import org.opentripplanner.framework.model.Gram;
import org.opentripplanner.model.plan.Emissions;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.Sandbox;

@Sandbox
public class DefaultEmissionsService implements EmissionsService {

  private final EmissionsRepository emissionsRepository;

  @Inject
  public DefaultEmissionsService(EmissionsRepository emissionsRepository) {
    this.emissionsRepository = emissionsRepository;
  }

  @Override
  public Optional<Emissions> getEmissionsPerMeterForRoute(FeedScopedId feedScopedRouteId) {
    return this.emissionsRepository.getCO2EmissionsById(feedScopedRouteId).map(co2 ->
        new Emissions(new Gram(co2))
      );
  }

  @Override
  public Optional<Emissions> getEmissionsPerMeterForCar() {
    return this.emissionsRepository.getCarAvgCo2PerMeter().map(co2 -> new Emissions(new Gram(co2)));
  }
}
