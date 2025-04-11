package org.opentripplanner.ext.emission;

import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.utils.lang.Sandbox;

/**
 * A service for getting emission                                                                                                                                                                                                                                                               emissions information for routes.
 */
@Sandbox
public interface EmissionService {
  /**
   * Calculate the passenger emission for car for a given distance. The calculation is based on the
   * configured number of people in the car and the average car emissions.
   *
   * @return The emissions per passanger for the whole given distance. {@link Emission#ZERO} is
   * retuned if no emission exist.
   */
  Emission calculateCarPassengerEmission(double distanceMeters);

  /**
   * Calculate passenger emissions per meter for a specific route.
   */
  Emission calculateEmissionPerMeterForRoute(FeedScopedId feedScopedRouteId, double distance_m);
}
