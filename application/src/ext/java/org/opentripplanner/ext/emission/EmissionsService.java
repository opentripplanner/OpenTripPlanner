package org.opentripplanner.ext.emissions;

import java.util.Optional;
import org.opentripplanner.model.plan.Emissions;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.Sandbox;

/**
 * A service for getting emissions information for routes.
 */
@Sandbox
public interface EmissionsService {
  /**
   * Get all emissions per meter for a specific route.
   *
   * @return Emissions per meter
   */
  Optional<Emissions> getEmissionsPerMeterForRoute(FeedScopedId feedScopedRouteId);

  /**
   * Get all emissions per meter for a car.
   *
   * @return Emissions per meter
   */
  Optional<Emissions> getEmissionsPerMeterForCar();
}
