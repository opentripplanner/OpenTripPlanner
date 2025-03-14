package org.opentripplanner.ext.emissions.internal;

import jakarta.inject.Inject;
import java.util.Optional;
import org.opentripplanner.ext.emissions.EmissionsDataModel;
import org.opentripplanner.ext.emissions.EmissionsService;
import org.opentripplanner.framework.model.Gram;
import org.opentripplanner.model.plan.Emissions;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.Sandbox;

@Sandbox
public class DefaultEmissionsService implements EmissionsService {

  private final EmissionsDataModel emissionsDataModel;

  @Inject
  public DefaultEmissionsService(EmissionsDataModel emissionsDataModel) {
    this.emissionsDataModel = emissionsDataModel;
  }

  @Override
  public Optional<Emissions> getEmissionsPerMeterForRoute(FeedScopedId feedScopedRouteId) {
    return this.emissionsDataModel.getCO2EmissionsById(feedScopedRouteId).map(co2 ->
        new Emissions(new Gram(co2))
      );
  }

  @Override
  public Optional<Emissions> getEmissionsPerMeterForCar() {
    return this.emissionsDataModel.getCarAvgCo2PerMeter().map(co2 -> new Emissions(new Gram(co2)));
  }
}
