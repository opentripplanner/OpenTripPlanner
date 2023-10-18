package org.opentripplanner.ext.emissions;

import jakarta.inject.Inject;
import java.util.Optional;
import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.transit.model.framework.FeedScopedId;

@Sandbox
public class EmissionsDataService implements EmissionsService {

  private EmissionsDataModel emissionsDataModel;

  public EmissionsDataService() {}

  @Inject
  public EmissionsDataService(EmissionsDataModel emissionsDataModel) {
    this.emissionsDataModel = emissionsDataModel;
  }

  @Override
  public Optional<Double> getEmissionsPerMeterForRoute(
    FeedScopedId feedScopedRouteId,
    EmissionType emissionType
  ) {
    switch (emissionType) {
      case CO2:
        return this.emissionsDataModel.getCO2EmissionsById(feedScopedRouteId);
      default:
        return Optional.empty();
    }
  }

  @Override
  public Optional<Double> getCarEmissionsPerMeter(EmissionType emissionType) {
    switch (emissionType) {
      case CO2:
        return Optional.ofNullable(this.emissionsDataModel.getCarAvgCo2PerMeter());
      default:
        return Optional.empty();
    }
  }
}
