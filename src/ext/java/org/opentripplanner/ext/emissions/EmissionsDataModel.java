package org.opentripplanner.ext.emissions;

import jakarta.inject.Inject;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Container for emissions data.
 */
public class EmissionsDataModel implements Serializable {

  private Map<FeedScopedId, Double> co2Emissions;
  private Double carAvgCo2PerMeter;

  @Inject
  public EmissionsDataModel() {}

  public EmissionsDataModel(Map<FeedScopedId, Double> co2Emissions, double carAvgCo2PerMeter) {
    this.co2Emissions = co2Emissions;
    this.carAvgCo2PerMeter = carAvgCo2PerMeter;
  }

  public void setCo2Emissions(Map<FeedScopedId, Double> co2Emissions) {
    this.co2Emissions = co2Emissions;
  }

  public void setCarAvgCo2PerMeter(double carAvgCo2PerMeter) {
    this.carAvgCo2PerMeter = carAvgCo2PerMeter;
  }

  public Optional<Double> getCarAvgCo2PerMeter() {
    return Optional.ofNullable(this.carAvgCo2PerMeter);
  }

  public Optional<Double> getCO2EmissionsById(FeedScopedId feedScopedRouteId) {
    return Optional.ofNullable(this.co2Emissions.get(feedScopedRouteId));
  }
}
