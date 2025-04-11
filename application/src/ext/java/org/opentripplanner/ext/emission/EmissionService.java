package org.opentripplanner.ext.emission;

import java.util.Optional;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.Sandbox;

/**
 * A service for getting emission                                                                                                                                                                                                                                                               emissions information for routes.
 */
@Sandbox
public interface EmissionService {
  /**
   * Get passenger emissions per meter for a specific route.
   */
  Optional<Emission> getEmissionPerMeterForRoute(FeedScopedId feedScopedRouteId);

  /**
   * Get emissions per meter per person for a car.
   */
  Optional<Emission> getEmissionPerMeterForCar();
}
