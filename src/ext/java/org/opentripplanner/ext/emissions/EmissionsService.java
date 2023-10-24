package org.opentripplanner.ext.emissions;

import java.util.Optional;
import org.opentripplanner.framework.lang.Sandbox;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * A service for getting emissions information for routes.
 */
@Sandbox
public interface EmissionsService {
  /**
   * Get specific type of emissions per meter for a specific route.
   * @param feedScopedRouteId
   * @param emissionType
   * @return Emissions per meter
   */
  Optional<Double> getEmissionsPerMeterForRoute(
    FeedScopedId feedScopedRouteId,
    EmissionType emissionType
  );

  /**
   * Get specific type of emissions for a car journey.
   * @param emissionType
   * @return Emissions per meter
   */
  Optional<Double> getEmissionsPerMeterForCar(EmissionType emissionType);
}
