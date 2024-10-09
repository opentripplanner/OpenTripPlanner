package org.opentripplanner.ext.emissions;

import jakarta.inject.Inject;
import java.util.Optional;
import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.framework.model.Grams;
import org.opentripplanner.model.plan.Emissions;
import org.opentripplanner.transit.model.framework.FeedScopedId;

@Sandbox
public class DefaultEmissionsService implements EmissionsService {

  private final EmissionsDataModel emissionsDataModel;

  @Inject
  public DefaultEmissionsService(EmissionsDataModel emissionsDataModel) {
    this.emissionsDataModel = emissionsDataModel;
  }

  @Override
  public Optional<Emissions> getEmissionsPerMeterForRoute(FeedScopedId feedScopedRouteId) {
    Optional<Double> co2Emissions = this.emissionsDataModel.getCO2EmissionsById(feedScopedRouteId);
    return co2Emissions.isPresent()
      ? Optional.of(new Emissions(new Grams(co2Emissions.get())))
      : Optional.empty();
  }

  @Override
  public Optional<Emissions> getEmissionsPerMeterForCar() {
    Optional<Double> co2Emissions = this.emissionsDataModel.getCarAvgCo2PerMeter();
    return co2Emissions.isPresent()
      ? Optional.of(new Emissions(new Grams(co2Emissions.get())))
      : Optional.empty();
  }
}
