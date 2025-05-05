package org.opentripplanner.ext.emission;

import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.utils.lang.Sandbox;

/**
 * A service for getting emissions.
 */
@Sandbox
public interface EmissionService {
  /**
   * Calculate the passenger emission for car for a given distance. The calculation is based on the
   * configured number of people in the car and the average car emissions.
   *
   * @return The emissions per passenger for the whole given distance. {@link Emission#ZERO} is
   * returned if no emission exist.
   */
  Emission calculateCarPassengerEmission(double distanceMeters);

  /**
   * Calculate the passenger emissions for a specific [route and distance] or [trip, from stop,
   * to stop ]. The service implementation will decide which of the two calculation methods
   * is used.
   *
   * @return The emissions per passenger for the whole. {@link Emission#ZERO} is returned if no
   * emission exist.
   */
  Emission calculateTransitPassengerEmissionForTripHops(
    Trip trip,
    int boardStopPosInPattern,
    int alightStopPosInPattern,
    double distance_m
  );
}
