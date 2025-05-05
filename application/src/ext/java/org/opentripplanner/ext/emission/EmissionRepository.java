package org.opentripplanner.ext.emission;

import java.io.Serializable;
import java.util.Map;
import org.opentripplanner.ext.emission.model.EmissionSummary;
import org.opentripplanner.ext.emission.model.TripPatternEmission;
import org.opentripplanner.framework.model.Gram;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * Repository for emission data.
 */
public interface EmissionRepository extends Serializable {
  /**
   * Return the configured average emissions per meter. If not set ZERO is returned.
   */
  Emission carAvgPassengerEmissionPerMeter();

  void setCarAvgCo2PerMeter(Gram carAvgCo2PerMeter);

  /**
   * Return average pasenger emissions per meter for a given route.
   */
  Emission routePassengerEmissionsPerMeter(FeedScopedId routeId);

  void addRouteEmissions(Map<FeedScopedId, Emission> routeAvgCo2Emissions);

  /**
   * Return emissions for all hops in the trip pattern for a given trip. The same trip-pattern may
   * have multiple different emissions depending on the vehicle(s) operating the trip on a given
   * date, but OTP does not keep information about the vehicles, so we calculate the average
   * emissions for each hop for a given trip.
   */
  TripPatternEmission tripPatternEmissions(FeedScopedId tripId);

  void addTripPatternEmissions(Map<FeedScopedId, TripPatternEmission> tripPatternEmissions);

  /**
   * Return emission statistics for logging.
   */
  EmissionSummary summary();
}
