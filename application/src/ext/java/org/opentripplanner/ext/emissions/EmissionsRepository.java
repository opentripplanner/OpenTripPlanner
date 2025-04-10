package org.opentripplanner.ext.emissions;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Repository for emissions data.
 */
public interface EmissionsRepository extends Serializable {
  void setCo2Emissions(Map<FeedScopedId, Double> co2Emissions);

  void setCarAvgCo2PerMeter(double carAvgCo2PerMeter);

  Optional<Double> getCarAvgCo2PerMeter();

  Optional<Double> getCO2EmissionsById(FeedScopedId feedScopedRouteId);
}
