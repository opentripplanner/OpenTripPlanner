package org.opentripplanner.ext.emission.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.ext.emission.EmissionRepository;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class DefaultEmissionRepository implements EmissionRepository {

  private Double carAvgCo2PerMeter;
  private Map<FeedScopedId, Emission> routeEmissions = new HashMap<>();

  public DefaultEmissionRepository() {}

  public void setCarAvgCo2PerMeter(double carAvgCo2PerMeter) {
    this.carAvgCo2PerMeter = carAvgCo2PerMeter;
  }

  public Optional<Double> getCarAvgCo2PerMeter() {
    return Optional.ofNullable(this.carAvgCo2PerMeter);
  }

  @Override
  public void addRouteEmissions(Map<FeedScopedId, Emission> routeAvgCo2Emissions) {
    this.routeEmissions.putAll(routeAvgCo2Emissions);
  }

  @Override
  public Emission routePassengerEmissionsPerMeter(FeedScopedId routeId) {
    var value = this.routeEmissions.get(routeId);
    return value == null ? Emission.ZERO : value;
  }
}
