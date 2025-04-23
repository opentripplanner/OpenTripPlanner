package org.opentripplanner.ext.emission;

import java.io.Serializable;
import java.util.Map;
import org.opentripplanner.ext.emission.model.TripPatternEmission;
import org.opentripplanner.framework.model.Gram;
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

  void setCarAvgCo2PerMeter(Gram carAvgCo2PerMeter);

  /**
   * Return average pasenger emissions per meter for a given route.
   */
  Emission routePassengerEmissionsPerMeter(FeedScopedId routeId);

  void addRouteEmissions(Map<FeedScopedId, Emission> routeAvgCo2Emissions);

  /**
   * Return emissions for all legs/sections in the trip pattern for a given trip. The same
   * trip-pattern may have multiple diffrent emissions depending on the viechle(s) operating the
   * trip on a given date, but OTP does not keep information about the viechles, so we calculate
   * the average emissions for each leg for a given trip.
   */
  TripPatternEmission tripPatternEmissions(FeedScopedId tripId);

  void addTripPatternEmissions(Map<FeedScopedId, TripPatternEmission> tripPatternEmissions);
}
