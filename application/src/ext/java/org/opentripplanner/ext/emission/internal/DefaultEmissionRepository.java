package org.opentripplanner.ext.emission.internal;

import java.util.Map;
import java.util.Optional;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class DefaultEmissionRepository implements EmissionRepository {

  private Map<FeedScopedId, Double> co2Emissions;
  private Double carAvgCo2PerMeter;

  public DefaultEmissionRepository() {}

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
