package org.opentripplanner.ext.emission;

import java.io.Serializable;
import java.util.Map;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Repository for emission data.
 */
public interface EmissionRepository extends Serializable {
  /**
   * Return the configured average emisions per meter. If not set ZERO is retuned.
   */
  Emission carAvgPassengerEmissionPerMeter();

  void setCarAvgCo2PerMeter(double carAvgCo2PerMeter);

  /**
   * Return average pasenger emissions per meter for a given route.
   */
  Emission routePassengerEmissionsPerMeter(FeedScopedId routeId);

  void addRouteEmissions(Map<FeedScopedId, Emission> routeAvgCo2Emissions);
}
