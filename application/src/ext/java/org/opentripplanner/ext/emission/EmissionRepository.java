package org.opentripplanner.ext.emission;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Repository for emission data.
 */
public interface EmissionRepository extends Serializable {
  Optional<Double> getCarAvgCo2PerMeter();

  void setCarAvgCo2PerMeter(double carAvgCo2PerMeter);

  /**
   * Return average pasenger emissions per meter for a given route.
   */
  Emission routePassengerEmissionsPerMeter(FeedScopedId routeId);

  void addRouteEmissions(Map<FeedScopedId, Emission> routeAvgCo2Emissions);
}
